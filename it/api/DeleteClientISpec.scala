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

package api

import database.{ClientStore, CodecReg}
import helpers.{Assertions, IntegrationApp, MockToken}
import models.Client
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits._

class DeleteClientISpec
  extends PlaySpec
    with IntegrationApp
    with Assertions
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with CodecReg {

  lazy val ws: WSClient = app.injector.instanceOf[WSClient]
  lazy val testClientStore: ClientStore = app.injector.instanceOf[ClientStore]

  val testToken: String = MockToken.build(
    clientId = "test-client-id",
    userId = "test-user-id",
    scope = "delete:app"
  )

  val testInvalidToken: String = MockToken.buildInvalid(
    clientId = "test-client-id",
    userId = "test-user-id",
    scope = "read:test,write:test"
  )

  val testClient: Client = Client(
    "test-user-id",
    "test app",
    "test desc",
    "testHomeUrl",
    Seq(),
    "confidential",
    false
  )

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testClientStore.ensureMultipleIndexes[Client](testClientStore))
    await(testClientStore.collection[Client].drop().toFuture())
  }

  override def beforeEach(): Unit = {
    super.beforeAll()
    await(testClientStore.collection[Client].drop().toFuture())
  }

  override def afterEach(): Unit = {
    super.beforeAll()
    await(testClientStore.collection[Client].drop().toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testClientStore.collection[Client].drop().toFuture())
  }

  "DELETE /client" should {
    "return a NoContent" when {
      "the client was delete" in {
        await(testClientStore.createClient(testClient))

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .delete()

        awaitAndAssert(result) {
          _.status mustBe NO_CONTENT
        }
      }
    }

    "return a BadRequest" when {
      "the request doesn't contain the correct query parameters" in {
        val result = ws
          .url(s"${testAppUrl}/client?id_error=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .delete()

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe BAD_REQUEST
          body.\("error").as[String] mustBe "Invalid parameters"
          body.\("message").as[String] mustBe "The supplied parameters are invalid. The correct parameters are one of client_id or id."
        }
      }
    }

    "return a Forbidden" when {
      "the access token doesn't contain the correct scopes for this request" in {
        val testToken: String = MockToken.build(
          clientId = "test-client-id",
          userId = "test-user-id",
          scope = "test-scope-invalid"
        )

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .delete()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are delete:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .delete()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .delete()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }
    }
  }
}
