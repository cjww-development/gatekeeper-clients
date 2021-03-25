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
import play.api.mvc.{BaseController, ControllerComponents, Result}
import play.api.test.Helpers._
import play.api.mvc.Results.Ok
import play.api.test.FakeRequest

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuthorisedActionSpec extends PlaySpec with Assertions {

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

  val testAuthAction: AuthorisedAction = new AuthorisedAction with BaseController {
    override val logger: Logger = LoggerFactory.getLogger(this.getClass)
    override protected def controllerComponents: ControllerComponents = stubControllerComponents()
    override val currentSignature: String = MockToken.testSignature
  }

  def okFunction(scopedOwner: ScopedOwner): Future[Result] = Future.successful(Ok(s"User: ${scopedOwner.sub} with scopes ${scopedOwner.scopes}"))

  "userAuthorisation" should {
    "return an Ok" when {
      "the auth token has been validated and the sub and scopes have been fetched from the token" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> s"Bearer $testToken")

        val result = testAuthAction.userAuthorisation[ScopedOwner] { _ => so =>
          okFunction(so)
        }(scopedOwner, global)(req)

        status(result) mustBe OK
        contentAsString(result) mustBe s"User: test-user-id with scopes Set(read:test, write:test)"
      }
    }

    "return a Forbidden" when {
      "the auth token is missing from the request" in {
        val result = testAuthAction.userAuthorisation[ScopedOwner] { _ => so =>
          okFunction(so)
        }(scopedOwner, global)(FakeRequest())

        status(result) mustBe FORBIDDEN
        val body = contentAsJson(result)
        body.\("error").as[String] mustBe "Invalid authorization header"
        body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
      }

      "the auth token was not prefixed with Bearer" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> testToken)

        val result = testAuthAction.userAuthorisation[ScopedOwner] { _ => so =>
          okFunction(so)
        }(scopedOwner, global)(req)

        status(result) mustBe FORBIDDEN
        val body = contentAsJson(result)
        body.\("error").as[String] mustBe "Invalid authorization header"
        body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
      }

      "the auth token was could not be validated" in {
        val req = FakeRequest()
          .withHeaders("Authorization" -> s"Bearer $testInvalidToken")

        val result = testAuthAction.userAuthorisation[ScopedOwner] { _ => so =>
          okFunction(so)
        }(scopedOwner, global)(req)

        status(result) mustBe FORBIDDEN
        val body = contentAsJson(result)
        body.\("error").as[String] mustBe "Invalid authorization header"
        body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
      }
    }
  }

  "scopeValidation" should {
    "return an Ok" when {
      "the the scopes are valid" in {
        assertOutput(testAuthAction.scopeValidation(Set("read:app"), Set("read:app"))( Future.successful(Ok) )) { res =>
          status(res) mustBe OK
        }
      }
    }

    "return a Forbidden" when {
      "the tokens scopes don't match when the valid scopes" in {
        assertOutput(testAuthAction.scopeValidation(Set("read:app"), Set("write:app"))( Future.successful(Ok) )) { res =>
          status(res) mustBe FORBIDDEN
          val body = contentAsJson(res)
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are read:app."
        }
      }
    }
  }
}
