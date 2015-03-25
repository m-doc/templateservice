package services

import java.io.{PrintWriter, File}
import java.net.URI
import java.util.UUID

import play.api.Logger

trait FileService {
  def getFile(path: String): Option[java.io.File]
  def createFile(fileContent: String, pathInBaseDir: String, suffix: Option[String]): String

}

private[this] final class SimpleFileService(baseDir: String) extends FileService {
  private[this] def fileSystemPath(path: String): URI = {
    new URI("file:" + baseDir + "/" + path)
  }

  override def getFile(path: String): Option[java.io.File] = {
    val file = new java.io.File(fileSystemPath(path))
    if (file.exists() && file.isFile && file.canRead) Some(file) else None
  }

  override def createFile(fileContent: String, pathInBaseDir: String, suffix: Option[String]): String = {
    val fileName = pathInBaseDir + UUID.randomUUID().toString.replaceAll("-", "") + suffix.map("." + _).getOrElse("")
    val outFile = new File(new URI("file:" + baseDir + fileName))
    val writer = new PrintWriter(outFile)
    try {
      Logger.info(s"writing file '${outFile.getAbsolutePath}'")
      writer.print(fileContent)
    }
    finally {
      writer.close()
    }
    fileName
  }
}

object FileService {
    def simpleFileService(baseDir: String): FileService = new SimpleFileService(baseDir)
}