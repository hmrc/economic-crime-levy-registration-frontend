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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.data

import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._

import java.time.Instant
import java.util.Date
import scala.concurrent.Future

trait GrsStubData[T] {

  def buildJourneyData(
    identifiersMatch: Boolean,
    registrationStatus: RegistrationStatus,
    verificationStatus: Option[VerificationStatus] = None,
    entityType: EntityType,
    businessPartnerId: String
  ): Future[T]

  def handleJourneyId(journeyId: String): Future[T] = {
    val (jId, entityType, businessPartnerId): (String, EntityType, String) = parseJourneyId(journeyId)

    jId match {
      case "1" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = RegistrationNotCalled,
          verificationStatus = Some(Fail),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "2" =>
        buildJourneyData(
          identifiersMatch = false,
          registrationStatus = RegistrationNotCalled,
          verificationStatus = Some(Unchallenged),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "3" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = RegistrationFailed,
          verificationStatus = Some(Pass),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "4" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = Registered,
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "5" =>
        buildJourneyData(
          identifiersMatch = false,
          registrationStatus = RegistrationNotCalled,
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "6" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = RegistrationFailed,
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case _   =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = Registered,
          verificationStatus = Some(Pass),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
    }
  }

  val companyProfile: CompanyProfile = CompanyProfile(
    companyName = "Test Entity Ltd.",
    companyNumber = "01234567",
    dateOfIncorporation = Date.from(Instant.parse("2007-12-03T10:15:30.00Z")),
    unsanitisedCHROAddress = IncorporatedEntityAddress(
      address_line_1 = Some("Test Address Line 1"),
      address_line_2 = Some("Test Address Line 2"),
      country = Some("United Kingdom"),
      locality = Some("Test Town"),
      po_box = Some("123"),
      postal_code = Some("AB1 2CD"),
      premises = None,
      region = Some("Test Region")
    )
  )

  private def registered(businessPartnerId: String) = GrsRegistrationResult(
    registrationStatus = Registered,
    registeredBusinessPartnerId = Some(businessPartnerId),
    failures = None
  )

  private val registrationNotCalled = GrsRegistrationResult(
    registrationStatus = RegistrationNotCalled,
    registeredBusinessPartnerId = None,
    failures = None
  )

  private val registrationFailed = GrsRegistrationResult(
    registrationStatus = RegistrationFailed,
    registeredBusinessPartnerId = None,
    failures = Some(
      Seq(
        GrsRegistrationResultFailures(
          code = "PARTY_TYPE_MISMATCH",
          reason = "The remote endpoint has indicated there is Party Type mismatch"
        )
      )
    )
  )

  def registrationResult(registrationStatus: RegistrationStatus, businessPartnerId: String): GrsRegistrationResult =
    registrationStatus match {
      case Registered            => registered(businessPartnerId)
      case RegistrationNotCalled => registrationNotCalled
      case RegistrationFailed    => registrationFailed
    }

  def parseJourneyId(journeyId: String): (String, EntityType, String) =
    journeyId.split("-").toList match {
      case List(a, b, c) => (a, EntityType.enumerable.value(b).get, c)
      case _             => throw new IllegalStateException("Invalid journeyId")
    }
}
