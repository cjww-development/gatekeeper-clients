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

package helpers

import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import java.time.Instant

object MockToken {
  val testSignature: String = "local-signing-key"

  def build(clientId: String, userId: String, scope: String): String = {
    val now = Instant.now

    val claims = JwtClaim()
      .to(clientId)
      .by("test-issuer")
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.plusMillis(90000000L).getEpochSecond)
      .about(userId)
      .++[String]("scp" -> scope)
      .toJson

    Jwt.encode(claims, testSignature, JwtAlgorithm.HS512)
  }

  def buildInvalid(clientId: String, userId: String, scope: String): String = {
    val now = Instant.now

    val claims = JwtClaim()
      .to(clientId)
      .by("test-issuer")
      .issuedAt(now.getEpochSecond)
      .startsAt(now.getEpochSecond)
      .expiresAt(now.minusMillis(90000000L).getEpochSecond)
      .about(userId)
      .++[String]("scp" -> scope)
      .toJson

    Jwt.encode(claims, testSignature, JwtAlgorithm.HS512)
  }
}
