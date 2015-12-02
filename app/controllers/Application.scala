package controllers

import org.fusesource.scalate._
import play.Logger
import play.api.libs.json._
import play.api.mvc.{Action, _}

import scalaz.Scalaz._
import scalaz.concurrent.Task
import scalaz.{Writer, _}

object Application extends Controller {

  val templateEngine = new TemplateEngine
  val basePath = {
    val templatesDir = play.Play.application.configuration.getString("templates.dir")
    templatesDir + (if (templatesDir.endsWith("/")) "" else "/")
  }

  def processTemplate(id: String) = Action { req =>
    lazy val variables: Writer[Vector[String], Map[String, String]] = {
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

    def template(path: String)(vars: Map[String, String]): Task[Writer[Vector[String], Option[String]]] = Task {
      try {
        templateEngine.layout(path, vars).some.set(Vector.empty)
      }
      catch {
        case _: ResourceNotFoundException => None.set(Vector(s"template $id not found"))
        case e: TemplateException => None.set(Vector(s"invalid template: ${e.getMessage}"))
      }
    }

    lazy val path = basePath + id

    lazy val processedTemplate: Task[Writer[Vector[String], Result]] =
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
