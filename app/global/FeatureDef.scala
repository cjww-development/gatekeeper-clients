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

import com.cjwwdev.featuremanagement.models.Features

import javax.inject.Inject

class FeatureDef @Inject extends Features {
  override val allFeatures: List[String] = List(
    Features.createClientApi,
    Features.fetchClientApi,
    Features.fetchOwnedClientsApi,
    Features.fetchClientDetailsApi,
    Features.fetchClientsDetailsApi,
    Features.updateClientApi,
    Features.regenerateClientApi,
    Features.deleteClientApi
  )
}

object Features {
  //Create API's
  val createClientApi = "create-client"

  //Read API's
  val fetchClientApi = "fetch-client"
  val fetchOwnedClientsApi = "fetch-owned-clients"
  val fetchClientDetailsApi = "fetch-client-details"
  val fetchClientsDetailsApi = "fetch-clients-details"

  //Update API's
  val updateClientApi = "update-client"
  val regenerateClientApi = "regenerate-client"

  //Delete API's
  val deleteClientApi = "delete-client"
}
