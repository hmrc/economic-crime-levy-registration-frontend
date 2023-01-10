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

package uk.gov.hmrc.economiccrimelevyregistration.services

import cats.data.ValidatedNec
import cats.implicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

import javax.inject.Inject

final case class DataMissingError(message: String)

class SubmissionValidationService @Inject() () {

  type ValidationResult[A] = ValidatedNec[DataMissingError, A]

  def validateRegistrationSubmission()(implicit request: RegistrationDataRequest[_]): ValidationResult[Registration] =
    (
      validateGrsJourneyData(request.registration),
      validateOptExists(request.registration.contacts.firstContactDetails.name, "First contact name"),
      validateOptExists(request.registration.contacts.firstContactDetails.role, "First contact role"),
      validateOptExists(request.registration.contacts.firstContactDetails.emailAddress, "First contact email"),
      validateOptExists(request.registration.contacts.firstContactDetails.telephoneNumber, "First contact number"),
      validateOptExists(request.registration.businessSector, "Business sector"),
      validateOptExists(request.registration.contactAddress, "Contact address"),
      validateOptExists(request.registration.amlSupervisor, "AML supervisor"),
      validateSecondContactDetails(request.registration)
    ).mapN((_, _, _, _, _, _, _, _, _) => request.registration)

  private def validateSecondContactDetails(registration: Registration): ValidationResult[Registration] =
    registration.contacts.secondContact match {
      case Some(true)  =>
        (
          validateOptExists(registration.contacts.secondContactDetails.name, "Second contact name"),
          validateOptExists(registration.contacts.secondContactDetails.role, "Second contact role"),
          validateOptExists(registration.contacts.secondContactDetails.emailAddress, "Second contact email"),
          validateOptExists(registration.contacts.secondContactDetails.telephoneNumber, "Second contact number")
        ).mapN((_, _, _, _) => registration)
      case Some(false) => registration.validNec
    }

  private def validateGrsJourneyData(registration: Registration): ValidationResult[Registration] = {
    val grsJourneyData: (
      Option[IncorporatedEntityJourneyData],
      Option[PartnershipEntityJourneyData],
      Option[SoleTraderEntityJourneyData]
    ) = (
      registration.incorporatedEntityJourneyData,
      registration.partnershipEntityJourneyData,
      registration.soleTraderEntityJourneyData
    )

    val validateBusinessPartnerId: Option[String] => ValidationResult[Registration] = {
      case Some(_) => registration.validNec
      case _       => DataMissingError(errorMessage("Business partner ID")).invalidNec
    }

    registration.entityType match {
      case Some(UkLimitedCompany) =>
        grsJourneyData match {
          case (Some(i), None, None) => validateBusinessPartnerId(i.registration.registeredBusinessPartnerId)
          case _                     => DataMissingError(errorMessage("Incorporated entity data")).invalidNec
        }
      case Some(
            LimitedLiabilityPartnership | GeneralPartnership | ScottishPartnership | LimitedPartnership |
            ScottishLimitedPartnership
          ) =>
        grsJourneyData match {
          case (None, Some(p), None) => validateBusinessPartnerId(p.registration.registeredBusinessPartnerId)
          case _                     => DataMissingError(errorMessage("Partnership data")).invalidNec
        }
      case Some(SoleTrader)       =>
        grsJourneyData match {
          case (None, None, Some(s)) => validateBusinessPartnerId(s.registration.registeredBusinessPartnerId)
          case _                     => DataMissingError(errorMessage("Sole trader data")).invalidNec
        }
      case _                      => DataMissingError("Entity type").invalidNec
    }
  }

  private def errorMessage(missingDataDescription: String): String = s"$missingDataDescription is missing"

  private def validateOptExists[T](optData: Option[T], description: String): ValidationResult[T] =
    optData match {
      case Some(value) => value.validNec
      case _           => DataMissingError(description).invalidNec
    }

}
