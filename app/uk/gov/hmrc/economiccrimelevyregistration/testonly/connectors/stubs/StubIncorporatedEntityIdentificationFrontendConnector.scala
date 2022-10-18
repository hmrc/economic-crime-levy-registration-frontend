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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{BusinessVerificationResult, GrsCreateJourneyResponse, IncorporatedEntityJourneyData}
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
    with GrsStubData {
  override def createLimitedCompanyJourney()(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl = "/register-for-economic-crime-levy/test-only/stub-grs-journey-data"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[IncorporatedEntityJourneyData] = {
    val (jid, _): (String, String) = parseJourneyId(journeyId)

    jid match {
      case "1" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          verificationStatus = Some("FAIL")
        )
      case "2" =>
        buildJourneyData(
          identifiersMatch = false,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          verificationStatus = Some("UNCHALLENGED")
        )
      case "3" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_FAILED",
          verificationStatus = Some("PASS")
        )
      case "4" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTERED"
        )
      case "5" =>
        buildJourneyData(
          identifiersMatch = false,
          registrationStatus = "REGISTRATION_NOT_CALLED"
        )
      case "6" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_FAILED"
        )
      case _   =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTERED",
          verificationStatus = Some("PASS")
        )
    }
  }

  private def buildJourneyData(
    identifiersMatch: Boolean,
    registrationStatus: String,
    verificationStatus: Option[String] = None
  ): Future[IncorporatedEntityJourneyData] =
    Future.successful(
      IncorporatedEntityJourneyData(
        companyProfile = companyProfile,
        ctutr = "1234567890",
        identifiersMatch = identifiersMatch,
        businessVerification = verificationStatus.map(BusinessVerificationResult(_)),
        registration = registrationResult(registrationStatus)
      )
    )
}
