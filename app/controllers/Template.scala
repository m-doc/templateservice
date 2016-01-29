package controllers

import ammonite.ops

import ammonite.ops._

import org.fusesource.scalate._
import play.Logger
import play.api.libs.json._
import play.api.mvc.{Action, _}

import scalaz.Scalaz._
import scalaz.concurrent.Task
import scalaz.{Writer, _}

import play.api.libs.json._

object Template extends Controller {

  case class TemplateView(name: String, sizeInBytes: Long)

  implicit val templateViewFormat = Json.format[TemplateView]

  val supportedFormats = List("mustache")
  val templateEngine = new TemplateEngine
  val basePath = {
    val templatesDir = play.Play.application.configuration.getString("templates.dir")
    templatesDir + (if (templatesDir.endsWith("/")) "" else "/")
  }

  def adminView() = Action {
    Ok(views.html.template_admin())
  }

  def templateViews() = Action {
    val templateFiles = (ls ! Path(basePath)).filter(p => supportedFormats.exists(ext => p.name.endsWith("." + ext)))
    val result = templateFiles.map(file => TemplateView(file.name, file.size))

    Ok(Json.toJson(result))
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
      }
      catch {
        case _: ResourceNotFoundException => None.set(Vector(s"template $id not found"))
        case e: TemplateException => None.set(Vector(s"invalid template: ${e.getMessage}"))
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
      case -\/(error) => Logger.error("unexpected error:", error); InternalServerError(error.getMessage)
      case \/-(success) =>
        val (logs, result) = success.run
        val logMsgs = logs.foldLeft(s"processTemplate $id")((a, b) => a + "\n" + b)
        Logger.info(logMsgs)
        result
    }
  }
}
