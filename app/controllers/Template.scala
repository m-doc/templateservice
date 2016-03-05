package controllers

import java.nio.file.Paths
import org.fusesource.scalate._
import play.api.libs.json._
import play.api.mvc._
import services.TemplateServiceOp.TemplateVars
import services._
import play.Logger
import play.api.libs.json._
import play.api.mvc.{ Action, _ }
import scalaz._
import scalaz.Scalaz._
import scalaz.concurrent.Task
import scalaz.{Writer, _}
import scalaz.{Free, Id, ~>, Coyoneda}
import services.TemplateService._

object Template extends Controller {

  private[this] implicit val templateViewFormat = Json.format[TemplateView]

  private[this] val supportedFormats = List("mustache")

  private[this] lazy val currentWorkingDir = Paths.get("").toAbsolutePath.toString

  private[this] val basePath = {
    val templatesDir = play.Play.application.configuration.getString("templates.dir")
    templatesDir + (if (templatesDir.endsWith("/")) "" else "/")
  }

  private[this] val absoluteBasePath = {
    val path = if (basePath.startsWith("/")) {
      basePath
    } else {
      basePath.split("/").foldLeft(currentWorkingDir)((z, s) => z + "/" + s)
    }
    if (path.endsWith("/")) path else path + "/"
  }

  def placeholders(id: String): Action[AnyContent] = Action {
    Logger.info(s"requested placeholders of template with id $id")
    Logger.info(s"templatepath: ${absoluteBasePath + id}")
    val program = TemplateServiceFileSystemInterpreter(GetPlaceholders(absoluteBasePath + id))
      .map { res =>
        res match {
          case TemplateNotFound => {
            Logger.info(s"template with id $id not found")
            NotFound(id)
          }
          case InvalidTemplateEncoding => {
            val errorMsg = s"invalid template encoding for template with id $id: only utf-8 is supported"
            Logger.warn(errorMsg)
            InternalServerError(errorMsg)
          }
          case Placeholders(placeholders) => {
            Logger.info(s"palceholders $placeholders found for template with id $id")
            Ok(Json.toJson(placeholders))
          }
        }
      }

    val result = program.run

    Logger.info(s"returning $result")
    result
  }

  def adminView(): Action[AnyContent] = Action {
    Ok(views.html.template_admin())
  }

  def templateViews(): Action[AnyContent] = Action {
    Logger.info("requested list of all templates")
    val (logMsg, result) = TemplateServiceFileSystemInterpreter(GetTemplates(absoluteBasePath, supportedFormats))
      .map(templates => Ok(Json.toJson(templates)).set(s"found ${templates.size} templates"))
      .run
      .run
    Logger.info(logMsg)
    result
  }

  def process(id: String): Action[AnyContent] = Action { req =>
    val variables: TemplateVars = req.body.asJson match {
      case Some(JsObject(content)) => {
        val contentMap = content.toMap
        Logger.info(s"request body contains: $contentMap")
        contentMap.mapValues(_.as[String])
      }
      case _ => {
        Logger.warn(s"request body contains no json: ${req.body}")
        Map.empty
      }
    }

    val path = basePath + id

    val program = TemplateServiceFileSystemInterpreter(ProcessTemplate(path, variables))
      .map {
        _ match {
          case ProcessedTemplate(content) => {
            Logger.info(s"successfully processed Template $path")
            Ok(content)
          }
          case TemplateNotFound => {
            val msg = s"template $path not found"
            Logger.warn(msg)
            NotFound(msg)
          }
          case InvalidTemplate(errMsg, exc) => {
            val msg = s"template-definition og $path is invalid: $errMsg"
            Logger.error(msg, exc)
            InternalServerError(msg)
          }
        }
      }

    program.run
  }

  def version: Action[AnyContent] = Action {
    Ok(org.mdoc.templates.BuildInfo.version)
  }
}
