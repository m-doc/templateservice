package controllers

import java.nio.file.{DirectoryStream, Paths}

import org.fusesource.scalate._
import play.Logger
import play.api.libs.json._
import play.api.mvc.{Action, _}
import services.TemplateService

import scala.reflect.io.Path
import scalaz.Scalaz._
import scalaz.concurrent.Task
import scalaz.{Writer, _}
import scalaz.{Free, Id, ~>, Coyoneda}

import org.mdoc.fshell.Shell.ShellSyntax

import play.api.libs.json._

object Template extends Controller {

  implicit val templateViewFormat = Json.format[TemplateService.TemplateView]

  val supportedFormats = List("mustache")
  val templateEngine = new TemplateEngine

  lazy val currentWorkingDir = Paths.get("").toAbsolutePath.toString

  val basePath = {
    val templatesDir = play.Play.application.configuration.getString("templates.dir")
    templatesDir + (if (templatesDir.endsWith("/")) "" else "/")
  }

  val absoluteBasePath = Paths.get(
    if (basePath.startsWith("/")) basePath
    else basePath.split("/").foldLeft(currentWorkingDir)((z, s) => z + "/" + s)
  )

  def placeholders(id: String) = Action {
    Logger.info(s"requested placeholders of template with id ${id}")
    val program = TemplateService.getPlaceholders.map(
      _
        .map(option =>
          option.map(either => either.bimap(
            error => {
              val errorMsg = s"invalid template encoding for template with id ${id}: only utf-8 is supported"
              InternalServerError(errorMsg).set(errorMsg)
            },
            variables => {
              val logMsg = s"palceholders ${variables} found for template with id ${id}"
              Ok(Json.toJson(variables)).set(logMsg)
            }
          ).merge).getOrElse(NotFound.set(s"template with id ${id} not found")))
    )
    val (logMsg, result) = program
      .run(absoluteBasePath.resolve(id))
      .runTask
      .run
      .run
    Logger.info(logMsg)
    Logger.info(s"returning ${result}")
    result
  }

  def adminView() = Action {
    Ok(views.html.template_admin())
  }

  def templateViews() = Action {
    Logger.info("requested list of all templates")
    val (logMsg, result) = TemplateService
      .getTemplates.map(_.map(templates => Ok(Json.toJson(templates)).set(s"found ${templates.size} templates")))
      .run((absoluteBasePath, supportedFormats))
      .run
      .run
    Logger.info(logMsg)
    result
  }

  def process(id: String) = Action { req =>
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

    processedTemplate.attemptRun match {
      case -\/(error) =>
        Logger.error("unexpected error:", error); InternalServerError(error.getMessage)
      case \/-(success) =>
        val (logs, result) = success.run
        val logMsgs = logs.foldLeft(s"processTemplate $id")((a, b) => a + "\n" + b)
        Logger.info(logMsgs)
        result
    }
  }
}
