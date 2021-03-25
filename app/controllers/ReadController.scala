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
import models.{Client, ClientValidation, ErrorResponses, ScopedOwner}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ClientService

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultReadController @Inject()(val controllerComponents: ControllerComponents,
                                      val config: Configuration,
                                      val featureService: FeatureService,
                                      val clientService: ClientService) extends ReadController {
  override implicit val ec: ExC = controllerComponents.executionContext
  override val logger: Logger = LoggerFactory.getLogger(this.getClass)
  override val currentSignature: String = config.get[String]("jwt.signature")
  override val validScopes: Set[String] = config.get[Seq[String]]("controllers.read-controller.scopes").toSet
}

trait ReadController extends MainController {

  implicit val ec: ExC

  val clientService: ClientService

  val validScopes: Set[String]

  def fetchClient(): Action[AnyContent] = userAuthorisation[ScopedOwner] { req => so =>
    scopeValidation(validScopes, so.scopes) {
      getSelector(req) match {
        case Some(sel) => clientService.getClientBy(so.sub, sel) map {
          _.fold(NotFound(ErrorResponses.clientNotFound))(client => Ok(Json.toJson(client)))
        }
        case None => Future.successful(BadRequest(ErrorResponses.invalidQueryParameters(
          "Valid parameters are id or clientId"
        )))
      }
    }
  }

  def fetchOwnedClients(groups_of: Int): Action[AnyContent] = userAuthorisation[ScopedOwner] { _ => so =>
    scopeValidation(validScopes, so.scopes) {
      clientService.getOwnedClients(so.sub, groups_of) map { clients =>
        if(clients.nonEmpty) Ok(Json.toJson(clients)) else NoContent
      }
    }
  }

  def fetchClientDetails(): Action[AnyContent] = userAuthorisation[ScopedOwner] { req => so =>
    scopeValidation(validScopes, so.scopes) {
      getSelector(req) match {
        case Some(sel) => clientService.getClient(so.sub, sel) map {
          _.fold(NotFound(ErrorResponses.clientNotFound))(client => Ok(Json.toJson(client)(Client.outboundDetailsWrites)))
        }
        case None => Future.successful(BadRequest(ErrorResponses.invalidQueryParameters(
          "Valid parameters are id or clientId"
        )))
      }
    }
  }

  def fetchClientsDetails(): Action[AnyContent] = userAuthorisation[ScopedOwner] { implicit req => so =>
    scopeValidation(validScopes, so.scopes) {
      withJsonBody[Seq[(String, String)]] { clients =>
        println(clients)
        clientService.getClientsDetails(so.sub, clients) map { clients =>
          if(clients.nonEmpty) Ok(Json.toJson(clients)(Client.outboundDetailsWritesSeq)) else NoContent
        }
      }(implicitly, ClientValidation.validateClientQuery)
    }
  }
}
