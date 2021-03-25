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

import controllers.actions.{AuthorisedAction, BodyParsers, FeatureManagement}
import models.Client
import play.api.mvc.{AnyContent, BaseController, Request}

trait MainController extends AuthorisedAction with BodyParsers with BaseController with FeatureManagement {

  val getSelector: Request[AnyContent] => Option[(String, String)] = req => {
    val id = req.getQueryString("id")
    val clientId = req.getQueryString("client_id")

    (id.isDefined, clientId.isDefined) match {
      case (true, false) => id.map(id => ("id", id))
      case (false, true) => clientId.map(cid => ("clientId", Client.stringObs.encrypt(cid)))
      case (true, true) => None
      case (false, false) => None
    }
  }
}
