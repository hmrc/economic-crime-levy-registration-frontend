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
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{CompanyProfile, GrsRegistrationResult, GrsRegistrationResultFailures, IncorporatedEntityAddress}

import java.time.Instant
import java.util.Date
import scala.concurrent.Future

trait GrsStubData[T] {

  def buildJourneyData(
    identifiersMatch: Boolean,
    registrationStatus: String,
    verificationStatus: Option[String] = None,
    entityType: EntityType,
    businessPartnerId: String
  ): Future[T]

  def handleJourneyId(journeyId: String): Future[T] = {
    val (jId, entityType, businessPartnerId): (String, EntityType, String) = parseJourneyId(journeyId)

    jId match {
      case "1" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          verificationStatus = Some("FAIL"),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "2" =>
        buildJourneyData(
          identifiersMatch = false,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          verificationStatus = Some("UNCHALLENGED"),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "3" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_FAILED",
          verificationStatus = Some("PASS"),
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "4" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTERED",
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "5" =>
        buildJourneyData(
          identifiersMatch = false,
          registrationStatus = "REGISTRATION_NOT_CALLED",
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case "6" =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTRATION_FAILED",
          entityType = entityType,
          businessPartnerId = businessPartnerId
        )
      case _   =>
        buildJourneyData(
          identifiersMatch = true,
          registrationStatus = "REGISTERED",
          verificationStatus = Some("PASS"),
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
      address_line_1 = "Test Address Line 1",
      address_line_2 = "Test Address Line 2",
      care_of = "Test Name",
      country = "United Kingdom",
      locality = "Test City",
      po_box = "123",
      postal_code = "AA11AA",
      premises = "1",
      region = "Test Region"
    )
  )

  private def registered(businessPartnerId: String) = GrsRegistrationResult(
    registrationStatus = "REGISTERED",
    registeredBusinessPartnerId = Some(businessPartnerId),
    failures = None
  )

  private val registrationNotCalled = GrsRegistrationResult(
    registrationStatus = "REGISTRATION_NOT_CALLED",
    registeredBusinessPartnerId = None,
    failures = None
  )

  private val registrationFailed = GrsRegistrationResult(
    registrationStatus = "REGISTRATION_FAILED",
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

  def registrationResult(registrationStatus: String, businessPartnerId: String): GrsRegistrationResult =
    registrationStatus match {
      case "REGISTERED"              => registered(businessPartnerId)
      case "REGISTRATION_NOT_CALLED" => registrationNotCalled
      case "REGISTRATION_FAILED"     => registrationFailed
    }

  def parseJourneyId(journeyId: String): (String, EntityType, String) =
    journeyId.split("-").toList match {
      case List(a, b, c) => (a, EntityType.enumerable.value(b).get, c)
      case _             => throw new IllegalStateException("Invalid journeyId")
    }
}
