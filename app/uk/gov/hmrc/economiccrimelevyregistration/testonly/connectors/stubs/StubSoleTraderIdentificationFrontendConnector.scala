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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{BusinessVerificationResult, FullName, GrsCreateJourneyResponse, GrsRegistrationResult, GrsRegistrationResultFailures, SoleTraderEntityJourneyData}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.Instant
import java.util.Date
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubSoleTraderIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends SoleTraderIdentificationFrontendConnector {
  override def createSoleTraderJourney()(implicit hc: HeaderCarrier): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl = "/register-for-economic-crime-levy/test-only/stub-grs-journey-data"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[SoleTraderEntityJourneyData] = {
    val (jid, _): (String, String) = journeyId.split("-").toList match {
      case List(a, b) => (a, b)
      case _          => throw new IllegalStateException("Invalid journeyId")
    }

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
  ): Future[SoleTraderEntityJourneyData] =
    Future.successful(
      SoleTraderEntityJourneyData(
        fullName = FullName(
          firstName = "John",
          lastName = "Test"
        ),
        dateOfBirth = Date.from(Instant.parse("1975-01-31T00:00:00.00Z")),
        nino = "BB111111B",
        sautr = "1234567890",
        identifiersMatch = identifiersMatch,
        businessVerification = verificationStatus match {
          case Some(data) => Some(BusinessVerificationResult(verificationStatus = data))
          case None       => None
        },
        registration = registrationStatus match {
          case "REGISTERED" =>
            GrsRegistrationResult(
              registrationStatus = registrationStatus,
              registeredBusinessPartnerId = Some("X00000123456789"),
              failures = None
            )

          case "REGISTRATION_NOT_CALLED" =>
            GrsRegistrationResult(
              registrationStatus = registrationStatus,
              registeredBusinessPartnerId = None,
              failures = None
            )

          case "REGISTRATION_FAILED" =>
            GrsRegistrationResult(
              registrationStatus = registrationStatus,
              registeredBusinessPartnerId = None,
              failures = Some(
                Seq(
                  GrsRegistrationResultFailures(
                    code = "E001",
                    reason = "An error of type 'test' occurred"
                  )
                )
              )
            )
        }
      )
    )
}
