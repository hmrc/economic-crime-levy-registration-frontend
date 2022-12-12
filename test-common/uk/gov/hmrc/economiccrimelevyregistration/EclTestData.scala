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
import scala.util.Try

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

case class GroupEnrolmentsResponseWithEcl(groupEnrolmentsResponse: GroupEnrolmentsResponse)

case class GroupEnrolmentsResponseWithoutEcl(groupEnrolmentsResponse: GroupEnrolmentsResponse)

case class IncorporatedEntityJourneyDataWithSuccessfulRegistration(
  incorporatedEntityJourneyData: IncorporatedEntityJourneyData
)

case class InvalidDayMonthYear(day: String, month: String, year: String)

trait EclTestData {

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbInvalidDayMonthYear: Arbitrary[InvalidDayMonthYear] = Arbitrary {
    def isInvalidLocalDate(dayMonthYear: (String, String, String)): Boolean = {
      val (day, month, year) = dayMonthYear
      Try {
        LocalDate.of(year.toInt, month.toInt, day.toInt)
      }.toOption.fold(true)(_ => false)
    }

    def nonEmptyString: Gen[String] = Gen.nonEmptyListOf[Char](Arbitrary.arbChar.arbitrary).map(_.mkString)

    val dayMonthYearGen: Gen[(String, String, String)] = for {
      day   <- nonEmptyString
      month <- nonEmptyString
      year  <- nonEmptyString
    } yield (day, month, year)

    dayMonthYearGen.retryUntil(dmy => isInvalidLocalDate(dmy)).map(s => InvalidDayMonthYear(s._1, s._2, s._3))
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
