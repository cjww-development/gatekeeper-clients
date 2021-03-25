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
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WSClient
import play.api.test.Helpers._

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits._

class FetchClientISpec
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
    scope = "read:app"
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

  val testPublicClient: Client = Client(
    "test-user-id",
    "test app",
    "test desc",
    "testHomeUrl",
    Seq(),
    "public",
    false
  )

  val dropQuery: Bson = mongoEqual("owner", testClient.owner)

  override def beforeAll(): Unit = {
    super.beforeAll()
    await(testClientStore.ensureMultipleIndexes[Client](testClientStore))
    await(testClientStore.collection[Client].deleteMany(dropQuery).toFuture())
  }

  override def beforeEach(): Unit = {
    super.beforeAll()
    await(testClientStore.collection[Client].deleteMany(dropQuery).toFuture())
  }

  override def afterEach(): Unit = {
    super.beforeAll()
    await(testClientStore.collection[Client].deleteMany(dropQuery).toFuture())
  }

  override def afterAll(): Unit = {
    super.afterAll()
    await(testClientStore.collection[Client].deleteMany(dropQuery).toFuture())
  }

  "GET /client" should {
    "return an Ok" when {
      "the user has been authorised and the client has been found based on the app Id (Confidential)" in {
        await(testClientStore.createClient(testClient))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
          body.\("client_type").as[String] mustBe testClient.clientType
          body.\("client_id").as[String] mustBe clientId
          body.\("client_secret").as[String] mustBe clientSec
        }
      }

      "the user has been authorised and the client has been found based on the client Id (Confidential)" in {
        await(testClientStore.createClient(testClient))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client?client_id=$clientId")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
          body.\("client_type").as[String] mustBe testClient.clientType
          body.\("client_id").as[String] mustBe clientId
          body.\("client_secret").as[String] mustBe clientSec
        }
      }

      "the user has been authorised and the client has been found based on the app Id (Public)" in {
        await(testClientStore.createClient(testPublicClient))

        val clientId = Client.stringDeObfuscate.decrypt(testPublicClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client?id=${testPublicClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("name").as[String] mustBe testPublicClient.name
          body.\("desc").as[String] mustBe testPublicClient.desc
          body.\("client_type").as[String] mustBe testPublicClient.clientType
          body.\("client_id").as[String] mustBe clientId
          body.\("client_secret").asOpt[String] mustBe None
        }
      }

      "the user has been authorised and the client has been found based on the client Id (Public)" in {
        await(testClientStore.createClient(testPublicClient))

        val clientId = Client.stringDeObfuscate.decrypt(testPublicClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client?client_id=$clientId")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("name").as[String] mustBe testPublicClient.name
          body.\("desc").as[String] mustBe testPublicClient.desc
          body.\("client_type").as[String] mustBe testPublicClient.clientType
          body.\("client_id").as[String] mustBe clientId
          body.\("client_secret").asOpt[String] mustBe None
        }
      }
    }

    "return a BadRequest" when {
      "the user has been authorised but the requests query parameters are invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client?i_d=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe BAD_REQUEST
          val body = res.json
          body.\("error").as[String] mustBe "Invalid parameters"
          body.\("message").as[String] mustBe "The supplied parameters are invalid. Valid parameters are id or clientId."
        }
      }
    }

    "return a NotFound" when {
      "the user has been authorised and the client hasn't been found based on the app Id" in {
        val result = ws
          .url(s"${testAppUrl}/client?id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe NOT_FOUND
          val body = res.json
          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
        }
      }

      "the user has been authorised and the client hasn't been found based on the client Id" in {
        val result = ws
          .url(s"${testAppUrl}/client?client_id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe NOT_FOUND
          val body = res.json
          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
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
          .url(s"${testAppUrl}/client?client_id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are read:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client?client_id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val result = ws
          .url(s"${testAppUrl}/client?client_id=abc123")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }
    }
  }

  "GET /client/details" should {
    "return an Ok" when {
      "the user has been authorised and the client has been found based on the app Id (Confidential)" in {
        await(testClientStore.createClient(testClient))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("owner").as[String] mustBe testClient.owner
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the client has been found based on the client Id (Confidential)" in {
        await(testClientStore.createClient(testClient))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?client_id=$clientId")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("owner").as[String] mustBe testClient.owner
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the client has been found based on the app Id (Public)" in {
        await(testClientStore.createClient(testPublicClient))

        val clientId = Client.stringDeObfuscate.decrypt(testPublicClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?id=${testPublicClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("owner").as[String] mustBe testClient.owner
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the client has been found based on the client Id (Public)" in {
        await(testClientStore.createClient(testPublicClient))

        val clientId = Client.stringDeObfuscate.decrypt(testPublicClient.clientId).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?client_id=$clientId")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("owner").as[String] mustBe testClient.owner
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the client has been found because the client is private and user is in the authorised user list" in {
        await(testClientStore.createClient(testClient.copy(isPrivate = true, authorisedUsers = Seq("test-other-user-id"))))

        val testToken: String = MockToken.build(
          clientId = "test-client-id",
          userId = "test-other-user-id",
          scope = "read:app"
        )

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("owner").as[String] mustBe testClient.owner
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the client has been found because the client is private and user isn't in the authorised user list, but the user owns the client" in {
        await(testClientStore.createClient(testClient.copy(isPrivate = true, authorisedUsers = Seq("test-other-user-id"))))

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json
          body.\("owner").as[String] mustBe testClient.owner
          body.\("name").as[String] mustBe testClient.name
          body.\("desc").as[String] mustBe testClient.desc
        }
      }
    }

    "return a BadRequest" when {
      "the user has been authorised but the requests query parameters are invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client/details?i_d=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe BAD_REQUEST
          val body = res.json
          body.\("error").as[String] mustBe "Invalid parameters"
          body.\("message").as[String] mustBe "The supplied parameters are invalid. Valid parameters are id or clientId."
        }
      }
    }

    "return a NotFound" when {
      "the user has been authorised and the client hasn't been found based on the app Id" in {
        val result = ws
          .url(s"${testAppUrl}/client/details?id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe NOT_FOUND
          val body = res.json
          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
        }
      }

      "the user has been authorised and the client hasn't been found based on the client Id" in {
        val result = ws
          .url(s"${testAppUrl}/client/details?client_id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe NOT_FOUND
          val body = res.json
          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
        }
      }

      "the user has been authorised and the client hasn't been found because the client is private and user isn't in the authorised user list" in {
        await(testClientStore.createClient(testClient.copy(isPrivate = true)))

        val testToken: String = MockToken.build(
          clientId = "test-client-id",
          userId = "test-other-user-id",
          scope = "read:app"
        )

        val clientId = Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse("")
        val clientSec = Client.stringDeObfuscate.decrypt(testClient.clientSecret.get).getOrElse("")

        val result = ws
          .url(s"${testAppUrl}/client/details?id=${testClient.id}")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe NOT_FOUND
          val body = res.json
          body.\("error").as[String] mustBe "Client not found"
          body.\("message").as[String] mustBe "There was no matching client found."
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
          .url(s"${testAppUrl}/client/details?client_id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are read:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/client/details?client_id=abc123")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val result = ws
          .url(s"${testAppUrl}/client/details?client_id=abc123")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }
    }
  }

  "GET /clients" should {
    "return an Ok" when {
      "there are clients owned by the user" in {
        await(testClientStore.createClient(testClient))

        val result = ws
          .url(s"${testAppUrl}/clients")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe OK
          val body = res.json.as[Seq[Seq[JsValue]]]
          body.length mustBe 1
          body.head.length mustBe 1
          body.head.head.\("name").as[String] mustBe testClient.name
        }
      }
    }

    "return a NoContent" when {
      "there are no clients owned by the user" in {
        val result = ws
          .url(s"${testAppUrl}/clients")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) {
          _.status mustBe NO_CONTENT
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
          .url(s"${testAppUrl}/clients")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are read:app."
        }
      }

      "the access token is invalid" in {
        val result = ws
          .url(s"${testAppUrl}/clients")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val result = ws
          .url(s"${testAppUrl}/clients")
          .get()

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }
    }
  }

  "POST /client/details" should {
    "return an Ok" when {
      "the user has been authorised and the clients details have been found (singular)" in {
        await(testClientStore.createClient(testClient))

        val clientQuery = Json.parse(
          s"""
            |[
            | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
            |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          val body = res.json.as[Seq[JsValue]]

          res.status mustBe OK
          body.head.\("owner").as[String] mustBe testClient.owner
          body.head.\("name").as[String] mustBe testClient.name
          body.head.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the clients details have been found (multiple)" in {
        await(testClientStore.createClient(testClient))
        await(testClientStore.createClient(testPublicClient))

        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" },
             | { "id" : "${testPublicClient.id}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          val body = res.json.as[Seq[JsValue]]

          res.status mustBe OK
          body.head.\("owner").as[String] mustBe testClient.owner
          body.head.\("name").as[String] mustBe testClient.name
          body.head.\("desc").as[String] mustBe testClient.desc

          body.last.\("owner").as[String] mustBe testPublicClient.owner
          body.last.\("name").as[String] mustBe testPublicClient.name
          body.last.\("desc").as[String] mustBe testPublicClient.desc
        }
      }

      "the user has been authorised and the client has been found because the client is private and user is in the authorised user list" in {
        await(testClientStore.createClient(testClient.copy(isPrivate = true, authorisedUsers = Seq("test-other-user-id"))))

        val testToken: String = MockToken.build(
          clientId = "test-client-id",
          userId = "test-other-user-id",
          scope = "read:app"
        )

        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          val body = res.json.as[Seq[JsValue]]

          res.status mustBe OK
          body.head.\("owner").as[String] mustBe testClient.owner
          body.head.\("name").as[String] mustBe testClient.name
          body.head.\("desc").as[String] mustBe testClient.desc
        }
      }

      "the user has been authorised and the client has been found because the client is private and user isn't in the authorised user list, but the user owns the client" in {
        await(testClientStore.createClient(testClient.copy(isPrivate = true, authorisedUsers = Seq("test-other-user-id"))))

        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          val body = res.json.as[Seq[JsValue]]

          res.status mustBe OK
          body.head.\("owner").as[String] mustBe testClient.owner
          body.head.\("name").as[String] mustBe testClient.name
          body.head.\("desc").as[String] mustBe testClient.desc
        }
      }
    }

    "return a BadRequest" when {
      "the user has been authorised but the requests body is invalid" in {
        val clientQuery = Json.parse(
          s"""
             |[
             | {
             |    "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}",
             |    "id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}"
             | }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          res.status mustBe BAD_REQUEST
        }
      }

      "the user has been authorised but the requests body fields are invalid" in {
        val clientQuery = Json.parse(
          s"""
             |[
             | { "client__id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          res.status mustBe BAD_REQUEST
        }
      }
    }

    "return a NoContent" when {
      "the user has been authorised and the client hasn't been found because the client is private and user isn't in the authorised user list" in {
        await(testClientStore.createClient(testClient.copy(isPrivate = true)))

        val testToken: String = MockToken.build(
          clientId = "test-client-id",
          userId = "test-other-user-id",
          scope = "read:app"
        )

        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          res.status mustBe NO_CONTENT
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

        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid scopes"
          body.\("message").as[String] mustBe "The access token did not contain the valid scopes for this request. Valid scopes are read:app."
        }
      }

      "the access token is invalid" in {
        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .withHttpHeaders("Authorization" -> s"Bearer $testInvalidToken")
          .post(clientQuery)

        awaitAndAssert(result) { res =>
          res.status mustBe FORBIDDEN
          val body = res.json
          body.\("error").as[String] mustBe "Invalid authorization header"
          body.\("message").as[String] mustBe "The authorization header was either missing or invalid. Check your header."
        }
      }

      "the access token is missing" in {
        val clientQuery = Json.parse(
          s"""
             |[
             | { "client_id" : "${Client.stringDeObfuscate.decrypt(testClient.clientId).getOrElse[String]("")}" }
             |]
          """.stripMargin
        )

        val result = ws
          .url(s"${testAppUrl}/clients/details")
          .post(clientQuery)

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
