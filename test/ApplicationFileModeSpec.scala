import java.io.{File, FileOutputStream}

import org.junit.runner._
import org.specs2.runner._
import play.api.Application
import play.api.Play.current

@RunWith(classOf[JUnitRunner])
class ApplicationFileModeSpec extends ApplicationSpec {

  override lazy val useDb = "false"

  override def createTestFile(implicit app: Application) = {
    val _filePath = filePath(fileName)
    val file = fileInBasedir(_filePath)
    if (!file.exists()) file.createNewFile()
    val writer = new FileOutputStream(file)
    try {
      writer.write(fileContent)
    }
    finally {
      writer.close()
      file.deleteOnExit()
    }
    _filePath
  }

  def cleanUp(fileId: String) = fileInBasedir("/" + fileId).deleteOnExit()

  def fileInBasedir(byPath: String) = new File(current.configuration.getString("static.files.dir").get + byPath)
}
