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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.PartnershipIdentificationFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{BusinessVerificationResult, GrsCreateJourneyResponse, PartnershipEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, GeneralPartnership, ScottishPartnership}
import uk.gov.hmrc.economiccrimelevyregistration.testonly.data.GrsStubData
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import javax.inject.Inject
import scala.concurrent.Future

class StubPartnershipIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi
) extends PartnershipIdentificationFrontendConnector
    with GrsStubData[PartnershipEntityJourneyData] {

  override def createPartnershipJourney(partnershipType: EntityType)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl = "/register-for-economic-crime-levy/test-only/stub-grs-journey-data"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[PartnershipEntityJourneyData] =
    handleJourneyId(journeyId)

  override def buildJourneyData(
    identifiersMatch: Boolean,
    registrationStatus: String,
    verificationStatus: Option[String] = None,
    entityType: EntityType,
    businessPartnerId: String
  ): Future[PartnershipEntityJourneyData] =
    Future.successful(
      PartnershipEntityJourneyData(
        companyProfile = entityType match {
          case GeneralPartnership | ScottishPartnership => None
          case _                                        =>
            Some(companyProfile)
        },
        sautr = Some("1234567890"),
        postcode = Some("AA11AA"),
        identifiersMatch = identifiersMatch,
        businessVerification = verificationStatus.map(BusinessVerificationResult(_)),
        registration = registrationResult(registrationStatus, businessPartnerId)
      )
    )
}
