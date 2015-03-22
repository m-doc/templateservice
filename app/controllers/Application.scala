package controllers

import java.io.{File, PrintWriter}
import java.net.URI
import java.util.UUID

import org.fusesource.scalate._
import play.api._
import play.api.libs.json._
import play.api.mvc.{Action, _}

object Application extends Controller {

  val baseDir = play.Play.application.configuration.getString("static.files.dir")
  val templateEngine = new TemplateEngine

  private[this] def fileSystemPath(path: String): URI = {
    new URI("file:" + baseDir + "/" + path)
  }

  private[this] def fileInBaseDir(path: String): Option[java.io.File] = {
    val file = new java.io.File(fileSystemPath(path))
    if (file.exists() && file.isFile && file.canRead) Some(file) else None
  }

  def getFile(path: String) = Action {
    Logger.trace(s"getFile(path=$path)")
    fileInBaseDir(path).map(f => {
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
      fileInBaseDir(path).map { f =>
        templateEngine.layout(f.getAbsolutePath, attr)
      }
    }
    def writeProcessedTemplateToFile(processedTemplate: Option[String]): Option[File] = {
      processedTemplate.map { content =>
        val outFile = new File(new URI("file:" + baseDir + pathInBaseDir))
        val writer = new PrintWriter(outFile)
        try {
          Logger.info(s"writing file '${outFile.getAbsolutePath}'")
          writer.print(content)
        }
        finally {
          writer.close()
        }
        outFile
      }
    }

    Logger.trace(s"processTemplate(path=$path): ${request.body}")
    val attr = parseAttributesFromRequest(request)
    val processedTemplate: Option[String] = processTemplate(attr)
    val outFile: Option[File] = writeProcessedTemplateToFile(processedTemplate)

    outFile.map(f => Ok(pathInBaseDir)).getOrElse(NotFound)
  }
}