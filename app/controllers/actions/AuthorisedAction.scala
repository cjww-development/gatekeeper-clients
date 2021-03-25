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

package controllers.actions

import models.{ErrorResponses, UserResult}
import org.slf4j.Logger
import pdi.jwt.{Jwt, JwtAlgorithm}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._

import scala.concurrent.{Future, ExecutionContext => ExC}

trait AuthorisedAction {
  self: BaseController =>

  type AuthReq[T] = Request[AnyContent] => T => Future[Result]
  type JsonAuthReq[T] = Request[JsValue] => T => Future[Result]

  val logger: Logger

  val currentSignature: String

  def userAuthorisation[T](f: => AuthReq[T])(implicit userResult: UserResult[T], ec: ExC): Action[AnyContent] = {
    Action.async { req =>
      getAuthToken(req.headers) match {
        case Some(token) => validateToken[T](token).fold(invalidTokenResult())(uR => f(req)(uR))
        case None => invalidTokenResult()
      }
    }
  }

  def scopeValidation(validScopes: Set[String], currentScopes: Set[String])(f: => Future[Result]): Future[Result] = {
    if(validScopes.intersect(currentScopes).nonEmpty) {
      logger.info("[scopeValidation] - Scopes validated")
      f
    } else {
      logger.warn(s"[scopeValidation] - The provided scopes were invalid for this request. " +
        s"Valid: ${validScopes.mkString(",")} v requested: ${currentScopes.mkString(",")}")
      Future.successful(Forbidden(ErrorResponses.invalidScopes(validScopes)))
    }
  }

  private val getAuthToken: Headers => Option[String] = _.get("Authorization") match {
    case Some(token) => if(token.startsWith("Bearer ")) Some(token.split(" ").last.trim) else {
      logger.warn(s"[userAuthorisation] - The auth token was not prefixed with 'Bearer '")
      None
    }
    case None =>
      logger.warn(s"[userAuthorisation] - The auth token is missing")
      None
  }

  private def validateToken[T](token: String)(implicit userResult: UserResult[T]): Option[T] = {
    //TODO: Validate token with Public key

    if(Jwt.isValid(token, currentSignature, Seq(JwtAlgorithm.HS512))) {
      val (_, payload, _) = Jwt.decodeAll(token, currentSignature, Seq(JwtAlgorithm.HS512)).get
      val json = Json.parse(payload.toJson)
      logger.info(s"[userAuthorisation] - Token has been validated")
      Some(userResult.toUserResult(json))
    } else {
      logger.warn(s"[userAuthorisation] - Provided token was not valid")
      None
    }
  }

  private val invalidTokenResult: () => Future[Result] = () => Future.successful(Forbidden(ErrorResponses.invalidAuthHeader))
}
