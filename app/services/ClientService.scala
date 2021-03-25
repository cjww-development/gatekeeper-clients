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

package services

import com.cjwwdev.mongo.responses.{MongoCreateResponse, MongoDeleteResponse, MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import database.ClientStore
import models.{Client, ClientUpdate}
import org.mongodb.scala.model.Filters.{and, equal, mod}
import org.mongodb.scala.model.Updates.set
import org.slf4j.{Logger, LoggerFactory}

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}

sealed trait RegenerationResponse
case object RegeneratedId extends RegenerationResponse
case object RegeneratedIdAndSecret extends RegenerationResponse
case object InvalidClientType extends RegenerationResponse
case object InvalidClient extends RegenerationResponse

class DefaultClientService @Inject()(val clientStore: ClientStore) extends ClientService

trait ClientService {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  val clientStore: ClientStore

  def getClientBy(owner: String, selector: (String, String))(implicit ec: ExC): Future[Option[Client]] = {
    val (field, value) = selector
    val query = and(
      equal("owner", owner),
      equal(field, value)
    )

    clientStore.getClientOn(query) map { client =>
      if(client.isDefined) {
        logger.info(s"[getClientBy] - Found client belonging to user ${owner} with $field $value")
      } else {
        logger.warn(s"[getClientBy] - Could not find client belonging to user ${owner} with $field $value")
      }
      client.map(Client.decode)
    }
  }

  def getOwnedClients(owner: String, groupsOf: Int)(implicit ec: ExC): Future[Seq[Seq[Client]]] = {
    val query = equal("owner", owner)

    clientStore.getClientsOn(query) map {
      _.map(Client.decode)
        .grouped(groupsOf)
        .toSeq
    }
  }

  def getClient(currentUser: String, selector: (String, String))(implicit ec: ExC): Future[Option[Client]] = {
    val (field, value) = selector
    val query = equal(field, value)

    clientStore.getClientOn(query) map {
      _.filter(client => if(client.isPrivate) client.authorisedUsers.contains(currentUser) || client.owner == currentUser else true)
        .map(Client.decode)
    }
  }

  def getClientsDetails(currentUser: String, clients: Seq[(String, String)])(implicit ec: ExC): Future[Seq[Client]] = {
    Future.sequence(clients.map(client => getClient(currentUser, client))).map(_.flatten)
  }

  def createClient(client: Client)(implicit ec: ExC): Future[MongoCreateResponse] = {
    clientStore.createClient(client)
  }

  def updateClient(owner: String, appSelector: (String, String), update: ClientUpdate)(implicit ec: ExC): Future[Either[MongoUpdatedResponse, Option[Client]]] = {
    val selector = and(equal("owner", owner), equal(appSelector._1, appSelector._2))
    val modifier = ClientUpdate.modifier(update)

    clientStore.updateClient(selector, modifier) flatMap {
      case MongoSuccessUpdate => clientStore.getClientOn(selector) map(Right(_))
      case resp@MongoFailedUpdate => Future.successful(Left(resp))
    }
  }

  def regenerateCredentials(owner: String, selector: (String, String), isConfidential: Boolean)(implicit ec: ExC): Future[Either[MongoUpdatedResponse, RegenerationResponse]] = {
    val newClientId = Client.generateClientId()
    val newClientSecret = Client.generateClientSecret()

    val query = and(
      equal("owner", owner),
      equal(selector._1, selector._2)
    )

    val modifier = if(isConfidential) {
      and(set("clientId", newClientId), set("clientSecret", newClientSecret))
    } else {
      and(set("clientId", newClientId))
    }

    val clientTypeChecker: Boolean => PartialFunction[String, Boolean] = isConfidential => {
      case "confidential" => isConfidential
      case "public" => !isConfidential
    }

    clientStore.getClientAndProject(query, "clientType") flatMap { projection =>
      val clientTypeChecked = projection
        .get("clientType")
        .map(_.asString().getValue)
        .map(clientTypeChecker(isConfidential))

      clientTypeChecked match {
        case Some(checked) => if(checked) {
          clientStore.updateClient(query, modifier) map {
            case MongoSuccessUpdate =>
              logger.info(s"[regenerateCredentials] - Regenerated clientId ${if(isConfidential) "and clientSecret"} for ${selector._1} ${selector._2}")
              Right(if(isConfidential) RegeneratedIdAndSecret else RegeneratedId)
            case resp@MongoFailedUpdate =>
              logger.warn(s"[regenerateCredentials] - There was a problem regenerating Ids and or secrets for ${selector._1} ${selector._2}")
              Left(resp)
          }
        } else {
          Future.successful(Right(InvalidClientType))
        }
        case None =>
          logger.warn(s"[regenerateCredentials] - There was no client matching ${selector._1} ${selector._2} to regenerate credentials")
          Future.successful(Right(InvalidClient))
      }
    }
  }

  def deleteClient(owner: String, appSelector: (String, String))(implicit ec: ExC): Future[MongoDeleteResponse] = {
    val query = and(
      equal("owner", owner),
      equal(appSelector._1, appSelector._2),
    )

    clientStore.deleteClient(query)
  }
}
