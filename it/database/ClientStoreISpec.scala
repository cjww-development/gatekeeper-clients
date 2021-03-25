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

package database

import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate, MongoSuccessDelete, MongoSuccessUpdate}
import helpers.{Assertions, IntegrationApp}
import models.Client
import org.bson.conversions.Bson
import org.joda.time.DateTime
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.model.Filters.{equal => mongoEqual}
import org.mongodb.scala.model.Updates.set
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.PlaySpec

import scala.collection.Seq
import scala.concurrent.ExecutionContext.Implicits.global

class ClientStoreISpec extends PlaySpec with IntegrationApp with Assertions with BeforeAndAfterAll with BeforeAndAfterEach with CodecReg {

  val testClientStore: ClientStore = app.injector.instanceOf[ClientStore]

  val now: DateTime = DateTime.now()

  val testClient: Client = Client(
    id                 = "testAppId1",
    owner              = "testOwner",
    name               = "test-app-name",
    desc               = "test desc",
    homeUrl            = "http://localhost:8080",
    redirects          = Seq("http://localhost:8080/rediect"),
    clientType         = "confidential",
    clientId           = "testClientId1",
    clientSecret       = Some("testClientSecret1"),
    validFlows         = Seq(),
    defaultScopes      = Seq(),
    customScopes       = Seq(),
    idTokenExpiry      = 0L,
    accessTokenExpiry  = 0L,
    refreshTokenExpiry = 0L,
    isPrivate = false,
    authorisedUsers = Seq(),
    createdAt          = DateTime.now()
  )

  val testClientTwo: Client = Client(
    id                 = "testAppId2",
    owner              = "testOwner",
    name               = "test-app-name",
    desc               = "test desc",
    homeUrl            = "http://localhost:8080",
    redirects          = Seq("http://localhost:8080/rediect"),
    clientType         = "confidential",
    clientId           = "testClientId2",
    clientSecret       = Some("testClientSecret2"),
    validFlows         = Seq(),
    defaultScopes      = Seq(),
    customScopes       = Seq(),
    idTokenExpiry      = 0L,
    accessTokenExpiry  = 0L,
    refreshTokenExpiry = 0L,
    isPrivate = false,
    authorisedUsers = Seq(),
    createdAt          = DateTime.now()
  )

  val testClientThree: Client = Client(
    id                 = "testAppId3",
    owner              = "testOwner",
    name               = "test-app-name",
    desc               = "test desc",
    homeUrl            = "http://localhost:8080",
    redirects          = Seq("http://localhost:8080/rediect"),
    clientType         = "confidential",
    clientId           = "testClientId3",
    clientSecret       = Some("testClientSecret3"),
    validFlows         = Seq(),
    defaultScopes      = Seq(),
    customScopes       = Seq(),
    idTokenExpiry      = 0L,
    accessTokenExpiry  = 0L,
    refreshTokenExpiry = 0L,
    isPrivate = false,
    authorisedUsers = Seq(),
    createdAt          = DateTime.now()
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

  "createClient" should {
    "return a MongoSuccessCreate" when {
      "a new client has been created" in {
        awaitAndAssert(testClientStore.createClient(testClient)) {
          _ mustBe MongoSuccessCreate
        }
      }
    }

    "return a MongoFailedCreate" when {
      "a new client already has the same details as another client" in {
        await(testClientStore.createClient(testClient))
        awaitAndAssert(testClientStore.createClient(testClient)) {
          _ mustBe MongoFailedCreate
        }
      }
    }
  }

  "getClientOn" should {
    "return a Client" when {
      "a client already exists with a matching clientId" in {
        await(testClientStore.createClient(testClient))

        awaitAndAssert(testClientStore.getClientOn(mongoEqual("clientId", testClient.clientId))) {
          _ mustBe Some(testClient)
        }
      }

      "a client already exists with a matching clientSecret" in {
        await(testClientStore.createClient(testClient))

        awaitAndAssert(testClientStore.getClientOn(mongoEqual("clientSecret", testClient.clientSecret.get))) {
          _ mustBe Some(testClient)
        }
      }
    }

    "return None" when {
      "a client doesn't exist with a matching clientId" in {
        awaitAndAssert(testClientStore.getClientOn(mongoEqual("clientId", "invalid-id"))) {
          _ mustBe None
        }
      }

      "a client doesn't exist with a matching clientSecret" in {
        awaitAndAssert(testClientStore.getClientOn(mongoEqual("clientSecret", "invalid-secret"))) {
          _ mustBe None
        }
      }
    }
  }

  "getClientAndProject" should {
    "return a map of values" when {
      "a client already exists and the specified values are projected" in {
        await(testClientStore.createClient(testClient))

        awaitAndAssert(testClientStore.getClientAndProject(mongoEqual("clientId", testClient.clientId), "clientType")) {
          _ mustBe Map(
            "id" -> BsonString(testClient.id),
            "clientType" -> BsonString("confidential")
          )
        }
      }
    }

    "return an empty map" when {
      "the client doesn't exist" in {
        awaitAndAssert(testClientStore.getClientAndProject(mongoEqual("clientId", "invalid-id"), "clientType")) {
          _ mustBe Map()
        }
      }
    }
  }

  "getClientsOn" should {
    "return a seq Client" when {
      "clients already exist with the same owner" in {
        await(testClientStore.createClient(testClient))
        await(testClientStore.createClient(testClientTwo))
        await(testClientStore.createClient(testClientThree))

        awaitAndAssert(testClientStore.getClientsOn(mongoEqual("owner", testClient.owner))) {
          _ mustBe Seq(testClient, testClientTwo, testClientThree)
        }
      }
    }

    "return an empty seq" when {
      "there are no matching clients" in {
        awaitAndAssert(testClientStore.getClientsOn(mongoEqual("owner", testClient.owner))) {
          _ mustBe Seq()
        }
      }
    }
  }

  "updateClient" should {
    "return a MongoSuccessUpdate" when {
      "the client has been updated" in {
        awaitAndAssert(testClientStore.createClient(testClient)) {
          _ mustBe MongoSuccessCreate
        }

        awaitAndAssert(testClientStore.updateClient(mongoEqual("clientId", testClient.clientId), set("desc", "new set desc"))) {
          _ mustBe MongoSuccessUpdate
        }

        awaitAndAssert(testClientStore.getClientOn(mongoEqual("clientId", testClient.clientId))) {
          _.get.desc mustBe "new set desc"
        }
      }
    }
  }

  "deleteClient" should {
    "return a MongoSuccessDelete" when {
      "the client has been deleted" in {
        awaitAndAssert(testClientStore.deleteClient(mongoEqual("clientId", testClient.clientId))) {
          _ mustBe MongoSuccessDelete
        }
      }
    }
  }


}
