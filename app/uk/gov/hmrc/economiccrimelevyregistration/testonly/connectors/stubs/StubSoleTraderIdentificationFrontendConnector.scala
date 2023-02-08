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

import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.connectors.SoleTraderIdentificationFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.SoleTrader
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._
import uk.gov.hmrc.economiccrimelevyregistration.testonly.utils.Base64Utils
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.Future

class StubSoleTraderIdentificationFrontendConnector @Inject() () extends SoleTraderIdentificationFrontendConnector {

  override def createSoleTraderJourney(mode: Mode)(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl =
          s"/register-for-the-economic-crime-levy/test-only/stub-grs-journey-data?continueUrl=${mode.toString.toLowerCase}&entityType=${SoleTrader.toString}"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData] =
    Future.successful(Json.parse(Base64Utils.base64UrlDecode(journeyId)).as[SoleTraderEntityJourneyData])

}
