package controllers

import java.io.{File, PrintWriter}
import java.net.URI
import java.util.UUID

import org.fusesource.scalate._
import play.api._
import play.api.libs.json._
import play.api.mvc.{Action, _}
import services.FileService

object Application extends Controller {

  val baseDir = play.Play.application.configuration.getString("static.files.dir")
  val templateEngine = new TemplateEngine
  val fileService = FileService.simpleFileService(baseDir)

  def getFile(path: String) = Action {
    Logger.trace(s"getFile(path=$path)")
    fileService.getFile(path).map(f => {
      Logger.info(s"returning file with absolute path '${f.getAbsolutePath}'")
      Ok.sendFile(content = f, inline = true)
    }).getOrElse {
      Logger.info(s"file $path not found")
      NotFound
    }
  }

  def processTemplate(path: String, fileType: Option[String]) = Action(parse.json(10000)) { request =>
    lazy val pathInBaseDir = "/out/" + UUID.randomUUID().toString.replaceAll("-", "") + fileType.map("." + _).getOrElse("")
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
      fileService.getFile(path).map { f =>
        templateEngine.layout(f.getAbsolutePath, attr)
      }
    }

    Logger.trace(s"processTemplate(path=$path): ${request.body}")
    val attr = parseAttributesFromRequest(request)
    val processedTemplate: Option[String] = processTemplate(attr)
    val outFile: Option[File] = processedTemplate.map { fileService.writeToFile(_, pathInBaseDir) }

    outFile.map(f => Ok(pathInBaseDir)).getOrElse(NotFound)
  }
}