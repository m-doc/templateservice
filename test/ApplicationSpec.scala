import java.io.{FileOutputStream, OutputStream, OutputStreamWriter, File}

import org.junit.runner._
import org.specs2.mutable._
import org.specs2.runner._
import play.api.libs.json.JsValue
import play.api.test.Helpers._
import play.api.test._
import play.api.Play.current
import play.api.libs.json._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {

  "Application" should {

    val fileContent = "Hello {{name}}!".getBytes
    val filePath = "/testfile.mustache"

    def fileInBasedir(byPath: String) = new File(current.configuration.getString("static.files.dir").get+byPath)

    def createTestFile() = {
      val file = fileInBasedir(filePath)
      if (!file.exists()) file.createNewFile()
      val writer = new FileOutputStream(file)
      try {
        writer.write(fileContent)
      }
      finally {
        writer.close()
        file.deleteOnExit()
      }
    }

    "send a file with relative path denoted by the request path" in new WithApplication {
      createTestFile()
      val result = route(FakeRequest(GET, filePath)).get
      status(result) must beEqualTo(OK)
      contentAsBytes(result) must beEqualTo(fileContent)
    }

    "send status 'not found' for unknown path" in new WithApplication {
      createTestFile()
      val result = route(FakeRequest(GET, "/asdf.qwer")).get
      status(result) must beEqualTo(NOT_FOUND)
    }

    "write a new file while processing a template" in new WithApplication {
      createTestFile()
      val requestProcessTemplate = FakeRequest(POST, filePath+"?fileType=html")
        .withJsonBody(Json.parse("""{ "name" : "World"}"""))
      val processTemplateResult = route(requestProcessTemplate).get
      status(processTemplateResult) must beEqualTo(OK)
      contentAsString(processTemplateResult) must beMatching(".+\\.html")

      val fileId = contentAsString(processTemplateResult)
      val getFileResult = route(FakeRequest(GET, "/"+fileId)).get
      fileInBasedir("/"+fileId).deleteOnExit()
      contentAsString(getFileResult) must beEqualTo("Hello World!")
    }
  }
}
