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

package global

import com.cjwwdev.http.request.RequestErrorHandler
import play.api.http.{Status, Writeable}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Call, RequestHeader}

import javax.inject.Inject

class ErrorHandler @Inject()() extends RequestErrorHandler[JsValue] {
  val jsonResponse: (Int, String) => JsValue = (status, msg) => Json.obj(
    "status" -> status,
    "msg" -> msg
  )

  override implicit val writer: Writeable[JsValue] = Writeable.writeableOf_JsValue
  override def standardError(status: Int)(implicit rh: RequestHeader): JsValue = jsonResponse(status, "Something went wrong")
  override def notFoundError(implicit rh: RequestHeader): JsValue = jsonResponse(Status.NOT_FOUND, "Resource not found")
  override def serverError(implicit rh: RequestHeader): JsValue = jsonResponse(Status.INTERNAL_SERVER_ERROR, "Something went wrong")
  override def forbiddenError(implicit rh: RequestHeader): Either[JsValue, Call] = Left(jsonResponse(Status.FORBIDDEN, "You are not authorised to access this resource"))
}
