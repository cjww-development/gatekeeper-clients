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
import com.cjwwdev.mongo.responses.{MongoFailedDelete, MongoSuccessDelete}
import models.UserResults._
import models.{ErrorResponses, ScopedOwner}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.mvc._
import services.ClientService

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

class DefaultDeleteController @Inject()(val controllerComponents: ControllerComponents,
                                        val config: Configuration,
                                        val featureService: FeatureService,
                                        val clientService: ClientService) extends DeleteController {
  override implicit val ec: ExC = controllerComponents.executionContext
  override val logger: Logger = LoggerFactory.getLogger(this.getClass)
  override val currentSignature: String = config.get[String]("jwt.signature")
  override val validScopes: Set[String] = config.get[Seq[String]]("controllers.delete-controller.scopes").toSet
}

trait DeleteController extends MainController {

  implicit val ec: ExC

  val clientService: ClientService

  val validScopes: Set[String]

  def deleteClient(): Action[AnyContent] = userAuthorisation[ScopedOwner] { implicit req => so =>
    scopeValidation(validScopes, so.scopes) {
      getSelector(req) match {
        case Some(sel) => clientService.deleteClient(so.sub, sel) map {
          case MongoSuccessDelete => NoContent
          case MongoFailedDelete => InternalServerError(ErrorResponses.generalError)
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
