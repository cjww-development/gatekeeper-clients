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

package filters

import akka.stream.Materializer
import helpers.{Assertions, IntegrationApp}
import org.scalatestplus.play.PlaySpec
import play.api.http.HttpVerbs
import play.api.libs.typedmap.TypedMap
import play.api.mvc.{Headers, RequestHeader, Result}
import play.api.mvc.Results._
import play.api.mvc.request.{RemoteConnection, RequestTarget}
import play.api.test.Helpers._

import java.net.URI
import scala.concurrent.Future

class DefaultOutageFilterSpec extends PlaySpec with IntegrationApp with Assertions {

  val materializer: Materializer = app.injector.instanceOf[Materializer]

  val testOutageFilter: OutageFilter = new OutageFilter {
    override val appName: String = "gatekeeper-clients-test"
    override implicit def mat: Materializer = materializer
  }

  val okFunction: RequestHeader => Future[Result] = rh => Future.successful(Ok("request allowed through"))

  def requestHeader(verb: String, route: String): RequestHeader = new RequestHeader {
    override def connection: RemoteConnection = ???
    override def method: String = verb
    override def target: RequestTarget = new RequestTarget {
      override def uri: URI = ???
      override def uriString: String = route
      override def path: String = ""
      override def queryMap: Map[String, Seq[String]] = ???
    }
    override def version: String = ???
    override def headers: Headers = ???
    override def attrs: TypedMap = ???
  }

  "OutageFilter" should {
    "return a ServiceAvailable" when {
      "the service is shuttered" in {
        System.setProperty("shuttered", "true")
        val result = testOutageFilter.apply(okFunction)(requestHeader(HttpVerbs.GET, "/assets/"))
        status(result) mustBe SERVICE_UNAVAILABLE
        val jsonBody = contentAsJson(result)
        jsonBody.\("status").as[Int] mustBe 503
        jsonBody.\("msg").as[String] mustBe "This service is currently shuttered. Come back later"
      }
    }

    "return an Ok" when {
      "the service isn't shuttered" in {
        System.setProperty("shuttered", "false")
        val result = testOutageFilter.apply(okFunction)(requestHeader(HttpVerbs.GET, "/assets/"))
        status(result) mustBe OK
      }
    }
  }
}
