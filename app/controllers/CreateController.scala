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
import com.cjwwdev.mongo.responses.{MongoFailedCreate, MongoSuccessCreate}
import models.UserResults._
import models.{Client, ErrorResponses, ScopedOwner}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.ClientService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext => ExC}

class DefaultCreateController @Inject()(val controllerComponents: ControllerComponents,
                                        val config: Configuration,
                                        val featureService: FeatureService,
                                        val clientService: ClientService) extends CreateController {
  override implicit val ec: ExC = controllerComponents.executionContext
  override val logger: Logger = LoggerFactory.getLogger(this.getClass)
  override val currentSignature: String = config.get[String]("jwt.signature")
  override val validScopes: Set[String] = config.get[Seq[String]]("controllers.create-controller.scopes").toSet
}

trait CreateController extends MainController {

  implicit val ec: ExC

  val clientService: ClientService

  val validScopes: Set[String]

  def createClient(): Action[AnyContent] = userAuthorisation[ScopedOwner] { implicit req => so =>
    scopeValidation(validScopes, so.scopes) {
      implicit val clientReads: Reads[Client] = Client.newClientReads(so.sub)
      withJsonBody[Client] { client =>
        clientService.createClient(client) map {
          case MongoSuccessCreate => Ok(Json.toJson(client))
          case MongoFailedCreate  => InternalServerError(ErrorResponses.generalError)
        }
      }
    }
  }
}
