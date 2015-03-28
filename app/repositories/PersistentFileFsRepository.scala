package repositories

import java.io._
import java.net.URI

import models.{PersistenFilePath, PersistentFile}

class PersistentFileFsRepository(baseDir: String) extends PersistentFileRepository {
  private[this] def fileSystemPath(path: String): String = baseDir + "/" + path

  override def create(file: PersistentFile) {
    val outFile = new File(new URI(fileSystemPath(file.path.path)))
    val writer = new PrintWriter(outFile)
    try {
      writer.print(file.content)
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
        Some(PersistentFile(PersistenFilePath(path), content))
      }
      finally {
        in.close()
      }
    }
    else None
  }
}

