package controllers

import ammonite.ops

import ammonite.ops._

import org.fusesource.scalate._
import org.fusesource.scalate.mustache.{Statement, Variable, MustacheParser}
import play.Logger
import play.api.libs.json._
import play.api.mvc.{Action, _}

import scalaz.Scalaz._
import scalaz.concurrent.Task
import scalaz.{Writer, _}
import scalaz.{Free, Id, ~>, Coyoneda}

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

  val absoluteBasePath =
    if (basePath.startsWith("/")) Path(basePath)
    else basePath.split("/").foldLeft(cwd)((z, ps) => z / ps)

  def placeholders(id: String) = Action { req =>
    sealed trait TemplateOps[+A]
    final case class CheckIfTemplateExists(path: Path) extends TemplateOps[Option[Path]]
    final case class ReadTemplateContent(path: Option[Path]) extends TemplateOps[Option[String]]

    type TemplateOpsCoyoneda[A] = Coyoneda[TemplateOps, A]

    val checkIfTemplateExists = (path: Path) => Free.liftFC(CheckIfTemplateExists(path))

    val readContent = (path: Option[Path]) => Free.liftFC(ReadTemplateContent(path))

    val parseStatements = (content: Option[String]) =>
      content.map(new MustacheParser().parse(_))

    val filterVariables = (stmnts: Option[List[Statement]]) => stmnts.map(_.flatMap(_ match {
      case v: Variable => Some(v.name.value)
      case _ => None
    }))

    val program = Reader(checkIfTemplateExists).map(
      _
        .flatMap(readContent)
        .map(parseStatements)
        .map(filterVariables)
    )

    val result = program.map(
      _
        .map(maybeVariables =>
          maybeVariables.map(variables => Ok(Json.toJson(variables)))
            .getOrElse(NotFound)
        )
    )

    val taskInterpreter: TemplateOps ~> Task = new (TemplateOps ~> Task) {
      override def apply[A](op: TemplateOps[A]): Task[A] = op match {
        case CheckIfTemplateExists(path) => checkIfTemplateExistsTask(path)
        case ReadTemplateContent(maybePath) => readContentTask(maybePath)
      }

      def checkIfTemplateExistsTask(path: Path) = Task {
        if (exists ! path) Some(path) else None
      }

      def readContentTask(maybePath: Option[Path]) = Task {
        maybePath.map(read ! _)
      }
    }

    val task = Free.runFC(result.run(absoluteBasePath / id))(taskInterpreter)
    task.run
  }

  def adminView() = Action {
    Ok(views.html.template_admin())
  }

  def templateViews() = Action {
    val templateFiles = (ls ! absoluteBasePath)
      .filter(p => supportedFormats.exists(ext => p.name.endsWith("." + ext)))
      .sortBy(_.name)
      .map(file => TemplateView(file.name, file.size))

    Ok(Json.toJson(templateFiles))
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
      case -\/(error) => Logger.error("unexpected error:", error); InternalServerError(error.getMessage)
      case \/-(success) =>
        val (logs, result) = success.run
        val logMsgs = logs.foldLeft(s"processTemplate $id")((a, b) => a + "\n" + b)
        Logger.info(logMsgs)
        result
    }
  }
}
