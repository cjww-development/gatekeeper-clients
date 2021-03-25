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

package controllers

import com.cjwwdev.featuremanagement.services.FeatureService
import models.UserResults._
import models.{ClientUpdate, ErrorResponse, ErrorResponses, ScopedOwner}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services._

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultUpdateController @Inject()(val controllerComponents: ControllerComponents,
                                        val config: Configuration,
                                        val featureService: FeatureService,
                                        val clientService: ClientService) extends UpdateController {
  override implicit val ec: ExC = controllerComponents.executionContext
  override val logger: Logger = LoggerFactory.getLogger(this.getClass)
  override val currentSignature: String = config.get[String]("jwt.signature")
  override val validScopes: Set[String] = config.get[Seq[String]]("controllers.update-controller.scopes").toSet
}

trait UpdateController extends MainController {

  implicit val ec: ExC

  val clientService: ClientService

  val validScopes: Set[String]

  def updateClient(): Action[AnyContent] = userAuthorisation[ScopedOwner] { implicit req => so =>
    scopeValidation(validScopes, so.scopes) {
      withJsonBody[ClientUpdate] { update =>
        getSelector(req) match {
          case Some(sel) => if(update.nonEmpty) {
            clientService.updateClient(so.sub, sel, update) map {
              _.fold(
                _ => InternalServerError(ErrorResponses.generalError),
                _.fold(NotFound(ErrorResponses.clientNotFound))(client => Ok(Json.toJson(client)))
              )
            }
          } else {
            Future.successful(BadRequest(
              ErrorResponses.malformedBody(
                "Review the docs for the correct request body for this API"
              )
            ))
          }
          case None => Future.successful(BadRequest(
            ErrorResponses.invalidQueryParameters(
              "The correct parameters are one of client_id or id"
            )
          ))
        }
      }
    }
  }

  def regenerateCredentials(): Action[AnyContent] = userAuthorisation[ScopedOwner] { implicit req => so =>
    val isConfidential = req
      .getQueryString("is_confidential")
      .filter(ic => ic == "true" || ic == "false")
      .map(_.toBoolean)

    scopeValidation(validScopes, so.scopes) {
      getSelector(req) -> isConfidential match {
        case (Some(sel), Some(isConfidential)) =>
          clientService.regenerateCredentials(so.sub, sel, isConfidential) map {
            _.fold(
              _ => InternalServerError(ErrorResponses.generalError),
              {
                case RegeneratedId => Ok(Json.obj("message" -> "Client Id regenerated"))
                case RegeneratedIdAndSecret => Ok(Json.obj("message" -> "Client Id and secret regenerated"))
                case InvalidClient => NotFound(ErrorResponses.clientNotFound)
                case InvalidClientType => BadRequest(ErrorResponse.toJson(
                  error = "Client type mismatch",
                  message = "The is_confidential query parameter did not match the clients type."
                ))
              }
            )
          }
        case (_, _) => Future.successful(BadRequest(
          ErrorResponses.invalidQueryParameters(
            "The correct parameters are one of client_id or id and is_confidential"
          )
        ))
      }
    }
  }
}
