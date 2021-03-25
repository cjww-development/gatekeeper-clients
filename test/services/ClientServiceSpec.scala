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

package services

import com.cjwwdev.mongo.responses.MongoFailedUpdate
import database.ClientStore
import helpers.Assertions
import mocks.database.MockClientStore
import models.{Client, ClientUpdate}
import org.bson.BsonValue
import org.bson.conversions.Bson
import org.mongodb.scala.bson.BsonString
import org.scalatestplus.play.PlaySpec

import scala.concurrent.ExecutionContext.Implicits.global

class ClientServiceSpec extends PlaySpec with MockClientStore with Assertions {

  val testService: ClientService = new ClientService {
    override val clientStore: ClientStore = mockClientStore
  }

  val testClient: Client = Client(
    "testOwner",
    "testName",
    "testDesc",
    "testHomeUrl",
    Seq("testRedirect1"),
    "confidential",
    false
  )

  "getClientById" should {
    "return a client" when {
      "a client has been found matching the owner and app Id" in {
        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.getClientBy("testOwner", ("id", testClient.id))) {
          _ mustBe Some(Client.decode(testClient))
        }
      }

      "a client has been found matching the owner and client Id" in {
        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.getClientBy("testOwner", ("clientId", testClient.clientId))) {
          _ mustBe Some(Client.decode(testClient))
        }
      }
    }

    "return none" when {
      "no client has been found matching the owner and app Id" in {
        mockGetClientOn(client = None)

        awaitAndAssert(testService.getClientBy("testOwner", ("id", "some-app-id"))) {
          _ mustBe None
        }
      }

      "no client has been found matching the owner and client Id" in {
        mockGetClientOn(client = None)

        awaitAndAssert(testService.getClientBy("testOwner", ("clientId", "some-client-id"))) {
          _ mustBe None
        }
      }
    }
  }

  "getOwnedClients" should {
    "return a seq of seq of clients" when {
      "there are clients are they're being grouped in one's" in {
        mockGetClientsOn(clients = Seq(testClient, testClient))

        val decodedClient = Client.decode(testClient)

        awaitAndAssert(testService.getOwnedClients("testOwner", groupsOf = 1)) {
          _ mustBe Seq(Seq(decodedClient), Seq(decodedClient))
        }
      }

      "there are clients are they're being grouped in two's" in {
        mockGetClientsOn(clients = Seq(testClient, testClient, testClient, testClient))

        val decodedClient = Client.decode(testClient)

        awaitAndAssert(testService.getOwnedClients("testOwner", groupsOf = 2)) {
          _ mustBe Seq(Seq(decodedClient, decodedClient), Seq(decodedClient, decodedClient))
        }
      }
    }

    "return an empty seq" when {
      "there are no clients to return" in {
        mockGetClientsOn(clients = Seq.empty)

        awaitAndAssert(testService.getOwnedClients("testOwner", groupsOf = 1)) {
          _ mustBe Seq.empty
        }
      }
    }
  }

  "getClient" should {
    "return a Client" when {
      "the client has been found and it's not private" in {
        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.getClient("test-user-id", ("id", testClient.id))) {
          _ mustBe Some(Client.decode(testClient))
        }
      }

      "the client has been found, it's private and the user is in the clients authorised user list" in {
        val testClient: Client = Client(
          "testOwner",
          "testName",
          "testDesc",
          "testHomeUrl",
          Seq("testRedirect1"),
          "confidential",
          true
        ).copy(authorisedUsers = Seq("test-authorised-user"))

        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.getClient("test-authorised-user", ("id", testClient.id))) {
          _ mustBe Some(Client.decode(testClient))
        }
      }

      "the client has been found, it's private and the user is not in the clients authorised user list but the user owns the client" in {
        val testClient: Client = Client(
          "testOwner",
          "testName",
          "testDesc",
          "testHomeUrl",
          Seq("testRedirect1"),
          "confidential",
          true
        ).copy(authorisedUsers = Seq("test-authorised-user"))

        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.getClient("testOwner", ("id", testClient.id))) {
          _ mustBe Some(Client.decode(testClient))
        }
      }
    }

    "return None" when {
      "the client has been found, the client is private but the user isn't in the authorised user list" in {
        val testClient: Client = Client(
          "testOwner",
          "testName",
          "testDesc",
          "testHomeUrl",
          Seq("testRedirect1"),
          "confidential",
          true
        ).copy(authorisedUsers = Seq("test-authorised-user"))

        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.getClient("test-unauthorised-user", ("id", testClient.id))) {
          _ mustBe None
        }
      }

      "the client has not been found" in {
        mockGetClientOn(client = None)

        awaitAndAssert(testService.getClient("test-authorised-user", ("id", "test-invalid-app-id"))) {
          _ mustBe None
        }
      }
    }
  }

  "updateClient" should {
    val update = ClientUpdate(
      redirects = Some(Seq("http://localhost:5600/test"))
    )

    "return a Client" when {
      "the app has been updated and the app has been fetched" in {
        mockUpdateClient(success = true)

        mockGetClientOn(client = Some(testClient))

        awaitAndAssert(testService.updateClient(testClient.owner, ("id", testClient.id),update)) {
          _ mustBe Right(Some(testClient))
        }
      }
    }

    "return None" when {
      "the app could not be found" in {
        mockUpdateClient(success = true)

        mockGetClientOn(client = None)

        awaitAndAssert(testService.updateClient(testClient.owner, ("id", testClient.id),update)) {
          _ mustBe Right(None)
        }
      }
    }

    "return a MongoFailedUpdate" when {
      "the specified app could not be updated" in {
        mockUpdateClient(success = false)

        awaitAndAssert(testService.updateClient(testClient.owner, ("id", testClient.id),update)) {
          _ mustBe Left(MongoFailedUpdate)
        }
      }
    }
  }

  "regenerateCredentials" should {
    "return a RegeneratedId response" when {
      "the clients clientId has been regenerated and the the confidential check has passed" in {
        mockGetClientAndProject(projection = Map("clientType" -> BsonString("public")))

        mockUpdateClient(success = true)

        awaitAndAssert(testService.regenerateCredentials("testOwner", ("clientId", testClient.clientId), isConfidential = false )) {
          _ mustBe Right(RegeneratedId)
        }
      }
    }

    "return a RegeneratedIdAndSecret response" when {
      "the clients clientId and clientSecret has been regenerated" in {
        mockGetClientAndProject(projection = Map("clientType" -> BsonString("confidential")))

        mockUpdateClient(success = true)

        awaitAndAssert(testService.regenerateCredentials("testOwner", ("clientId", testClient.clientId), isConfidential = true )) {
          _ mustBe Right(RegeneratedIdAndSecret)
        }
      }
    }

    "return a InvalidClientType response" when {
      "isConfidential is true but the client is actually public" in {
        mockGetClientAndProject(projection = Map("clientType" -> BsonString("public")))

        awaitAndAssert(testService.regenerateCredentials("testOwner", ("clientId", testClient.clientId), isConfidential = true )) {
          _ mustBe Right(InvalidClientType)
        }
      }

      "isConfidential is false but the client is actually confidential" in {
        mockGetClientAndProject(projection = Map("clientType" -> BsonString("confidential")))

        awaitAndAssert(testService.regenerateCredentials("testOwner", ("clientId", testClient.clientId), isConfidential = false )) {
          _ mustBe Right(InvalidClientType)
        }
      }
    }

    "return a InvalidClient response" when {
      "the client does not exist" in {
        mockGetClientAndProject(projection = Map())

        awaitAndAssert(testService.regenerateCredentials("testOwner", ("clientId", testClient.clientId), isConfidential = false )) {
          _ mustBe Right(InvalidClient)
        }
      }
    }

    "return a MongoFailedUpdate response" when {
      "there was a problem regenerating the credentials for the client" in {
        mockGetClientAndProject(projection = Map("clientType" -> BsonString("confidential")))

        mockUpdateClient(success = false)

        awaitAndAssert(testService.regenerateCredentials("testOwner", ("clientId", testClient.clientId), isConfidential = true )) {
          _ mustBe Left(MongoFailedUpdate)
        }
      }
    }
  }
}
