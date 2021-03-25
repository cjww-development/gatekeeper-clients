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

import play.api.libs.json.{JsError, JsObject, JsPath, JsSuccess, JsValue, Reads}

object ClientValidation {

  val validRedirects: Reads[Seq[String]] = (json: JsValue) => {
    val redirects = json.as[Seq[String]]
    if(redirects.forall(url => url.startsWith("http://") || url.startsWith("https://"))) {
      JsSuccess(redirects)
    } else {
      JsError(JsPath.\("redirects"), "Valid redirects start with http:// or https://")
    }
  }

  val validHomeUrl: Reads[String] = (json: JsValue) => {
    val homeUrl = json.as[String]
    if(homeUrl.startsWith("http://") || homeUrl.startsWith("https://")) {
      JsSuccess(homeUrl)
    } else {
      JsError(JsPath.\("home_url"), "Valid home urls start with http:// or https://")
    }
  }

  val validFlows: Reads[Seq[String]] = (json: JsValue) => {
    val validValues = Seq("authorization_code", "client_credentials", "refresh_token")

    val flows = json.as[Seq[String]]
    if(flows.forall(flow => validValues.contains(flow))) {
      JsSuccess(flows)
    } else {
      JsError(JsPath.\("valid_flows"), s"Valid flows are: ${validValues.mkString(", ")}")
    }
  }

  val validDefaultScopes: Reads[Seq[String]] = (json: JsValue) => {
    val validValues = Seq("openid", "profile", "email", "address", "phone")

    val scopes = json.as[Seq[String]]
    if(scopes.forall(scp => validValues.contains(scp))) {
      JsSuccess(scopes)
    } else {
      JsError(JsPath.\("default_scopes"), s"Valid default scopes are: ${validValues.mkString(", ")}")
    }
  }

  val tokenExpiry: Reads[Long] = (json: JsValue) => {
    val expiry = json.as[Long]
    if(expiry >= 300000L & expiry <= 86400000L) {
      JsSuccess(expiry)
    } else {
      JsError(s"The token expiry should be between 300000ms (5 mins) and 86400000ms (1 day)")
    }
  }

  val refreshTokenExpiry: Reads[Long] = (json: JsValue) => {
    val expiry = json.as[Long]
    if(expiry >= 3600000L & expiry <= 315360000000L) {
      JsSuccess(expiry)
    } else {
      JsError(s"The token expiry should be between 3600000ms (60 mins) and 315360000000ms (3650 days)")
    }
  }

  val validateClientQuery: Reads[Seq[(String, String)]] = (json: JsValue) => {
    val clients = json.as[Seq[JsObject]]
      .map(_.fields.map(tuple => (tuple._1, tuple._2.as[String])))

    val validNestedLength = clients.forall(_.size == 1)
    if(validNestedLength) {
      val flattenedClients = clients.flatten
      val validFieldNames = flattenedClients.forall(kv => kv._1 == "client_id" || kv._1 == "id")

      if(validFieldNames) {
       JsSuccess(flattenedClients.map(kv => if(kv._1 == "client_id") "clientId" -> Client.stringObs.encrypt(kv._2) else kv))
      } else {
        JsError("Each nested json objects field should be either client_id or id")
      }
    } else {
      JsError("Each nested json object should contain only one field and value")
    }
  }
}
