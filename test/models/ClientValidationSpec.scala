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

class ClientValidationSpec extends PlaySpec with Assertions {

  "validateClientQuery" should {
    "return a sequence of tuples" when {
      "given valid json input" in {

        val testJson = Json.parse(
          """
            |[
            | { "client_id" : "test-id-1" },
            | { "id" : "test-id-2" },
            | { "client_id" : "test-id-3" },
            | { "id" : "test-id-4" }
            |]
          """.stripMargin
        )

        assertOutput(Json.fromJson(testJson)(ClientValidation.validateClientQuery)) {
          _.get mustBe Seq(("clientId", Client.stringObs.encrypt("test-id-1")), ("id", "test-id-2"), ("clientId", Client.stringObs.encrypt("test-id-3")), ("id", "test-id-4"))
        }
      }
    }

    "return a JsError" when {
      "nested objects contain more than one field and value" in {
        val testJson = Json.parse(
          """
            |[
            | {
            |   "client_id" : "test-id-1",
            |   "id" : "test-id-invalid"
            | },
            | { "id" : "test-id-2" },
            | { "client_id" : "test-id-3" },
            | { "id" : "test-id-4" }
            |]
          """.stripMargin
        )

        assertOutput(Json.fromJson(testJson)(ClientValidation.validateClientQuery)) { res =>
          assert(res.isError)
          res mustBe JsError("Each nested json object should contain only one field and value")
        }
      }

      "nested objects contain invalid field names" in {
        val testJson = Json.parse(
          """
            |[
            | { "client_id" : "test-id-1" },
            | { "id_invalid" : "test-id-2" },
            | { "client_id" : "test-id-3" },
            | { "id" : "test-id-4" }
            |]
          """.stripMargin
        )

        assertOutput(Json.fromJson(testJson)(ClientValidation.validateClientQuery)) { res =>
          assert(res.isError)
          res mustBe JsError("Each nested json objects field should be either client_id or id")
        }
      }
    }
  }
}
