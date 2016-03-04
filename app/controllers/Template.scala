package controllers

import java.nio.file.Paths
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
  private[this] val templateEngine = new TemplateEngine

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

  //TODO move businesslogic to TemplateService
  def process(id: String): Action[AnyContent] = Action { req =>
    type TemplateVars = Map[String, String]
    type Content = String
    type Logs = Vector[String]
    type LogsWith[A] = Writer[Logs, A]

    lazy val variables: LogsWith[TemplateVars] = {
      val attributes = req.body.asJson match {
        case Some(JsObject(content)) =>
          val contentMap = content.toMap
          Some(contentMap).set(Vector(s"request body contains: $contentMap"))
        case _ => None.set(Vector(s"request body contains no json: ${req.body}"))
      }
      attributes.map {
        _
          .map[Map[String, String]](_.mapValues(_.as[String]))
          .getOrElse(Map.empty)
      }
    }

    def template(path: String)(vars: TemplateVars): Task[LogsWith[Option[Content]]] = Task {
      try {
        templateEngine.layout(path, vars).some.set(Vector.empty)
      } catch {
        case _: ResourceNotFoundException => none[Content].set(Vector(s"template $id not found"))
        case e: TemplateException => none[Content].set(Vector(s"invalid template: ${e.getMessage}"))
      }
    }

    lazy val path = basePath + id

    lazy val processedTemplate: Task[LogsWith[Result]] =
      variables
        .map(vars => template(path)(vars))
        .sequence
        .map(_.flatMap(identity))
        .map(_.map(_.map(Ok(_)).getOrElse(NotFound)))

    val (logs, result) = processedTemplate.run.run
    val logMsgs = logs.foldLeft(s"processTemplate $id")((a, b) => a + "\n" + b)
    Logger.info(logMsgs)
    result
  }

  def version: Action[AnyContent] = Action {
    Ok(org.mdoc.templates.BuildInfo.version)
  }
}
