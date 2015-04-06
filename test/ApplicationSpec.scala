import java.util.UUID

import org.specs2.mutable.BeforeAfter
import play.api.Application
import play.api.libs.json.Json
import play.api.test.{FakeRequest, PlaySpecification, WithApplication}

trait ApplicationSpec extends PlaySpecification with BeforeAfter {

  lazy val originalUseDbProperty = System.getProperty("use.db")

  lazy val fileName = UUID.randomUUID().toString.replaceAll("-", "") + ".mustache"
  lazy val filePath = "/" + fileName
  lazy val fileContent = "Hello {{name}}!".getBytes

  def createTestFile(implicit app: Application): Unit

  def cleanUp(fileId: String): Unit

  def useDb: String

  "Application" should {

    "send a file with relative path denoted by the request path" in new WithApplication {
      createTestFile
      val result = route(FakeRequest(GET, filePath)).get
      status(result) must beEqualTo(OK)
      contentAsBytes(result) must beEqualTo(fileContent)
    }

    "send status 'not found' for unknown path" in new WithApplication {
      val result = route(FakeRequest(GET, "/asdf.qwer")).get
      status(result) must beEqualTo(NOT_FOUND)
    }

    "write a new file while processing a template" in new WithApplication {
      createTestFile
      val requestProcessTemplate = FakeRequest(POST, filePath+"?fileType=html")
        .withJsonBody(Json.parse("""{ "name" : "World"}"""))
      val processTemplateResult = route(requestProcessTemplate).get
      status(processTemplateResult) must beEqualTo(OK)
      val fileId = contentAsString(processTemplateResult)
      fileId must beMatching(".+\\.html")
      cleanUp(fileId)
      val getFileResult = route(FakeRequest(GET, "/"+fileId)).get
      contentAsString(getFileResult) must beEqualTo("Hello World!")
    }
  }

  override def after {
    System.setProperty("use.db", originalUseDbProperty)
  }

  override def before {
    System.setProperty("use.db", useDb)
  }

}