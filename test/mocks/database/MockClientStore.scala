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

package mocks.database

import com.cjwwdev.mongo.responses.{MongoFailedUpdate, MongoSuccessUpdate, MongoUpdatedResponse}
import database.ClientStore
import models.Client
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.{reset, when}
import org.mockito.stubbing.OngoingStubbing
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.conversions.Bson
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec

import scala.concurrent.Future

trait MockClientStore extends MockitoSugar with BeforeAndAfterEach {
  self: PlaySpec =>

  val mockClientStore: ClientStore = mock[ClientStore]

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockClientStore)
  }

  def mockGetClientOn(client: Option[Client]): OngoingStubbing[Future[Option[Client]]] = {
    when(mockClientStore.getClientOn(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(client))
  }

  def mockGetClientsOn(clients: Seq[Client]): OngoingStubbing[Future[Seq[Client]]] = {
    when(mockClientStore.getClientsOn(ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(clients))
  }

  def mockGetClientAndProject(projection: Map[String, BsonValue]): OngoingStubbing[Future[Map[String, BsonValue]]] = {
    when(mockClientStore.getClientAndProject(ArgumentMatchers.any[Bson](), ArgumentMatchers.any())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(projection))
  }

  def mockUpdateClient(success: Boolean): OngoingStubbing[Future[MongoUpdatedResponse]] = {
    when(mockClientStore.updateClient(ArgumentMatchers.any[Bson](), ArgumentMatchers.any[Bson]())(ArgumentMatchers.any()))
      .thenReturn(Future.successful(if(success) MongoSuccessUpdate else MongoFailedUpdate))
  }
}
