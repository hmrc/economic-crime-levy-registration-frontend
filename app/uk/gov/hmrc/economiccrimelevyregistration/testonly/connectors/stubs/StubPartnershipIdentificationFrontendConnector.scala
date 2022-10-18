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
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{BusinessVerificationResult, CompanyProfile, GrsCreateJourneyResponse, GrsRegistrationResult, GrsRegistrationResultFailures, IncorporatedEntityAddress, PartnershipEntityJourneyData}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import java.time.Instant
import java.util.Date
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubPartnershipIdentificationFrontendConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val messagesApi: MessagesApi,
  ec: ExecutionContext
) extends PartnershipIdentificationFrontendConnector {
  override def createPartnershipJourney(partnershipType: EntityType)(implicit
    hc: HeaderCarrier
  ): Future[GrsCreateJourneyResponse] =
    Future.successful(
      GrsCreateJourneyResponse(
        journeyStartUrl = "/register-for-economic-crime-levy/test-only/stub-grs-journey-data"
      )
    )

  override def getJourneyData(journeyId: String)(implicit hc: HeaderCarrier): Future[PartnershipEntityJourneyData] = {
    val (jid, entityType): (String, String) = journeyId.split("-").toList match {
      case List(a, b) => (a, b)
      case _          => throw new IllegalStateException("Invalid journeyId")
    }

    jid match {
      case "1" =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          verificationStatus = Some("FAIL")
        )
      case "2" =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = false,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          verificationStatus = Some("UNCHALLENGED")
        )
      case "3" =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_FAILED",
          verificationStatus = Some("PASS")
        )
      case "4" =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = true,
          registrationStatus = "REGISTERED"
        )
      case "5" =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = false,
          registrationStatus = "REGISTRATION_NOT_CALLED"
        )
      case "6" =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_FAILED"
        )
      case _   =>
        buildJourneyData(
          entityType = entityType,
          identifiersMatch = true,
          registrationStatus = "REGISTERED",
          verificationStatus = Some("PASS")
        )

    }
  }

  private def buildJourneyData(
    entityType: String,
    identifiersMatch: Boolean,
    registrationStatus: String,
    verificationStatus: Option[String] = None
  ): Future[PartnershipEntityJourneyData] =
    Future.successful(
      PartnershipEntityJourneyData(
        companyProfile = entityType match {
          case "GeneralPartnership" | "ScottishPartnership" => None
          case _                                            =>
            Some(
              CompanyProfile(
                companyName = "Test Company Ltd",
                companyNumber = "01234567",
                dateOfIncorporation = Date.from(Instant.parse("2007-12-03T10:15:30.00Z")),
                unsanitisedCHROAddress = IncorporatedEntityAddress(
                  address_line_1 = "testLine1",
                  address_line_2 = "test town",
                  care_of = "test name",
                  country = "United Kingdom",
                  locality = "test city",
                  po_box = "123",
                  postal_code = "AA11AA",
                  premises = "1",
                  region = "test region"
                )
              )
            )
        },
        sautr = "1234567890",
        postcode = "AA11AA",
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
