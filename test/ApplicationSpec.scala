import java.util.UUID

import play.api.Application
import play.api.libs.json.Json
import play.api.test.{FakeApplication, FakeRequest, PlaySpecification, WithApplication}

trait ApplicationSpec extends PlaySpecification {

  //vals in Specification must be lazy, otherwise an exception occurs
  lazy val originalUseDbProperty = System.getProperty("use.db")

  def fileName = UUID.randomUUID().toString.replaceAll("-", "") + ".mustache"
  def filePath(fileName: String) = "/" + fileName
  lazy val fileContent = "Hello {{name}}!".getBytes

  def createTestFile(implicit app: Application): String

  def cleanUp(fileId: String): Unit

  def useDb: String

  "Application" should {

    "send a file with relative path denoted by the request path" in new WithApplication {
      val filePath = createTestFile
      val result = route(FakeRequest(GET, filePath)).get
      status(result) must beEqualTo(OK)
      contentAsBytes(result) must beEqualTo(fileContent)
    }

    "send status 'not found' for unknown path" in new WithApplication(FakeApplication(additionalConfiguration = Map("use.db" -> useDb))) {
      val result = route(FakeRequest(GET, "/asdf.qwer")).get
      status(result) must beEqualTo(NOT_FOUND)
    }

    "write a new file while processing a template" in new WithApplication {
      val filePath = createTestFile
      val requestProcessTemplate = FakeRequest(POST, filePath+"?fileType=html")
        .withJsonBody(Json.parse("""{ "name" : "World"}"""))
      val processTemplateResult = route(requestProcessTemplate).get
      status(processTemplateResult) must beEqualTo(OK)
      val fileId = contentAsString(processTemplateResult)
      fileId must beMatching(".+\\.html")
      cleanUp(fileId)
      val getFileResult = route(FakeRequest(GET, "/" + fileId)).get
      contentAsString(getFileResult) must beEqualTo("Hello World!")
    }
  }

}