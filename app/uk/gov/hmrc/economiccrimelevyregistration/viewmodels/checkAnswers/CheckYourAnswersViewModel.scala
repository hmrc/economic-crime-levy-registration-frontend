/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers

import play.api.http._
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration, RegistrationType}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}

case class CheckYourAnswersViewModel(
  registration: Registration,
  getSubscriptionResponse: Option[GetSubscriptionResponse],
  eclReference: Option[String]
) extends TrackRegistrationChanges {

  val hasSecondContact: Boolean = registration.contacts.secondContact.contains(true)

  def addressDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = addIfNot(
        hasAddressChanged,
        ContactAddressSummary.row(
          registration.useRegisteredOfficeAddressAsContactAddress,
          registration.contactAddress
        )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def firstContactDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIfNot(hasSecondContactDetailsPresentChanged, SecondContactSummary.row(registration.contacts.secondContact))
          ++ addIfNot(
            hasFirstContactNameChanged,
            FirstContactNameSummary.row(registration.contacts.firstContactDetails.name)
          )
          ++ addIfNot(
            hasFirstContactRoleChanged,
            FirstContactRoleSummary.row(registration.contacts.firstContactDetails.role)
          )
          ++ addIfNot(
            hasFirstContactEmailChanged,
            FirstContactEmailSummary.row(registration.contacts.firstContactDetails.emailAddress)
          )
          ++ addIfNot(
            hasFirstContactPhoneChanged,
            FirstContactNumberSummary.row(
              registration.contacts.firstContactDetails.telephoneNumber
            )
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contactDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIfNot(hasSecondContactDetailsPresentChanged, SecondContactSummary.row(registration.contacts.secondContact))
          ++ addIfNot(
            hasFirstContactNameChanged,
            ContactNameSummary.row(registration.contacts.firstContactDetails.name)
          )
          ++ addIfNot(
            hasFirstContactRoleChanged,
            ContactRoleSummary.row(registration.contacts.firstContactDetails.role)
          )
          ++ addIfNot(
            hasFirstContactEmailChanged,
            ContactEmailSummary.row(registration.contacts.firstContactDetails.emailAddress)
          )
          ++ addIfNot(
            hasFirstContactPhoneChanged,
            ContactNumberSummary.row(
              registration.contacts.firstContactDetails.telephoneNumber
            )
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def secondContactDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIfNot(
          hasSecondContactNameChanged,
          SecondContactNameSummary.row(registration.contacts.secondContactDetails.name)
        )
          ++ addIfNot(
            hasSecondContactRoleChanged,
            SecondContactRoleSummary.row(registration.contacts.secondContactDetails.role)
          )
          ++ addIfNot(
            hasSecondContactEmailChanged,
            SecondContactEmailSummary.row(registration.contacts.secondContactDetails.emailAddress)
          )
          ++ addIfNot(
            hasSecondContactPhoneChanged,
            SecondContactNumberSummary.row(registration.contacts.secondContactDetails.telephoneNumber)
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def otherEntityDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(
          isInitialRegistration,
          BusinessNameSummary.row(registration.otherEntityJourneyData.businessName)
        )
          ++ addIf(
            isInitialRegistration,
            CharityRegistrationNumberSummary.row(registration.otherEntityJourneyData.charityRegistrationNumber)
          )
          ++ addIf(
            isInitialRegistration,
            DoYouHaveCrnSummary.row(registration.otherEntityJourneyData.isUkCrnPresent)
          )
          ++ addIf(
            isInitialRegistration,
            CompanyRegistrationNumberSummary.row(
              registration.otherEntityJourneyData.companyRegistrationNumber,
              registration.entityType
            )
          )
          ++ addIf(
            isInitialRegistration,
            DoYouHaveCtUtrSummary.row(registration.otherEntityJourneyData.isCtUtrPresent, registration.entityType)
          )
          ++ addIf(isInitialRegistration, UtrTypeSummary.row(registration.otherEntityJourneyData.utrType))
          ++ addIf(isInitialRegistration, OtherEntitySaUtrSummary.row(registration.otherEntityJourneyData.saUtr))
          ++ addIf(
            isInitialRegistration,
            OtherEntityCtUtrSummary.row(registration.otherEntityJourneyData.ctUtr, registration.entityType)
          )
          ++ addIf(isInitialRegistration, OtherEntityPostcodeSummary.row(registration.otherEntityJourneyData.postcode))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def organisationDetails(liabilityRow: Option[SummaryListRow] = None)(implicit
    messages: Messages
  ): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(
          isInitialRegistration,
          AmlRegulatedActivitySummary.row(registration.carriedOutAmlRegulatedActivityInCurrentFy)
        )
          ++ addIfNot(hasAmlSupervisorChanged, AmlSupervisorSummary.row(registration.amlSupervisor, registrationType))
          ++ addIf(isInitialRegistration, RelevantAp12MonthsSummary.row(registration.relevantAp12Months))
          ++ addIf(isInitialRegistration, RelevantApLengthSummary.row(registration.relevantApLength))
          ++ addIf(isInitialRegistration, UkRevenueSummary.row(registration.relevantApRevenue))
          ++ addIf(isInitialRegistration, EntityTypeSummary.row(registration.entityType))
          ++ addIf(isInitialRegistration, EntityNameSummary.row(registration.entityName, registration.entityType))
          ++ addIf(isInitialRegistration, CompanyNumberSummary.row(registration.companyNumber))
          ++ addIf(isInitialRegistration, CtUtrSummary.row(registration.ctUtr))
          ++ addIf(isInitialRegistration, SaUtrSummary.row(registration.saUtr))
          ++ addIf(isInitialRegistration, NinoSummary.row(registration.nino))
          ++ addIf(isInitialRegistration, DateOfBirthSummary.row(registration.dateOfBirth))
          ++ addIf(isInitialRegistration, liabilityRow)
          ++ addIfNot(hasBusinessSectorChanged, BusinessSectorSummary.row(registration.businessSector))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def eclDetails(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = addIf(isAmendRegistration, EclReferenceNumberSummary.row(eclReference)).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendReasonDetails(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        AmendReasonSummary.row(registration.amendReason)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendedAnswersDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (addIf(hasBusinessSectorChanged, BusinessSectorSummary.row(registration.businessSector))
        ++ addIf(
          hasAddressChanged,
          ContactAddressSummary
            .row(registration.useRegisteredOfficeAddressAsContactAddress, registration.contactAddress)
        ) ++ addIf(
          hasAmlSupervisorChanged,
          AmlSupervisorSummary.row(registration.amlSupervisor, registrationType)
        ) ++ addIf(
          hasFirstContactNameChanged,
          FirstContactNameSummary.row(registration.contacts.firstContactDetails.name)
        ) ++ addIf(
          hasFirstContactRoleChanged,
          FirstContactRoleSummary.row(registration.contacts.firstContactDetails.role)
        ) ++ addIf(
          hasFirstContactEmailChanged,
          FirstContactEmailSummary.row(registration.contacts.firstContactDetails.emailAddress)
        ) ++ addIf(
          hasFirstContactPhoneChanged,
          FirstContactNumberSummary
            .row(registration.contacts.firstContactDetails.telephoneNumber)
        ) ++ addIf(
          hasSecondContactDetailsPresentChanged,
          SecondContactSummary.row(registration.contacts.secondContact)
        ) ++ addIf(
          hasSecondContactNameChanged && hasSecondContact,
          SecondContactNameSummary.row(registration.contacts.secondContactDetails.name)
        ) ++ addIf(
          hasSecondContactRoleChanged && hasSecondContact,
          SecondContactRoleSummary.row(registration.contacts.secondContactDetails.role)
        ) ++ addIf(
          hasSecondContactPhoneChanged && hasSecondContact,
          SecondContactNumberSummary.row(registration.contacts.secondContactDetails.telephoneNumber)
        ) ++ addIf(
          hasSecondContactEmailChanged && hasSecondContact,
          SecondContactEmailSummary.row(registration.contacts.secondContactDetails.emailAddress)
        )).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def addIf[T](condition: Boolean, value: T): Seq[T]    = if (condition) Seq(value) else Seq.empty
  private def addIfNot[T](condition: Boolean, value: T): Seq[T] = if (!condition) Seq(value) else Seq.empty

  val registrationType: Option[RegistrationType] = registration.registrationType

}

object CheckYourAnswersViewModel {
  implicit val format: OFormat[CheckYourAnswersViewModel] = Json.format[CheckYourAnswersViewModel]

  implicit val contentType: ContentTypeOf[CheckYourAnswersViewModel] =
    ContentTypeOf[CheckYourAnswersViewModel](Some(ContentTypes.JSON))
  implicit val writes: Writeable[CheckYourAnswersViewModel]          = Writeable(
    Writeable.writeableOf_JsValue.transform.compose(format.writes)
  )
}
