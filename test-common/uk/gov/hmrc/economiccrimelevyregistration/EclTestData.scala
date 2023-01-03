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

package uk.gov.hmrc.economiccrimelevyregistration

import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.{Enrolments, Enrolment => AuthEnrolment}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, Enrolment, GroupEnrolmentsResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._

import java.time.{Instant, LocalDate}

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

case class GroupEnrolmentsResponseWithEcl(groupEnrolmentsResponse: GroupEnrolmentsResponse)

case class GroupEnrolmentsResponseWithoutEcl(groupEnrolmentsResponse: GroupEnrolmentsResponse)

case class IncorporatedEntityJourneyDataWithSuccessfulRegistration(
  incorporatedEntityJourneyData: IncorporatedEntityJourneyData
)

case class IncorporatedEntityJourneyDataWithValidCompanyProfile(
  incorporatedEntityJourneyData: IncorporatedEntityJourneyData
)

trait EclTestData {

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbPartnershipType: Arbitrary[PartnershipType] = Arbitrary {
    for {
      partnershipType <- Gen.oneOf(
                           Seq(
                             LimitedPartnership,
                             LimitedLiabilityPartnership,
                             GeneralPartnership,
                             ScottishPartnership,
                             ScottishLimitedPartnership
                           )
                         )
    } yield PartnershipType(partnershipType)
  }

  implicit val arbEnrolmentsWithEcl: Arbitrary[EnrolmentsWithEcl] = Arbitrary {
    for {
      enrolments  <- Arbitrary.arbitrary[Enrolments]
      enrolment   <- Arbitrary.arbitrary[AuthEnrolment]
      eclEnrolment = enrolment.copy(key = EclEnrolment.ServiceName)
    } yield EnrolmentsWithEcl(enrolments.copy(enrolments.enrolments + eclEnrolment))
  }

  implicit val arbEnrolmentsWithoutEcl: Arbitrary[EnrolmentsWithoutEcl] = Arbitrary {
    Arbitrary
      .arbitrary[Enrolments]
      .retryUntil(!_.enrolments.exists(_.key == EclEnrolment.ServiceName))
      .map(EnrolmentsWithoutEcl)
  }

  implicit val arbGroupEnrolmentsResponseWithEcl: Arbitrary[GroupEnrolmentsResponseWithEcl] = Arbitrary {
    for {
      enrolmentsWithEcl <- Arbitrary.arbitrary[EnrolmentsWithEcl]
    } yield GroupEnrolmentsResponseWithEcl(
      GroupEnrolmentsResponse(
        authEnrolmentsToEnrolments(enrolmentsWithEcl.enrolments)
      )
    )
  }

  implicit val arbGroupEnrolmentsResponseWithoutEcl: Arbitrary[GroupEnrolmentsResponseWithoutEcl] = Arbitrary {
    for {
      enrolmentsWithoutEcl <- Arbitrary.arbitrary[EnrolmentsWithoutEcl]
    } yield GroupEnrolmentsResponseWithoutEcl(
      GroupEnrolmentsResponse(
        authEnrolmentsToEnrolments(enrolmentsWithoutEcl.enrolments)
      )
    )
  }

  implicit val arbIncorporatedEntityJourneyDataWithValidCompanyProfile
    : Arbitrary[IncorporatedEntityJourneyDataWithValidCompanyProfile] = Arbitrary {
    for {
      incorpEntityJourneyData <- Arbitrary.arbitrary[IncorporatedEntityJourneyData]
      addressLine1            <- Arbitrary.arbitrary[String]
      addressLine2            <- Arbitrary.arbitrary[String]
      townOrCity              <- Arbitrary.arbitrary[String]
      region                  <- Arbitrary.arbitrary[String]
      postcode                <- Arbitrary.arbitrary[String]
    } yield IncorporatedEntityJourneyDataWithValidCompanyProfile(
      incorpEntityJourneyData.copy(companyProfile =
        incorpEntityJourneyData.companyProfile.copy(unsanitisedCHROAddress =
          IncorporatedEntityAddress(
            address_line_1 = Some(addressLine1),
            address_line_2 = Some(addressLine2),
            locality = Some(townOrCity),
            region = Some(region),
            postal_code = Some(postcode),
            country = None,
            po_box = None,
            premises = None
          )
        )
      )
    )
  }

  def arbAmlSupervisor(appConfig: AppConfig): Arbitrary[AmlSupervisor] = Arbitrary {
    for {
      amlSupervisorType     <- Arbitrary.arbitrary[AmlSupervisorType]
      otherProfessionalBody <- Gen.oneOf(appConfig.amlProfessionalBodySupervisors)
    } yield amlSupervisorType match {
      case AmlSupervisorType.Other => AmlSupervisor(AmlSupervisorType.Other, Some(otherProfessionalBody))
      case _                       => AmlSupervisor(amlSupervisorType, None)
    }
  }

  def successfulGrsRegistrationResult(businessPartnerId: String): GrsRegistrationResult =
    GrsRegistrationResult(Registered, registeredBusinessPartnerId = Some(businessPartnerId), None)

  val failedRegistrationResult: GrsRegistrationResult =
    GrsRegistrationResult(RegistrationFailed, None, None)

  val registrationNotCalled: GrsRegistrationResult =
    GrsRegistrationResult(RegistrationNotCalled, None, None)

  val failedBvResult: Option[BusinessVerificationResult] = Some(BusinessVerificationResult(Fail))

  private def authEnrolmentsToEnrolments(authEnrolments: Enrolments) =
    authEnrolments.enrolments
      .map(e => Enrolment(e.key, e.identifiers.map(i => KeyValue(i.key, i.value))))
      .toSeq

  def alphaNumericString: String = Gen.alphaNumStr.retryUntil(_.nonEmpty).sample.get

  val testInternalId: String               = alphaNumericString
  val testGroupId: String                  = alphaNumericString
  val testEclRegistrationReference: String = alphaNumericString

}
