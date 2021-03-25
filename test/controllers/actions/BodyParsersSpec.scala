/*
 * Copyright 2021 CJWW Development
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.actions

import helpers.{Assertions, MockToken}
import models.ScopedOwner
import models.UserResults._
import org.scalatestplus.play.PlaySpec
import org.slf4j.{Logger, LoggerFactory}
import play.api.libs.json.{JsObject, JsValue, Json, OFormat}
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContentAsJson, BaseController, ControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class BodyParsersSpec extends PlaySpec with Assertions {

  val testToken: String = MockToken.build(
    clientId = "test-client-id",
    userId = "test-user-id",
    scope = "read:test,write:test"
  )

  val testInvalidToken: String = MockToken.buildInvalid(
    clientId = "test-client-id",
    userId = "test-user-id",
    scope = "read:test,write:test"
  )

  val testBodyParsers: BodyParsers = new BodyParsers with BaseController {
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
  }

  def okFunction(model: TestModel): Future[Result] = Future.successful(Ok(s"str: ${model.str} int: ${model.int}"))

  case class TestModel(str: String, int: Int)

  implicit val format: OFormat[TestModel] = Json.format[TestModel]

  "withJsonBody" should {
    "return an Ok" when {
      "the request body has been transformed into the specified type" in {
        implicit val req: FakeRequest[AnyContentAsJson] = FakeRequest()
          .withJsonBody(Json.obj("str" -> "test", "int" -> 616))

        assertOutput(testBodyParsers.withJsonBody[TestModel](okFunction)) { res =>
          status(res) mustBe OK
          contentAsString(res) mustBe "str: test int: 616"
        }
      }
    }

    "return a BadRequest" when {
      "the request body json doesn't conform to the specified type" in {
        implicit val req: FakeRequest[AnyContentAsJson] = FakeRequest()
          .withJsonBody(Json.obj("strr" -> "test", "int" -> 616))

        assertOutput(testBodyParsers.withJsonBody[TestModel](okFunction)) { res =>
          status(res) mustBe BAD_REQUEST
          val body = contentAsJson(res)
          body.\("obj.str").as[Seq[JsValue]].head.\("msg").as[Seq[String]].head mustBe "error.path.missing"
        }
      }

      "the request body did not contain a json payload" in {
        implicit val req = FakeRequest()

        assertOutput(testBodyParsers.withJsonBody[TestModel](okFunction)) { res =>
          status(res) mustBe BAD_REQUEST
          val body = contentAsJson(res)
          body.\("error").as[String] mustBe "Missing body"
          body.\("message").as[String] mustBe "No body was included with the request. Consult the docs to find the correct body."
        }
      }
    }
  }
}
