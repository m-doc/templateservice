package services

import java.io.{PrintWriter, File}
import java.net.URI

import play.api.Logger

trait FileService {
  protected def baseDir: String

  def getFile(path: String): Option[java.io.File]
  def writeToFile(fileContent: String, pathInBaseDir: String): File

}

private[this] final class SimpleFileService(override val baseDir: String) extends FileService {
  private[this] def fileSystemPath(path: String): URI = {
    new URI("file:" + baseDir + "/" + path)
  }

  override def getFile(path: String): Option[java.io.File] = {
    val file = new java.io.File(fileSystemPath(path))
    if (file.exists() && file.isFile && file.canRead) Some(file) else None
  }

  override def writeToFile(fileContent: String, pathInBaseDir: String): File = {
    val outFile = new File(new URI("file:" + baseDir + pathInBaseDir))
    val writer = new PrintWriter(outFile)
    try {
      Logger.info(s"writing file '${outFile.getAbsolutePath}'")
      writer.print(fileContent)
    }
    finally {
      writer.close()
    }
    outFile
  }
}

object FileService {
    def simpleFileService(baseDir: String): FileService = new SimpleFileService(baseDir)
}