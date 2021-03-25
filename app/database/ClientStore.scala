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

package database

import com.cjwwdev.mongo.DatabaseRepository
import com.cjwwdev.mongo.connection.ConnectionSettings
import com.cjwwdev.mongo.indexing.RepositoryIndexer
import com.cjwwdev.mongo.responses._
import com.typesafe.config.Config
import models.Client
import org.bson.codecs.configuration.CodecRegistry
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.collection.immutable.Document
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Projections.{excludeId, fields, include}
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import org.slf4j.{Logger, LoggerFactory}
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{Future, ExecutionContext => ExC}
import scala.reflect.ClassTag

class DefaultClientStore @Inject()(val configuration: Configuration) extends ClientStore with ConnectionSettings {
  override val config: Config = configuration.underlying
}

trait ClientStore extends DatabaseRepository with CodecReg with RepositoryIndexer {

  private val logger: Logger = LoggerFactory.getLogger(this.getClass)

  private def clientBasedCollection(implicit ct: ClassTag[Client], codec: CodecRegistry) = collection[Client](ct, codec)
  private def documentBasedCollection(implicit ct: ClassTag[Document], codec: CodecRegistry) = collection[Document](ct, codec)

  override def indexes: Seq[IndexModel] = Seq(
    IndexModel(Indexes.ascending("id"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("owner"), IndexOptions().background(false).unique(false)),
    IndexModel(Indexes.ascending("clientType"), IndexOptions().background(false).unique(false)),
    IndexModel(Indexes.ascending("clientId"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("clientSecret"), IndexOptions().background(false).unique(true)),
    IndexModel(Indexes.ascending("createdAt"), IndexOptions().background(false).unique(false))
  )

  def createClient(client: Client)(implicit ec: ExC): Future[MongoCreateResponse] = {
    clientBasedCollection
      .insertOne(client)
      .toFuture()
      .map { _ =>
        logger.info(s"[createClient] - Created new client under name ${client.id}")
        MongoSuccessCreate
      }.recover {
        case e =>
          logger.error(s"[createClient] - There was a problem creating a new client under name ${client.id}", e)
          MongoFailedCreate
      }
  }

  def getClientOn(query: Bson)(implicit ec: ExC): Future[Option[Client]] = {
    clientBasedCollection
      .find(query)
      .first()
      .toFutureOption()
      .map { client =>
        if(client.isDefined) {
          logger.info(s"[getClientOn] - Found client")
        } else {
          logger.warn(s"[getClientOn] - Client not found")
        }
        client
      }
  }

  def getClientAndProject(query: Bson, projections: String*)(implicit ec: ExC): Future[Map[String, BsonValue]] = {
    def buildMap(doc: Option[Document]): Map[String, BsonValue] = {
      doc.fold[Map[String, BsonValue]](Map())(_.toMap)
    }

    val inclusions = fields((projections
      .map(str => include(str)) ++ Seq(include("id"), excludeId())):_*)

    documentBasedCollection
      .find(query)
      .projection(inclusions)
      .first()
      .toFutureOption()
      .map(buildMap)
      .recover(_ => Map())
  }

  def getClientsOn(query: Bson)(implicit ec: ExC): Future[Seq[Client]] = {
    clientBasedCollection
      .find(query)
      .toFuture()
      .map { clients =>
        if(clients.nonEmpty) {
          logger.info(s"[getClientsOn] - Found ${clients.size} clients")
        } else {
          logger.warn(s"[getClientsOn] - No clients found")
        }
        clients
      }
  }

  def updateClient(query: Bson, update: Bson)(implicit ec: ExC): Future[MongoUpdatedResponse] = {
    clientBasedCollection
      .updateOne(query, update)
      .toFuture()
      .map { _ =>
        logger.info(s"[updateClient] - Updated client")
        MongoSuccessUpdate
      }.recover {
        case e =>
          logger.warn(s"[updateClient] - There was a problem updating the client", e)
          MongoFailedUpdate
      }
  }

  def deleteClient(query: Bson)(implicit ec: ExC): Future[MongoDeleteResponse] = {
    clientBasedCollection
      .deleteOne(query)
      .toFuture()
      .map { _ =>
        logger.info(s"[deleteClient] - The client was successfully deleted")
        MongoSuccessDelete
      } recover {
        case e =>
          logger.warn(s"[deleteClient] - There was a problem deleting the client", e)
          MongoFailedDelete
      }
  }
}
