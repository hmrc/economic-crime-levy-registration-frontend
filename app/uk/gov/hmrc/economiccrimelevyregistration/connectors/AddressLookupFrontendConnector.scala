/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import akka.actor.ActorSystem
import com.typesafe.config.Config
import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait AddressLookupFrontendConnector {
  def initJourney(ukMode: Boolean, mode: Mode)(implicit hc: HeaderCarrier): Future[String]
  def getAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData]
}

@Singleton
class AddressLookupFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends AddressLookupFrontendConnector
    with BaseConnector
    with Retries {

  private val baseUrl = appConfig.addressLookupFrontendBaseUrl

  def initJourney(ukMode: Boolean, mode: Mode)(implicit
    hc: HeaderCarrier
  ): Future[String] = {
    val alfLabels = AlfEnCyLabels(appConfig)
    val body      = AlfJourneyConfig(
      options = AlfOptions(
        continueUrl = s"${appConfig.alfContinueUrl}/${mode.toString.toLowerCase}",
        homeNavHref = routes.StartController.onPageLoad().url,
        signOutHref = appConfig.eclSignOutUrl,
        accessibilityFooterUrl = appConfig.accessibilityStatementPath,
        deskProServiceName = appConfig.appName,
        ukMode = ukMode
      ),
      labels = alfLabels
    )
    retryFor[String]("Address look up - Initiate journey")(retryCondition) {
      httpClient
        .post(url"$baseUrl/api/init")
        .withBody(Json.toJson(body))
        .executeAndExtractHeader[String]
    }
  }

  def getAddress(addressId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData] =
    retryFor[AlfAddressData]("Address look up - Get address")(retryCondition) {
      httpClient
        .get(url"$baseUrl/api/confirmed?id=$addressId")
        .executeAndDeserialise[AlfAddressData]
    }

}
