package repositories

import java.io._
import java.net.URI

import models.{PersistentFilePath, PersistentFile}
import play.api.Logger

class PersistentFileFsRepository(baseDir: String) extends PersistentFileRepository {
  private[this] def fileSystemPath(path: String): String = baseDir + "/" + path

  override def create(file: PersistentFile) {
    val outFile = new File(fileSystemPath(file.path.path))
    val writer = new FileOutputStream(outFile)
    try {
      writer.write(file.content)
      Logger.info(s"created file with path=${file.path.path} in file system path ${outFile.getAbsolutePath}")
    }
    finally {
      writer.close()
    }
  }

  override def findByPath(path: String) = {
    val file = new java.io.File(fileSystemPath(path))
    if (file.exists() && file.isFile && file.canRead) {
      val in = new FileInputStream(file)
      try {
        val content: Array[Byte] = new Array[Byte](file.length().toInt)
        in.read(content)
        Some(PersistentFile(PersistentFilePath(path), content))
      }
      finally {
        in.close()
      }
    }
    else None
  }
}

