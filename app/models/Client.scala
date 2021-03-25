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

import com.cjwwdev.security.Implicits._
import com.cjwwdev.security.deobfuscation.DeObfuscators
import com.cjwwdev.security.obfuscation.Obfuscators
import org.bson.codecs.configuration.CodecProvider
import org.joda.time.DateTime
import org.mongodb.scala.bson.codecs.Macros
import play.api.libs.json._

import java.util.UUID
import scala.collection.Seq
import scala.{Seq => Seqq}

case class Client(id: String,
                  owner: String,
                  name: String,
                  desc: String,
                  homeUrl: String,
                  redirects: Seq[String],
                  clientType: String,
                  clientId: String,
                  clientSecret: Option[String],
                  validFlows: Seq[String],
                  defaultScopes: Seq[String],
                  customScopes: Seq[String],
                  idTokenExpiry: Long,
                  accessTokenExpiry: Long,
                  refreshTokenExpiry: Long,
                  isPrivate: Boolean,
                  authorisedUsers: Seq[String],
                  createdAt: DateTime)

object Client extends Obfuscators with DeObfuscators with TimeFormat {
  override val locale: String = "client"

  val codec: CodecProvider = Macros.createCodecProviderIgnoreNone[Client]()

  val generateClientId: () => String = () => {
    UUID.randomUUID().toString.replace("-", "").encrypt
  }

  val generateClientSecret: () => String = () => {
    (0 to 1)
      .map(_ => UUID.randomUUID().toString.replace("-", ""))
      .mkString
      .encrypt
  }

  val decode: Client => Client = client => client.copy(
    homeUrl = stringDeObfuscate.decrypt(client.homeUrl).getOrElse(""),
    redirects = client.redirects.map(url => stringDeObfuscate.decrypt(url).getOrElse("")),
    clientId = stringDeObfuscate.decrypt(client.clientId).getOrElse(""),
    clientSecret = client.clientSecret.map(sec => stringDeObfuscate.decrypt(sec).getOrElse(""))
  )

  val apply: (String, String, String, String, Seq[String], String, Boolean) => Client = (owner, name, desc, homeUrl, redirects, clientType, isPrivate) => new Client(
    id = s"client-${UUID.randomUUID().toString}",
    owner,
    name,
    desc,
    homeUrl.encrypt,
    redirects.map(_.encrypt),
    clientType,
    clientId = generateClientId(),
    clientSecret = clientType match {
      case "confidential" => Some(generateClientSecret())
      case "public"       => None
    },
    validFlows = Seq(),
    defaultScopes = Seq(),
    customScopes = Seq(),
    idTokenExpiry = 3600000L,
    accessTokenExpiry = 3600000L,
    refreshTokenExpiry = 2592000000L,
    isPrivate,
    authorisedUsers = Seq(),
    createdAt = DateTime.now()
  )

  implicit val newClientReads: String => Reads[Client] = owner => (json: JsValue) => {
    val problemFields: Map[String, JsResult[_]] = Map(
      "client_type" -> json.\("client_type").validate[String](ClientTypes.reads),
      "home_url" -> json.\("home_url").validate[String](ClientValidation.validHomeUrl),
      "redirects" -> json.\("redirects").validate(ClientValidation.validRedirects)
    )

    val errors = problemFields.filter({ case (_, v) => v.isError })

    if (errors.nonEmpty) {
      type JsErr = Seq[(JsPath, Seq[JsonValidationError])]
      JsError(
        errors
          .collect({ case (_, JsError(e)) => e })
          .foldLeft[JsErr](Seq())((a, b) => JsError.merge(a, b))
      )
    } else {
      val clientType: String = problemFields("client_type").get.asInstanceOf[String]

      JsSuccess(Client(
        owner,
        json.\("name").as[String].trim,
        json.\("desc").as[String].trim,
        json.\("home_url").as[String],
        json.\("redirects").as[Seq[String]],
        clientType,
        json.\("is_private").as[Boolean]
      ))
    }
  }

  val outboundDetailsWrites: Writes[Client] = (client: Client) => {
    Json.obj(
      "owner" -> client.owner,
      "name" -> client.name,
      "desc" -> client.desc
    )
  }

  val outboundDetailsWritesSeq: Writes[Seqq[Client]] = (clients: Seq[Client]) => {
    val jsonClients = clients.map(Json.toJson(_)(outboundDetailsWrites))
    Json.toJson(jsonClients.flatMap(x => Seqq(x)))
  }

  implicit val outboundWrites: Writes[Client] = (client: Client) => {
    val clientInfo = Json.obj(
      "name" -> client.name,
      "desc" -> client.desc,
      "created_at" -> client.createdAt,
      "home_url" -> stringDeObfuscate.decrypt(client.homeUrl).getOrElse[String](client.homeUrl),
      "redirects" -> client.redirects.map(url => stringDeObfuscate.decrypt(url).getOrElse[String](url)),
      "client_type" -> client.clientType,
      "client_id" -> stringDeObfuscate.decrypt(client.clientId).getOrElse[String](client.clientId)
    )

    val clientSecret = client.clientSecret.fold(Json.obj()) { sec =>
      Json.obj("client_secret" -> stringDeObfuscate.decrypt(sec).getOrElse[String](sec))
    }

    val clientConfig = Json.obj(
      "valid_flows" -> client.validFlows,
      "default_scopes" -> client.defaultScopes,
      "custom_scopes" -> client.customScopes,
      "id_token_expiry" -> client.idTokenExpiry,
      "access_token_expiry" -> client.accessTokenExpiry,
      "refresh_token_expiry" -> client.refreshTokenExpiry,
      "is_private" -> client.isPrivate,
      "authorised_users" -> client.authorisedUsers
    )

    clientInfo ++ clientSecret ++ clientConfig
  }
}
