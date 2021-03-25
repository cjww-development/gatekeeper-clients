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

package models

import org.bson.codecs.configuration.CodecProvider
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.and
import org.mongodb.scala.model.Updates.set
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class ClientUpdate(name: Option[String] = None,
                        desc: Option[String] = None,
                        homeUrl: Option[String] = None,
                        redirects: Option[Seq[String]] = None,
                        validFlows: Option[Seq[String]] = None,
                        defaultScopes: Option[Seq[String]] = None,
                        customScopes: Option[Seq[String]] = None,
                        idTokenExpiry: Option[Long] = None,
                        accessTokenExpiry: Option[Long] = None,
                        refreshTokenExpiry: Option[Long] = None,
                        isPrivate: Option[Boolean] = None,
                        authorisedUsers: Option[Seq[String]] = None) {
  def nonEmpty: Boolean = Seq(name, desc, redirects, validFlows, defaultScopes, customScopes, idTokenExpiry, accessTokenExpiry, refreshTokenExpiry).flatten.nonEmpty
}

object ClientUpdate {
  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[ClientUpdate]()

  implicit val inboundReads: Reads[ClientUpdate] = (
    (__ \ "name").readNullable[String] and
    (__ \ "desc").readNullable[String] and
    (__ \ "home_url").readNullable[String](ClientValidation.validHomeUrl) and
    (__ \ "redirects").readNullable[Seq[String]](ClientValidation.validRedirects) and
    (__ \ "valid_flows").readNullable[Seq[String]](ClientValidation.validFlows) and
    (__ \ "default_scopes").readNullable[Seq[String]](ClientValidation.validDefaultScopes) and
    (__ \ "custom_scopes").readNullable[Seq[String]] and
    (__ \ "id_token_expiry").readNullable[Long](ClientValidation.tokenExpiry) and
    (__ \ "access_token_expiry").readNullable[Long](ClientValidation.tokenExpiry) and
    (__ \ "refresh_token_expiry").readNullable[Long](ClientValidation.refreshTokenExpiry) and
    (__ \ "is_private").readNullable[Boolean] and
    (__ \ "authorised_users").readNullable[Seq[String]]
  )(ClientUpdate.apply _)

  val modifier: ClientUpdate => Bson = update => {
    val name = update.name.map(name => set("name", name))
    val desc = update.desc.map(desc => set("desc", desc))
    val homeUrl = update.homeUrl.map(url => set("homeUrl", Client.stringObs.encrypt(url)))
    val redirects = update.redirects.map(rds => set("redirects", rds.map(Client.stringObs.encrypt)))
    val validFlows = update.validFlows.map(vf => set("validFlows", vf))
    val defaultScopes = update.defaultScopes.map(ds => set("defaultScopes", ds))
    val customScopes = update.customScopes.map(ds => set("customScopes", ds))
    val idTokenExpiry = update.idTokenExpiry.map(ite => set("idTokenExpiry", ite))
    val accessTokenExpiry = update.accessTokenExpiry.map(ate => set("accessTokenExpiry", ate))
    val refreshTokenExpiry = update.refreshTokenExpiry.map(rte => set("refreshTokenExpiry", rte))
    val isPrivate = update.isPrivate.map(isPrivate => set("isPrivate", isPrivate))
    val authorisedUsers = update.authorisedUsers.map(users => set("authorisedUsers", users))

    val combo = Seq(
      name, desc, homeUrl,
      redirects, validFlows, defaultScopes,
      customScopes, idTokenExpiry, accessTokenExpiry,
      refreshTokenExpiry, isPrivate, authorisedUsers).flatten

    and(combo:_*)
  }
}
