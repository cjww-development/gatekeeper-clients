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
import org.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits._

class CreateClientISpec
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
    scope = "write:app"
  )

  val testInvalidToken: String = MockToken.buildInvalid(
    clientId = "test-client-id",
    userId = "test-user-id",
    scope = "read:test,write:test"
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

  "POST /client" should {
    "return an Ok" when {
      "the user has been authorised and the client has been created" in {
        val testNewClient = Json.parse(
          """
            |{
            | "name" : "test-app",
            | "desc" : "test description",
            | "home_url" : "http://localhost:3000",
            | "redirects" : [ "http://localhost:3000/auth/callback" ],
            | "client_type" : "confidential",
            | "is_private": false
            |}
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/client")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(testNewClient)

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe OK

          body.\("name").as[String] mustBe "test-app"
          body.\("desc").as[String] mustBe "test description"
          body.\("client_type").as[String] mustBe "confidential"
          body.\("home_url").as[String] mustBe "http://localhost:3000"
          body.\("redirects").as[Seq[String]] mustBe Seq("http://localhost:3000/auth/callback")
          body.\("is_private").as[Boolean] mustBe false
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
          .url(s"${testAppUrl}/client")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post("")

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are write:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .post("")

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val result = ws
          .url(s"${testAppUrl}/client")
          .post("")

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
