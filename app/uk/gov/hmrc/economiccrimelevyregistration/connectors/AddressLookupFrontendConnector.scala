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

import play.api.i18n.MessagesApi
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait AddressLookupFrontendConnector {
  def initJourney(ukMode: Boolean)(implicit hc: HeaderCarrier): Future[String]
  def getAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData]
}

@Singleton
class AddressLookupFrontendConnectorImpl @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends AddressLookupFrontendConnector {
  private val baseUrl = appConfig.addressLookupFrontendUrl

  def initJourney(ukMode: Boolean)(implicit
    hc: HeaderCarrier
  ): Future[String] = {
    val alfLabels = AlfEnCyLabels()

    httpClient.POST[AlfJourneyConfig, String](
      s"$baseUrl/api/init",
      AlfJourneyConfig(
        options = AlfOptions(
          continueUrl = appConfig.alfContinueUrl,
          homeNavHref = appConfig.alfHomeUrl,
          signOutHref = appConfig.alfSignOutUrl,
          accessibilityFooterUrl = appConfig.alfAccessibilityFooterUrl,
          deskProServiceName = appConfig.alfDeskProServiceName,
          ukMode = ukMode
        ),
        labels = alfLabels
      )
    )
  }

  def getAddress(addressId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData] = ???
}
