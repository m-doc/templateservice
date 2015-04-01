package controllers

import java.nio.charset.Charset
import java.util.UUID

import models.{PersistentFile, PersistentFilePath}
import org.fusesource.scalate._
import org.fusesource.scalate.support.StringTemplateSource
import play.api._
import play.api.libs.json._
import play.api.mvc.{Action, _}
import play.api.Play.current
import repositories.RepositoryFactory

object Application extends Controller {

  val templateEngine = new TemplateEngine {
    allowCaching = false
    allowReload = false
  }
  val fileRepository = RepositoryResolver.fileRepository

  def getFile(path: String) = Action {
    Logger.trace(s"getFile(path=$path)")
    fileRepository.findByPath(path).map(f => {
      Ok(f.content)
    }).getOrElse {
      NotFound
    }
  }

  def processTemplate(path: String, fileType: Option[String]) = Action(parse.json(100000)) { request =>
    def parseAttributesFromRequest(request: Request[JsValue]): Map[String, String] = {
      val attributesOption = request.body match {
        case JsObject(content) => Some(content.toMap)
        case _ => None
      }
      val result = for {
        attributes <- attributesOption
      } yield attributes.mapValues(_.asOpt[String].get)
      result.getOrElse(Map.empty[String, String])
    }
    def processTemplate(attr: Map[String, String]): Option[String] = {
      fileRepository.findByPath(path).map { f =>
        templateEngine.layout(new StringTemplateSource(f.path.path, new String(f.content)), attr)
      }
    }

    Logger.trace(s"processTemplate(path=$path): ${request.body}")
    val attr = parseAttributesFromRequest(request)
    val processedTemplate: Option[String] = processTemplate(attr)

    val persistentFile = processedTemplate.map { pt =>
      val path = PersistentFilePath(UUID.randomUUID().toString.replace("-", "") + fileType.map("."+_).getOrElse(""))
      val content = pt.getBytes(Charset.forName("UTF-8"))
      val persistentFile = PersistentFile(path, content)
      fileRepository.create(persistentFile)
      persistentFile
    }

    persistentFile.map(f => Ok(f.path.path)).getOrElse(NotFound)
  }
}

object RepositoryResolver {
  val useDb = current.configuration.getString("use.db")
  val baseDir = current.configuration.getString("static.files.dir")

  val fileRepository = useDb match {
    case Some(_) => RepositoryFactory.persistentFileDbRepository
    case _ => RepositoryFactory.persistentFileFsRespository(baseDir.get)
  }
}