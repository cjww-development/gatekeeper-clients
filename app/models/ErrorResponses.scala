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

import play.api.libs.json.JsValue

object ErrorResponses {
  val invalidQueryParameters: String => JsValue = supplementMessage => ErrorResponse.toJson(
    error = "Invalid parameters",
    message = s"The supplied parameters are invalid. $supplementMessage."
  )

  val clientNotFound: JsValue = ErrorResponse.toJson(
    error = "Client not found",
    message = "There was no matching client found."
  )

  val invalidAuthHeader: JsValue = ErrorResponse.toJson(
    error = "Invalid authorization header",
    message = "The authorization header was either missing or invalid. Check your header."
  )

  val invalidScopes: Set[String] => JsValue = scopes => ErrorResponse.toJson(
    error = "Invalid scopes",
    message = s"The access token did not contain the valid scopes for this request. Valid scopes are ${scopes.mkString(",")}."
  )

  val generalError: JsValue = ErrorResponse.toJson(
    error = "Unknown error",
    message = "There was an error processing the request. Please try again later."
  )

  val missingBody: JsValue = ErrorResponse.toJson(
    error = "Missing body",
    message = "No body was included with the request. Consult the docs to find the correct body."
  )

  val malformedBody: String => JsValue = supplementMessage => ErrorResponse.toJson(
    error = "Malformed request body",
    message = s"The request body was not in the correct structure. $supplementMessage."
  )

  val serviceUnavailable: JsValue = ErrorResponse.toJson(
    error = "Service unavailable",
    message = "This API is currently unavailable, try again later."
  )
}
