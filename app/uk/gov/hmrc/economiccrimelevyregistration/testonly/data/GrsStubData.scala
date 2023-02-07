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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.data

import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{GeneralPartnership, LimitedLiabilityPartnership, LimitedPartnership, ScottishLimitedPartnership, ScottishPartnership, SoleTrader, UkLimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus.Fail
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._

import java.time.Instant
import java.util.Date

trait GrsStubData {

  private val defaultIncorporatedEntityJourneyData: IncorporatedEntityJourneyData = IncorporatedEntityJourneyData(
    companyProfile = validCompanyProfile(partnership = false),
    ctutr = "1234567890",
    identifiersMatch = true,
    businessVerification = None,
    registration = registered()
  )

  private val defaultSoleTraderJourneyData: SoleTraderEntityJourneyData = SoleTraderEntityJourneyData(
    fullName = FullName(
      firstName = "John",
      lastName = "Doe"
    ),
    dateOfBirth = Date.from(Instant.parse("1975-01-31T00:00:00.00Z")),
    nino = Some("BB111111B"),
    sautr = Some("1234567890"),
    identifiersMatch = true,
    businessVerification = None,
    registration = registered()
  )

  private def defaultPartnershipJourneyData(entityType: EntityType): PartnershipEntityJourneyData =
    PartnershipEntityJourneyData(
      companyProfile = entityType match {
        case GeneralPartnership | ScottishPartnership => None
        case _                                        =>
          Some(validCompanyProfile(partnership = true))
      },
      sautr = Some("1234567890"),
      postcode = Some("AA11AA"),
      identifiersMatch = true,
      businessVerification = None,
      registration = registered()
    )

  private def validCompanyProfile(partnership: Boolean): CompanyProfile = CompanyProfile(
    companyName = if (partnership) "Test Partnership Name" else "Test Company Name",
    companyNumber = "01234567",
    dateOfIncorporation = Date.from(Instant.parse("2007-12-03T10:15:30.00Z")),
    unsanitisedCHROAddress = IncorporatedEntityAddress(
      address_line_1 = Some("Test Address Line 1"),
      address_line_2 = Some("Test Address Line 2"),
      country = Some("United Kingdom"),
      locality = Some("Test Town"),
      po_box = None,
      postal_code = Some("AB1 2CD"),
      premises = None,
      region = Some("Test Region")
    )
  )

  private def registered(): GrsRegistrationResult = GrsRegistrationResult(
    registrationStatus = Registered,
    registeredBusinessPartnerId = Some("X00000000000001"),
    failures = None
  )

  private val registrationNotCalled: GrsRegistrationResult = GrsRegistrationResult(
    registrationStatus = RegistrationNotCalled,
    registeredBusinessPartnerId = None,
    failures = None
  )

  private val registrationFailed: GrsRegistrationResult = GrsRegistrationResult(
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

  val registrationFailedPartyTypeMismatchJson: String =
    Json.prettyPrint(
      Json.obj(
        "registration" -> Json.toJson(registrationFailed)
      )
    )

  val registrationNotCalledIdentifierMismatchJson: String =
    Json.prettyPrint(
      Json.obj(
        "identifiersMatch" -> false,
        "registration"     -> Json.toJson(registrationNotCalled)
      )
    )

  val registrationNotCalledBvFailedJson: String =
    Json.prettyPrint(
      Json.obj(
        "businessVerification" -> Json.toJson(BusinessVerificationResult(Fail)),
        "registration"         -> Json.toJson(registrationNotCalled)
      )
    )

  def constructDefaultGrsStubFormData(entityType: EntityType): JsValue = entityType match {
    case UkLimitedCompany => Json.toJson(defaultIncorporatedEntityJourneyData)
    case SoleTrader       => Json.toJson(defaultSoleTraderJourneyData)
    case GeneralPartnership | ScottishPartnership | LimitedPartnership | LimitedLiabilityPartnership |
        ScottishLimitedPartnership =>
      Json.toJson(defaultPartnershipJourneyData(entityType))
    case o                => throw new IllegalStateException(s"$o is not a valid GRS entity type")
  }

}
