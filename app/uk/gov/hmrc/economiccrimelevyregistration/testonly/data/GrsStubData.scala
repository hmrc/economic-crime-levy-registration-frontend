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

import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus.{Fail, Pass}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._

import java.time.LocalDate

trait GrsStubData {

  private def defaultIncorporatedEntityJourneyData(
    businessVerification: Option[BusinessVerificationResult],
    registrationResult: GrsRegistrationResult,
    identifiersMatch: Boolean
  ): IncorporatedEntityJourneyData = IncorporatedEntityJourneyData(
    companyProfile = validCompanyProfile(partnership = false),
    ctutr = "1234567890",
    identifiersMatch = identifiersMatch,
    businessVerification = businessVerification,
    registration = registrationResult
  )

  private def defaultSoleTraderJourneyData(
    businessVerification: Option[BusinessVerificationResult],
    registrationResult: GrsRegistrationResult,
    identifiersMatch: Boolean
  ): SoleTraderEntityJourneyData = SoleTraderEntityJourneyData(
    fullName = FullName(
      firstName = "John",
      lastName = "Doe"
    ),
    dateOfBirth = LocalDate.parse("1975-01-31"),
    nino = Some("BB111111B"),
    sautr = Some("1234567890"),
    identifiersMatch = identifiersMatch,
    businessVerification = businessVerification,
    registration = registrationResult
  )

  private def defaultPartnershipJourneyData(
    entityType: EntityType,
    businessVerification: Option[BusinessVerificationResult],
    registrationResult: GrsRegistrationResult,
    identifiersMatch: Boolean
  ): PartnershipEntityJourneyData =
    PartnershipEntityJourneyData(
      companyProfile = entityType match {
        case GeneralPartnership | ScottishPartnership => None
        case _                                        =>
          Some(validCompanyProfile(partnership = true))
      },
      sautr = Some("1234567890"),
      postcode = Some("AA11AA"),
      identifiersMatch = identifiersMatch,
      businessVerification = businessVerification,
      registration = registrationResult
    )

  private def validCompanyProfile(partnership: Boolean): CompanyProfile = CompanyProfile(
    companyName = if (partnership) "Test Partnership Name" else "Test Company Name",
    companyNumber = "01234567",
    dateOfIncorporation = "2007-12-03",
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

  val registered: GrsRegistrationResult = GrsRegistrationResult(
    registrationStatus = Registered,
    registeredBusinessPartnerId = Some("XA0000000000001"),
    failures = None
  )

  val registrationNotCalled: GrsRegistrationResult = GrsRegistrationResult(
    registrationStatus = RegistrationNotCalled,
    registeredBusinessPartnerId = None,
    failures = None
  )

  val registrationFailedPartyTypeMismatch: GrsRegistrationResult = GrsRegistrationResult(
    registrationStatus = RegistrationFailed,
    registeredBusinessPartnerId = None,
    failures = Some(
      Seq(
        GrsRegistrationResultFailure(
          code = GrsErrorCodes.partyTypeMismatch,
          reason = "The remote endpoint has indicated there is Party Type mismatch"
        )
      )
    )
  )

  val registrationFailedGeneric: GrsRegistrationResult = GrsRegistrationResult(
    registrationStatus = RegistrationFailed,
    registeredBusinessPartnerId = None,
    failures = None
  )

  val bvFailed: Option[BusinessVerificationResult] = Some(BusinessVerificationResult(Fail))
  val bvPassed: Option[BusinessVerificationResult] = Some(BusinessVerificationResult(Pass))

  def constructGrsStubFormData(
    entityType: EntityType,
    businessVerification: Option[BusinessVerificationResult] = None,
    registrationResult: GrsRegistrationResult,
    identifiersMatch: Boolean
  ): String = entityType match {
    case UkLimitedCompany | UnlimitedCompany | RegisteredSociety =>
      Json.prettyPrint(
        Json.toJson(defaultIncorporatedEntityJourneyData(businessVerification, registrationResult, identifiersMatch))
      )
    case SoleTrader                                              =>
      Json.prettyPrint(
        Json.toJson(defaultSoleTraderJourneyData(businessVerification, registrationResult, identifiersMatch))
      )
    case GeneralPartnership | ScottishPartnership | LimitedPartnership | LimitedLiabilityPartnership |
        ScottishLimitedPartnership =>
      Json.prettyPrint(
        Json.toJson(
          defaultPartnershipJourneyData(entityType, businessVerification, registrationResult, identifiersMatch)
        )
      )
    case o                                                       => throw new IllegalStateException(s"$o is not a valid GRS entity type")
  }

}
