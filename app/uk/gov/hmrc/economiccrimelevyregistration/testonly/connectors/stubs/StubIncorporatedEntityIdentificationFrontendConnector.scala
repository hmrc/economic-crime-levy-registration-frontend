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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.IncorporatedEntityIdentificationFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{BusinessVerificationResult, GrsCreateJourneyResponse, IncorporatedEntityJourneyData, RegistrationStatus, VerificationStatus}
import uk.gov.hmrc.economiccrimelevyregistration.testonly.data.GrsStubData
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.Future

class StubIncorporatedEntityIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi
) extends IncorporatedEntityIdentificationFrontendConnector
    with GrsStubData[IncorporatedEntityJourneyData] {

  override def createLimitedCompanyJourney()(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl = "/register-for-economic-crime-levy/test-only/stub-grs-journey-data"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData] =
    handleJourneyId(journeyId)

  override def buildJourneyData(
    identifiersMatch: Boolean,
    registrationStatus: RegistrationStatus,
    verificationStatus: Option[VerificationStatus] = None,
    entityType: EntityType,
    businessPartnerId: String
  ): Future[IncorporatedEntityJourneyData] =
    Future.successful(
      IncorporatedEntityJourneyData(
        companyProfile = companyProfile,
        ctutr = "1234567890",
        identifiersMatch = identifiersMatch,
        businessVerification = verificationStatus.map(BusinessVerificationResult(_)),
        registration = registrationResult(registrationStatus, businessPartnerId)
      )
    )
}
