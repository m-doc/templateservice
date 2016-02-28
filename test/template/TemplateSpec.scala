package template

import java.util.UUID
import play.api.libs.json.{JsArray, JsObject, JsString}
import play.api.test.{ PlaySpecification, _ }

object TemplateSpec extends PlaySpecification {

  val fakeApplication = FakeApplication()

  val testfile: String = "test.mustache"

  "Template processing" should {

    "return status OK if the template exists" in new WithApplication(fakeApplication) {
      val requestBody = JsObject(List(("name", JsString(UUID.randomUUID().toString))))
      val Some(result) = route(FakeRequest(controllers.routes.Template.process(testfile)).withJsonBody(requestBody))

      status(result) must equalTo(OK)
    }

    "return status NOT_FOUND if no valid template with requested name exists" in new WithApplication(fakeApplication) {
      val requestBody = JsObject(List(("name", JsString(UUID.randomUUID().toString))))
      val Some(result) = route(FakeRequest(controllers.routes.Template.process("notexists.mustache")).withJsonBody(requestBody))

      status(result) must equalTo(NOT_FOUND)
    }

    "process the requested template using the provided json - attributes" in new WithApplication(fakeApplication) {
      val requestBody = JsObject(List(("name", JsString("World"))))
      val Some(result) = route(FakeRequest(controllers.routes.Template.process(testfile)).withJsonBody(requestBody))

      contentAsString(result) must equalTo("Hello World!\n")
    }
  }

  "Template templateViews" should {
    "list all available template-files" in new WithApplication(fakeApplication) {
      val Some(result) = route(FakeRequest(controllers.routes.Template.templateViews()))
      val jsonResult = contentAsJson(result)
      jsonResult.\\("name") must have size 1
      jsonResult.\\("name").head must equalTo(JsString(testfile))
      jsonResult.\\("sizeInBytes") must have size 1
    }
  }

  "Template placeholders" should {
    "return status ok if template exists" in new WithApplication(fakeApplication) {
      val Some(result) = route(FakeRequest(controllers.routes.Template.placeholders(testfile)))
      status(result) must equalTo(OK)
    }

    "list all placeholders contained in a template" in new WithApplication(fakeApplication) {
      val Some(result) = route(FakeRequest(controllers.routes.Template.placeholders(testfile)))
      val jsonResult = contentAsJson(result)
      jsonResult mustEqual JsArray(List(JsString("name")))
    }
  }
}
