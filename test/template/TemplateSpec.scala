package template

import java.util.UUID

import play.api.libs.json.{JsObject, JsString}
import play.api.test.{PlaySpecification, _}

object TemplateSpec extends PlaySpecification {

  val fakeApplication = FakeApplication()

  "Template" should {

    "return status OK if the template exists" in new WithApplication(fakeApplication) {
      val requestBody = JsObject(List(("name", JsString(UUID.randomUUID().toString))))
      val Some(result) = route(FakeRequest(POST, "/api/process/test.mustache").withJsonBody(requestBody))

      status(result) must equalTo(OK)
    }

    "return status NOT_FOUND if no valid template with requested name exists" in new WithApplication(fakeApplication) {
      val requestBody = JsObject(List(("name", JsString(UUID.randomUUID().toString))))
      val Some(result) = route(FakeRequest(POST, "/api/process/notexists.mustache").withJsonBody(requestBody))

      status(result) must equalTo(NOT_FOUND)
    }

    "process the requested template using the provided json - attributes" in new WithApplication(fakeApplication) {
      val requestBody = JsObject(List(("name", JsString("World"))))
      val Some(result) = route(FakeRequest(POST, "/api/process/test.mustache").withJsonBody(requestBody))

      contentAsString(result) must equalTo("Hello World!\n")
    }
  }
}
