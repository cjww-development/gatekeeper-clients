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

import com.cjwwdev.featuremanagement.controllers.FeatureController
import com.cjwwdev.featuremanagement.models.Features
import controllers.`private`.DefaultFeatureController
import controllers.{CreateController, DefaultCreateController, DefaultDeleteController, DefaultReadController, DefaultUpdateController, DeleteController, ReadController, UpdateController}
import database.{ClientStore, DefaultClientStore}
import filters.{DefaultOutageFilter, DefaultRequestLoggingFilter, OutageFilter, RequestLoggingFilter}
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import services.{ClientService, DefaultClientService}

class ServiceBindings extends Module {
  override def bindings(environment: Environment, configuration: Configuration): collection.Seq[Binding[_]] = {
    globals() ++
    filters() ++
    dataStores() ++
    serviceLayer() ++
    orchestrators() ++
    controllers() ++
    apiControllers() ++
    testControllers() ++
    systemControllers()
  }

  private def globals(): Seq[Binding[_]] = Seq(
    bind[Features].to[FeatureDef].eagerly(),
    bind[GatekeeperIndexer].toSelf.eagerly()
  )

  private def filters(): Seq[Binding[_]] = Seq(
    bind[OutageFilter].to[DefaultOutageFilter].eagerly(),
    bind[RequestLoggingFilter].to[DefaultRequestLoggingFilter].eagerly()
  )

  private def dataStores(): Seq[Binding[_]] = Seq(
    bind[ClientStore].to[DefaultClientStore].eagerly()
  )

  private def serviceLayer(): Seq[Binding[_]] = Seq(
    bind[ClientService].to[DefaultClientService].eagerly()
  )

  private def orchestrators(): Seq[Binding[_]] = Seq(

  )

  private def controllers(): Seq[Binding[_]] = Seq(

  )

  private def apiControllers(): Seq[Binding[_]] = Seq(
    bind[CreateController].to[DefaultCreateController].eagerly(),
    bind[ReadController].to[DefaultReadController].eagerly(),
    bind[UpdateController].to[DefaultUpdateController].eagerly(),
    bind[DeleteController].to[DefaultDeleteController].eagerly()
  )

  private def systemControllers(): Seq[Binding[_]] = Seq(
    bind[FeatureController].to[DefaultFeatureController].eagerly()
  )

  private def testControllers(): Seq[Binding[_]] = Seq(

  )
}
