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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors.stubs

import play.api.i18n.MessagesApi
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.economiccrimelevyregistration.testonly.utils.Base64Utils
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.Future

class StubAddressLookupFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi
) extends AddressLookupFrontendConnector {
  override def initJourney(isUkAddress: Boolean, mode: Mode)(implicit hc: HeaderCarrier): Future[String] =
    Future.successful(
      s"${appConfig.host}/register-for-economic-crime-levy/test-only/stub-alf-journey-data?continueUrl=${mode.toString.toLowerCase}"
    )

  override def getAddress(journeyId: String)(implicit hc: HeaderCarrier): Future[AlfAddressData] =
    Future.successful(Json.parse(Base64Utils.base64UrlDecode(journeyId)).as[AlfAddressData])
}
