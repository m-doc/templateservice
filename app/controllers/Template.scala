package controllers

import java.io.File
import java.nio.charset.Charset
import java.nio.file.{Path, Paths}
import org.apache.commons.io.{FileUtils, IOUtils}
import org.fusesource.scalate._
import play.api.libs.json._
import play.api.mvc._
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
          case notFound: TemplateNotFound => {
            handleTemplateNotFound(notFound)
          }
          case invalidEncoding: InvalidTemplateEncoding => {
            handleInvalidTemplateEncoding(invalidEncoding)
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

  private[this] def handleTemplateNotFound(notFound: TemplateNotFound): Result = {
    Logger.info(s"template with id ${notFound.id} not found")
    NotFound(notFound.id)
  }

  private[this] def handleInvalidTemplateEncoding(invalidEncoding: InvalidTemplateEncoding): Result = {
    val errorMsg = s"invalid template encoding for template with id ${invalidEncoding.id}: " +
      s"expected ${invalidEncoding.expectedEncoding}"
    Logger.warn(errorMsg)
    InternalServerError(errorMsg)
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

    def fileContent(path: String) = TemplateServiceFileSystemInterpreter(GetTemplateContent(path))

    def process(content: GetContentResult): Task[Result] = {
      Logger.debug(s"template ($path) content:\n $content")
      TemplateServiceFileSystemInterpreter(ProcessTemplate(content, variables))
        .map {
          _ match {
            case ProcessedTemplate(content) => {
              Logger.info(s"successfully processed Template $path")
              Logger.debug(s"processed $path with result:\n$content")
              Ok(content.content)
            }
            case notFound: TemplateNotFound => {
              handleTemplateNotFound(notFound)
            }
            case InvalidTemplate(errMsg, exc) => {
              val msg = s"template-definition og $path is invalid: $errMsg"
              Logger.error(msg, exc)
              InternalServerError(msg)
            }
            case invalidEncoding: InvalidTemplateEncoding => {
              handleInvalidTemplateEncoding(invalidEncoding)
            }
          }
        }
    }

    fileContent(path).flatMap {
      process(_)
    }
      .run
  }

  def version: Action[AnyContent] = Action {
    Ok(org.mdoc.templates.BuildInfo.version)
  }
}
