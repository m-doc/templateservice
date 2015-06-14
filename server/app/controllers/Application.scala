package controllers

import java.nio.charset.Charset
import java.util.UUID

import models.{PersistentFile, PersistentFilePath}
import org.fusesource.scalate._
import org.fusesource.scalate.support.StringTemplateSource
import play.api.Play.current
import play.api._
import play.api.libs.json._
import play.api.mvc.{Action, _}
import repositories.RepositoryFactory

import scalaz.effect.IO
import scalaz.Scalaz._


object Application extends Controller {

  def fileRepository = RepositoryResolver.fileRepository
  val templateEngine = new TemplateEngine {
    allowCaching = false
    allowReload = false
  }

  def index = Action {
    Ok(views.html.index())
  }

  def getFile(path: String) = Action {
    Logger.trace(s"getFile(path=$path)")
    (for {
      fileOpt <- fileRepository.findByPath(path)
    } yield fileOpt match {
        case Some(file) => Ok(file.content)
        case _ => NotFound
      }).unsafePerformIO
  }

  def processTemplate(path: String, fileType: Option[String]) = Action(parse.json(100000)) { req =>
    lazy val parseAttributesFromRequest: (Request[JsValue]) => Map[String, String] =
      (request) => {
        lazy val attributesOption = request.body match {
        case JsObject(content) => Some(content.toMap)
        case _ => None
      }
      val result = for {
        attributes <- attributesOption
      } yield attributes.mapValues(_.asOpt[String].get)
      result.getOrElse(Map.empty[String, String])
    }

    lazy val processTemplate: (PersistentFile) => (Map[String, String]) => String =
      (templateFile) => (attr) => templateEngine.layout(
        new StringTemplateSource(templateFile.path.path, new String(templateFile.content)), attr)

    lazy val toOutFile: (String) => PersistentFile =
      (processedTemplate) => {
      val path = PersistentFilePath(UUID.randomUUID().toString.replace("-", "") + fileType.map("."+_).getOrElse(""))
        val content = processedTemplate.getBytes(Charset.forName("UTF-8"))
        PersistentFile(path, content)
    }

    def toOk: (PersistentFile) => Result =
      (outFile) => Ok(outFile.path.path)

    def run(templateFile: PersistentFile): PersistentFile =
      parseAttributesFromRequest
        .andThen(processTemplate.apply(templateFile))
        .andThen(toOutFile)
        .apply(req)

    lazy val result: IO[Option[Result]] = fileRepository.findByPath(path).flatMap{ fileOpt =>
      val processedTemplateOpt = fileOpt.map(run(_))
      val unit: Option[IO[Unit]] = processedTemplateOpt.map(fileRepository.create(_))
      val io: IO[Option[Unit]] = unit.sequence[IO, Unit]
      val result: IO[Option[PersistentFile]] = io.map(opt => opt.flatMap(_ => processedTemplateOpt))
      result.map(opt => opt.map(toOk(_)))
    }

    result
      .map(_.getOrElse(NotFound))
      .unsafePerformIO()
  }
}

object RepositoryResolver {
  def useDb = current.configuration.getString("use.db")

  def baseDir = current.configuration.getString("static.files.dir")

  def fileRepository = useDb match {
    case Some("true") => RepositoryFactory.persistentFileDbRepository
    case _ => RepositoryFactory.persistentFileFsRespository(baseDir.get)
  }
}