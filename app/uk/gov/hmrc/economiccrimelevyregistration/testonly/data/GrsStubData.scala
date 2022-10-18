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

import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{CompanyProfile, GrsRegistrationResult, GrsRegistrationResultFailures, IncorporatedEntityAddress}

import java.time.Instant
import java.util.Date

trait GrsStubData {
  val companyProfile: CompanyProfile = CompanyProfile(
    companyName = "Test Entity",
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

  private val registered = GrsRegistrationResult(
    registrationStatus = "REGISTERED",
    registeredBusinessPartnerId = Some("X00000123456789"),
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

  def registrationResult(registrationStatus: String): GrsRegistrationResult =
    registrationStatus match {
      case "REGISTERED"              => registered
      case "REGISTRATION_NOT_CALLED" => registrationNotCalled
      case "REGISTRATION_FAILED"     => registrationFailed
    }

  def parseJourneyId(journeyId: String): (String, String) =
    journeyId.split("-").toList match {
      case List(a, b) => (a, b)
      case _          => throw new IllegalStateException("Invalid journeyId")
    }
}
