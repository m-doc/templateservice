package services

import java.nio.file.Path

import scalaz._
import scalaz.concurrent.Task

final case class TemplateView(name: String, sizeInBytes: Long)

trait GetTemplates {

  //TODO switch to FSHell as soon as 'FilesInDirectory' is available

  type GetTemplates = Reader[(Path, Seq[String]), Task[Seq[TemplateView]]]
  val getTemplates: GetTemplates = Reader {
    case (path, fileEndings) =>
      Task {
        val supportedFormatsFilterString = fileEndings.foldLeft("*.{")((z, ext) => z + "," + ext) + "}"
        val files = java.nio.file.Files.newDirectoryStream(path, supportedFormatsFilterString)
        try {
          import scala.collection.JavaConversions._
          files.iterator().toSeq
            .map(path => TemplateView(path.getFileName.toString, path.toFile.length))
            .sortBy(tv => tv.name)
        } finally {
          files.close()
        }
      }
  }
}
