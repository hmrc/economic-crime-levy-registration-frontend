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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.auth.core.{AffinityGroup, EnrolmentIdentifier, Enrolments, Enrolment => AuthEnrolment}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.{Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.{EclEnrolment, Enrolment, GroupEnrolmentsResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.RegistrationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.VerificationStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs._

import java.time.{Instant, LocalDate}

final case class IncorporatedEntityType(entityType: EntityType)

final case class PartnershipType(entityType: EntityType)

final case class ScottishOrGeneralPartnershipType(entityType: EntityType)

final case class LimitedPartnershipType(entityType: EntityType)

final case class SelfAssessmentEntityType(entityType: EntityType)

final case class EnrolmentsWithEcl(enrolments: Enrolments, eclReferenceNumber: String)

final case class EnrolmentsWithoutEcl(enrolments: Enrolments)

final case class RegistrationWithUnincorporatedAssociation(registration: Registration)
final case class ValidTrustRegistration(registration: Registration)

final case class GroupEnrolmentsResponseWithEcl(
  groupEnrolmentsResponse: GroupEnrolmentsResponse,
  eclReferenceNumber: String
)

final case class GroupEnrolmentsResponseWithoutEcl(groupEnrolmentsResponse: GroupEnrolmentsResponse)

final case class IncorporatedEntityJourneyDataWithSuccessfulRegistration(
  incorporatedEntityJourneyData: IncorporatedEntityJourneyData
)

final case class EligibleAmlSupervisor(amlSupervisor: AmlSupervisor)

final case class IneligibleAmlSupervisor(amlSupervisor: AmlSupervisor)

final case class IncorporatedEntityJourneyDataWithValidCompanyProfile(
  incorporatedEntityJourneyData: IncorporatedEntityJourneyData
)

trait EclTestData {

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbIncorporatedEntityType: Arbitrary[IncorporatedEntityType] = Arbitrary {
    for {
      companyType <- Gen.oneOf(Seq(UkLimitedCompany, UnlimitedCompany, RegisteredSociety))
    } yield IncorporatedEntityType(companyType)
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

  implicit val arbScottishOrGeneralPartnershipType: Arbitrary[ScottishOrGeneralPartnershipType] = Arbitrary {
    for {
      scottishOrGeneralPartnershipType <- Gen.oneOf(
                                            Seq(
                                              GeneralPartnership,
                                              ScottishPartnership
                                            )
                                          )
    } yield ScottishOrGeneralPartnershipType(scottishOrGeneralPartnershipType)
  }

  implicit val arbLimitedPartnershipType: Arbitrary[LimitedPartnershipType] = Arbitrary {
    for {
      limitedPartnershipType <- Gen.oneOf(
                                  Seq(
                                    LimitedPartnership,
                                    LimitedLiabilityPartnership,
                                    ScottishLimitedPartnership
                                  )
                                )
    } yield LimitedPartnershipType(limitedPartnershipType)
  }

  implicit val arbSelfAssessmentEntityType: Arbitrary[SelfAssessmentEntityType] = Arbitrary {
    for {
      selfAssessmentEntityType <- Gen.oneOf(
                                    Seq(
                                      LimitedPartnership,
                                      LimitedLiabilityPartnership,
                                      GeneralPartnership,
                                      ScottishPartnership,
                                      ScottishLimitedPartnership,
                                      SoleTrader
                                    )
                                  )
    } yield SelfAssessmentEntityType(selfAssessmentEntityType)
  }

  implicit val arbEnrolmentsWithEcl: Arbitrary[EnrolmentsWithEcl] = Arbitrary {
    for {
      enrolments         <- Arbitrary.arbitrary[Enrolments]
      enrolment          <- Arbitrary.arbitrary[AuthEnrolment]
      eclReferenceNumber <- Arbitrary.arbitrary[String]
      eclEnrolment        = enrolment.copy(
                              key = EclEnrolment.ServiceName,
                              identifiers =
                                Seq(EnrolmentIdentifier(key = EclEnrolment.IdentifierKey, value = eclReferenceNumber))
                            )
    } yield EnrolmentsWithEcl(enrolments.copy(enrolments.enrolments + eclEnrolment), eclReferenceNumber)
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
      ),
      enrolmentsWithEcl.eclReferenceNumber
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

  def arbEligibleAmlSupervisor(appConfig: AppConfig): Arbitrary[EligibleAmlSupervisor] = Arbitrary {
    for {
      amlSupervisorType     <- Gen.oneOf(Seq(AmlSupervisorType.Hmrc, AmlSupervisorType.Other))
      otherProfessionalBody <- Gen.oneOf(appConfig.amlProfessionalBodySupervisors)
    } yield amlSupervisorType match {
      case AmlSupervisorType.Other =>
        EligibleAmlSupervisor(AmlSupervisor(AmlSupervisorType.Other, Some(otherProfessionalBody)))
      case _                       => EligibleAmlSupervisor(AmlSupervisor(amlSupervisorType, None))
    }
  }

  implicit val arbIneligibleAmlSupervisor: Arbitrary[IneligibleAmlSupervisor] = Arbitrary {
    for {
      amlSupervisorType <-
        Gen.oneOf(Seq(AmlSupervisorType.FinancialConductAuthority, AmlSupervisorType.GamblingCommission))
    } yield IneligibleAmlSupervisor(AmlSupervisor(amlSupervisorType, None))
  }

  implicit val arbAffinityGroup: Arbitrary[AffinityGroup] = Arbitrary {
    Gen.oneOf(Seq(Organisation, Individual, Agent))
  }

  def successfulGrsRegistrationResult(businessPartnerId: String): GrsRegistrationResult =
    GrsRegistrationResult(Registered, registeredBusinessPartnerId = Some(businessPartnerId), None)

  val failedRegistrationResult: GrsRegistrationResult =
    GrsRegistrationResult(RegistrationFailed, None, None)

  val partyTypeMismatchResult: GrsRegistrationResult =
    GrsRegistrationResult(
      RegistrationFailed,
      None,
      Some(
        Seq(
          GrsRegistrationResultFailure(
            GrsErrorCodes.PartyTypeMismatch,
            "The remote endpoint has indicated there is Party Type mismatch"
          )
        )
      )
    )

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
  val validContactDetails: ContactDetails  = ContactDetails(
    name = Some(alphaNumericString),
    role = Some(alphaNumericString),
    emailAddress = Some(alphaNumericString),
    telephoneNumber = Some(alphaNumericString)
  )

  implicit val arbRegistrationWithUnincorporatedAssociation: Arbitrary[RegistrationWithUnincorporatedAssociation] =
    Arbitrary {
      for {
        registration          <- Arbitrary.arbitrary[Registration]
        businessName          <- Arbitrary.arbitrary[String]
        ctutr                 <- Arbitrary.arbitrary[String]
        postcode              <- Arbitrary.arbitrary[String]
        isCtUtrPresent        <- Arbitrary.arbitrary[Boolean]
        otherEntityJourneyData = OtherEntityJourneyData(
                                   Some(UnincorporatedAssociation),
                                   Some(businessName),
                                   None,
                                   None,
                                   None,
                                   Some(ctutr),
                                   Some(isCtUtrPresent),
                                   None,
                                   None,
                                   Some(postcode)
                                 )
      } yield RegistrationWithUnincorporatedAssociation(registration =
        registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData), entityType = Some(Other))
      )
    }

  implicit val arbValidTrustRegistration: Arbitrary[ValidTrustRegistration] =
    Arbitrary {
      for {
        registration          <- Arbitrary.arbitrary[Registration]
        businessName          <- Arbitrary.arbitrary[String]
        ctutr                 <- Arbitrary.arbitrary[String]
        otherEntityJourneyData = OtherEntityJourneyData(
                                   Some(Trust),
                                   Some(businessName),
                                   None,
                                   None,
                                   None,
                                   Some(ctutr),
                                   None,
                                   None,
                                   None,
                                   None
                                 )
      } yield ValidTrustRegistration(registration =
        registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData), entityType = Some(Other))
      )
    }
}
