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

package models

import helpers.Assertions
import org.joda.time.DateTime
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsError, JsPath, Json}

class ClientSpec extends PlaySpec with Assertions {

  "newClientReads" should {
    "transform json into a valid client case class" when {
      "the client type is confidential" in {
        val testJson = Json.parse(
          """
            |{
            |    "name": "testName",
            |    "desc": "testDesc",
            |    "home_url": "http://localhost:3000",
            |    "redirects": [
            |        "http://localhost:3000/auth/callback",
            |        "http://localhost:3000/auth/redirect"
            |    ],
            |    "client_type": "confidential",
            |    "is_private": false
            |}
          """.stripMargin
        )

        val result = Json.fromJson[Client](testJson)(Client.newClientReads("testOwner")).get

        assertOutput(result) { res =>
          res.name           mustBe "testName"
          res.desc           mustBe "testDesc"
          res.clientType     mustBe "confidential"
          res.isPrivate      mustBe false

          assert(res.homeUrl != "http://localhost:3000")

          assert(res.redirects.head != "http://localhost:3000/auth/callback")
          assert(res.redirects.last !="http://localhost:3000/auth/redirect")
          assert(res.clientSecret.nonEmpty)

          assert(res.redirects.length == 2)
          assert(res.validFlows.isEmpty)
          assert(res.defaultScopes.isEmpty)
          assert(res.customScopes.isEmpty)

          res.idTokenExpiry      mustBe 3600000L
          res.accessTokenExpiry  mustBe 3600000L
          res.refreshTokenExpiry mustBe 2592000000L
        }
      }

      "the client type is public" in {
        val testJson = Json.parse(
          """
            |{
            |    "name": "testName",
            |    "desc": "testDesc",
            |    "home_url": "http://localhost:3000",
            |    "redirects": [
            |        "http://localhost:3000/auth/callback",
            |        "http://localhost:3000/auth/redirect"
            |    ],
            |    "client_type": "public",
            |    "is_private": true
            |}
          """.stripMargin
        )

        val result = Json.fromJson[Client](testJson)(Client.newClientReads("testOwner")).get

        assertOutput(result) { res =>
          res.name           mustBe "testName"
          res.desc           mustBe "testDesc"
          res.clientType     mustBe "public"
          res.isPrivate      mustBe true

          assert(res.homeUrl != "http://localhost:3000")

          assert(res.redirects.head != "http://localhost:3000/auth/callback")
          assert(res.redirects.last !="http://localhost:3000/auth/redirect")
          assert(res.clientSecret.isEmpty)

          assert(res.redirects.length == 2)
          assert(res.validFlows.isEmpty)
          assert(res.defaultScopes.isEmpty)
          assert(res.customScopes.isEmpty)

          res.idTokenExpiry      mustBe 3600000L
          res.accessTokenExpiry  mustBe 3600000L
          res.refreshTokenExpiry mustBe 2592000000L
        }
      }
    }

    "return an error" when {
      "the client_type is not confidential or public" in {
        val testJson = Json.parse(
          """
            |{
            |    "name": "testName",
            |    "desc": "testDesc",
            |    "home_url": "http://localhost:3000",
            |    "redirects": [
            |        "http://localhost:3000/auth/callback",
            |        "http://localhost:3000/auth/redirect"
            |    ],
            |    "client_type": "confidentiallll",
            |    "is_private": false
            |}
          """.stripMargin
        )

        val result = Json.fromJson[Client](testJson)(Client.newClientReads("testOwner"))

        assertOutput(result) { res =>
          assert(res.isError)
          res mustBe JsError(JsPath.\("client_type"), "Unsupported client type")
        }
      }

      "the home_url does not start with http or https" in {
        val testJson = Json.parse(
          """
            |{
            |    "name": "testName",
            |    "desc": "testDesc",
            |    "home_url": "htttp://localhost:3000",
            |    "redirects": [
            |        "http://localhost:3000/auth/callback",
            |        "http://localhost:3000/auth/redirect"
            |    ],
            |    "client_type": "confidential",
            |    "is_private": false
            |}
          """.stripMargin
        )

        val result = Json.fromJson[Client](testJson)(Client.newClientReads("testOwner"))

        assertOutput(result) { res =>
          assert(res.isError)
          res mustBe JsError(JsPath.\("home_url"), "Valid home urls start with http:// or https://")
        }
      }

      "a redirect does not start with http or https" in {
        val testJson = Json.parse(
          """
            |{
            |    "name": "testName",
            |    "desc": "testDesc",
            |    "home_url": "http://localhost:3000",
            |    "redirects": [
            |        "http://localhost:3000/auth/callback",
            |        "httpss://localhost:3000/auth/redirect"
            |    ],
            |    "client_type": "confidential",
            |    "is_private": false
            |}
          """.stripMargin
        )

        val result = Json.fromJson[Client](testJson)(Client.newClientReads("testOwner"))

        assertOutput(result) { res =>
          assert(res.isError)
          res mustBe JsError(JsPath.\("redirects"), "Valid redirects start with http:// or https://")
        }
      }
    }
  }

  "decode" should {
    "return a Client with plain text values" when {
      "the clients client id, secret and redirects were obfuscated" in {
        val now = DateTime.now()

        val testClient = Client(
          id = "testAppId",
          owner = "testOwner",
          name = "testName",
          desc = "testDesc",
          homeUrl = Client.stringObs.encrypt("testHomeUrl"),
          redirects = Seq(Client.stringObs.encrypt("testRedirect")),
          clientType = "confidential",
          clientId = Client.stringObs.encrypt("testClientId"),
          clientSecret = Some(Client.stringObs.encrypt("testClientSecret")),
          validFlows = Seq(),
          defaultScopes = Seq(),
          customScopes = Seq(),
          idTokenExpiry = 0L,
          accessTokenExpiry = 0L,
          refreshTokenExpiry = 0L,
          isPrivate = false,
          authorisedUsers = Seq(),
          createdAt = now
        )

        val result = Client.decode(testClient)

        assertOutput(result) { res =>
          assert(res.redirects != testClient.redirects)
          res.redirects.head mustBe "testRedirect"

          assert(res.clientId != testClient.clientId)
          res.clientId mustBe "testClientId"

          assert(res.homeUrl != testClient.homeUrl)
          res.homeUrl mustBe "testHomeUrl"

          assert(res.clientSecret != testClient.clientSecret)
          res.clientSecret mustBe Some("testClientSecret")
        }
      }
    }
  }
}
