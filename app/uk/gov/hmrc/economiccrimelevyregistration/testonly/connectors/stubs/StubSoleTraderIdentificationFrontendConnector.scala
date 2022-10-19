/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.SoleTraderIdentificationFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{BusinessVerificationResult, FullName, GrsCreateJourneyResponse, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.testonly.data.GrsStubData
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.Instant
import java.util.Date
import javax.inject.Inject
import scala.concurrent.Future

class StubSoleTraderIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi
) extends SoleTraderIdentificationFrontendConnector
    with GrsStubData[SoleTraderEntityJourneyData] {

  override def createSoleTraderJourney()(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl = "/register-for-economic-crime-levy/test-only/stub-grs-journey-data"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData] =
    handleJourneyId(journeyId)

  override def buildJourneyData(
    identifiersMatch: Boolean,
    registrationStatus: String,
    verificationStatus: Option[String] = None,
    entityType: EntityType
  ): Future[SoleTraderEntityJourneyData] =
    Future.successful(
      SoleTraderEntityJourneyData(
        fullName = FullName(
          firstName = "John",
          lastName = "Doe"
        ),
        dateOfBirth = Date.from(Instant.parse("1975-01-31T00:00:00.00Z")),
        nino = "BB111111B",
        sautr = "1234567890",
        identifiersMatch = identifiersMatch,
        businessVerification = verificationStatus.map(BusinessVerificationResult(_)),
        registration = registrationResult(registrationStatus)
      )
    )
}
