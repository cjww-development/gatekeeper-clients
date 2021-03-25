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

class UpdateClientISpec
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
    scope = "update:app"
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

  "PATCH /client" should {
    "return an Ok" when {
      "the client was updated" in {
        await(testClientStore.createClient(testClient))

        val testNewClient = Json.parse(
          """
            |{
            |  "name": "test app updated",
            |  "desc": "test desc updated",
            |  "home_url": "http://localhost:3000",
            |  "redirects": ["http://localhost:3000/auth/callback"],
            |  "valid_flows": ["authorization_code", "client_credentials", "refresh_token"],
            |  "default_scopes": ["openid", "profile", "email"],
            |  "custom_scopes": ["test-custom-scope"],
            |  "id_token_expiry": 300001,
            |  "access_token_expiry": 300001,
            |  "refresh_token_expiry": 3600001,
            |  "is_private": true,
            |  "authorised_users": [ "user-1", "user-2" ]
            |}
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch(testNewClient)

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe OK

          body.\("name").as[String] mustBe "test app updated"
          body.\("desc").as[String] mustBe "test desc updated"
          body.\("home_url").as[String] mustBe "http://localhost:3000"
          body.\("redirects").as[Seq[String]] mustBe Seq("http://localhost:3000/auth/callback")
          body.\("valid_flows").as[Seq[String]] mustBe Seq("authorization_code", "client_credentials", "refresh_token")
          body.\("default_scopes").as[Seq[String]] mustBe Seq("openid", "profile", "email")
          body.\("custom_scopes").as[Seq[String]] mustBe Seq("test-custom-scope")
          body.\("id_token_expiry").as[Long] mustBe 300001L
          body.\("access_token_expiry").as[Long] mustBe 300001L
          body.\("refresh_token_expiry").as[Long] mustBe 3600001L
          body.\("is_private").as[Boolean] mustBe true
          body.\("authorised_users").as[Seq[String]] mustBe Seq("user-1", "user-2")
        }
      }
    }

    "return a NotFound" when {
      "the client was not found after an update" in {
        val testNewClient = Json.parse(
          """
            |{
            |  "name": "test app updated",
            |  "desc": "test desc updated",
            |  "home_url": "http://localhost:3000",
            |  "redirects": ["http://localhost:3000/auth/callback"],
            |  "valid_flows": ["authorization_code", "client_credentials", "refresh_token"],
            |  "default_scopes": ["openid", "profile", "email"],
            |  "custom_scopes": ["test-custom-scope"],
            |  "id_token_expiry": 300001,
            |  "access_token_expiry": 300001,
            |  "refresh_token_expiry": 3600001,
            |  "is_private": true,
            |  "authorised_users": [ "user-1", "user-2" ]
            |}
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch(testNewClient)

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe NOT_FOUND
          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
        }
      }
    }

    "return a BadRequest" when {
      "the request doesn't contain the correct query parameters" in {
        val testNewClient = Json.parse(
          """
            |{
            |  "name": "test app updated",
            |  "desc": "test desc updated",
            |  "redirects": ["http://localhost:3000/auth/callback"],
            |  "valid_flows": ["authorization_code", "client_credentials", "refresh_token"],
            |  "default_scopes": ["openid", "profile", "email"],
            |  "custom_scopes": ["test-custom-scope"],
            |  "id_token_expiry": 300001,
            |  "access_token_expiry": 300001,
            |  "refresh_token_expiry": 3600001
            |}
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/client?id_error=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch(testNewClient)

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe BAD_REQUEST
          body.\("error").as[String] mustBe "Invalid parameters"
          body.\("message").as[String] mustBe "The supplied parameters are invalid. The correct parameters are one of client_id or id."
        }
      }

      "the request doesn't contain the correct fields in the body" in {
        val testNewClient = Json.parse(
          """
            |{
            |  "name_abc": "test app updated",
            |  "desc_": "test desc updated",
            |  "homeee_urll": "http://localhost:3000",
            |  "reddirects": ["http://localhost:3000/auth/callback"],
            |  "valid__flows": ["authorization_code", "client_credentials", "refresh_token"],
            |  "defaultt_scopes": ["openid", "profile", "email"],
            |  "custom_sscopes": ["test-custom-scope"],
            |  "id_tokken_expiry": 300001,
            |  "accesss_token_expiry": 300001,
            |  "refresh_token_expiryyy": 3600001,
            |  "is___private": true,
            |  "auth_users": [ "user-1", "user-2" ]
            |}
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch(testNewClient)

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe BAD_REQUEST
          body.\("error").as[String] mustBe "Malformed request body"
          body.\("message").as[String] mustBe "The request body was not in the correct structure. Review the docs for the correct request body for this API."
        }
      }

      "the correct body fields are present but the values are invalid" in {
        val testNewClient = Json.parse(
          """
            |{
            |  "home_url": "htp://localhost:3000",
            |  "redirects": ["htttp://localhost:3000/auth/callback"],
            |  "valid_flows": ["auth_code"],
            |  "default_scopes": ["openidd"],
            |  "id_token_expiry": 299999,
            |  "access_token_expiry": 299999,
            |  "refresh_token_expiryyy": 3599999
            |}
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch(testNewClient)

        awaitAndAssert(result) { res =>
          res.status mustBe BAD_REQUEST
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
          .patch("")

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are update:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .patch("")

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
          .patch("")

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }
    }
  }

  "PATCH /client/regenerate-credentials" should {
    "return an Ok" when {
      "the confidential client was given a new client Id and secret" in {
        await(testClientStore.createClient(testClient))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=$clientId&is_confidential=true")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe OK

          body.\("message").as[String] mustBe "Client Id and secret regenerated"
        }
      }

      "the public client was given a new client Id" in {
        await(testClientStore.createClient(testClient.copy(clientType = "public", clientSecret = None)))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=$clientId&is_confidential=false")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe OK

          body.\("message").as[String] mustBe "Client Id regenerated"
        }
      }
    }

    "return a NotFound" when {
      "the client was not found after an update" in {
        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=${testClient.clientId}&is_confidential=true")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe NOT_FOUND

          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
        }
      }
    }

    "return a BadRequest" when {
      "the client is confidential but is_confidential is false" in {
        await(testClientStore.createClient(testClient))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=$clientId&is_confidential=false")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe BAD_REQUEST

          body.\("error").as[String] mustBe "Client type mismatch"
          body.\("message").as[String] mustBe "The is_confidential query parameter did not match the clients type."
        }
      }

      "the client is public but is_confidential is true" in {
        await(testClientStore.createClient(testClient.copy(clientType = "public", clientSecret = None)))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=$clientId&is_confidential=true")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe BAD_REQUEST

          body.\("error").as[String] mustBe "Client type mismatch"
          body.\("message").as[String] mustBe "The is_confidential query parameter did not match the clients type."
        }
      }

      "the request did not contain the correct query parameters" in {
        await(testClientStore.createClient(testClient))

        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?clienttt_id=${testClient.clientId}&is__confidential=true")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          val body = res.json

          res.status mustBe BAD_REQUEST

          body.\("error").as[String] mustBe "Invalid parameters"
          body.\("message").as[String] mustBe "The supplied parameters are invalid. The correct parameters are one of client_id or id and is_confidential."
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
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=${testClient.clientId}&is_confidential=true")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .patch("")

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are update:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=${testClient.clientId}&is_confidential=true")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .patch("")

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val result = ws
          .url(s"${testAppUrl}/client/regenerate-credentials?client_id=${testClient.clientId}&is_confidential=true")
          .patch("")

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
