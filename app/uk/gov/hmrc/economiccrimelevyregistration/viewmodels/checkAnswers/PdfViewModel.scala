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

import play.api.http.{ContentTypeOf, ContentTypes, Writeable}
import play.api.i18n.Messages
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration, RegistrationAdditionalInfo, RegistrationType}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._

import java.time.LocalDate

case class PdfViewModel(
  registration: Registration,
  getSubscriptionResponse: Option[GetSubscriptionResponse],
  eclReference: Option[String],
  additionalInfo: Option[RegistrationAdditionalInfo] = None
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
        addIfNot(
          hasSecondContactDetailsPresentChanged,
          formatRow(SecondContactSummary.row(registration.contacts.secondContact))
        )
          ++ addIfNot(
            hasFirstContactNameChanged,
            formatRow(FirstContactNameSummary.row(registration.contacts.firstContactDetails.name))
          )
          ++ addIfNot(
            hasFirstContactRoleChanged,
            formatRow(FirstContactRoleSummary.row(registration.contacts.firstContactDetails.role))
          )
          ++ addIfNot(
            hasFirstContactEmailChanged,
            formatRow(FirstContactEmailSummary.row(registration.contacts.firstContactDetails.emailAddress))
          )
          ++ addIfNot(
            hasFirstContactPhoneChanged,
            formatRow(
              FirstContactNumberSummary.row(
                registration.contacts.firstContactDetails.telephoneNumber
              )
            )
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contactDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIfNot(
          hasSecondContactDetailsPresentChanged,
          formatRow(SecondContactSummary.row(registration.contacts.secondContact))
        )
          ++ addIfNot(
            hasFirstContactNameChanged,
            formatRow(ContactNameSummary.row(registration.contacts.firstContactDetails.name))
          )
          ++ addIfNot(
            hasFirstContactRoleChanged,
            formatRow(ContactRoleSummary.row(registration.contacts.firstContactDetails.role))
          )
          ++ addIfNot(
            hasFirstContactEmailChanged,
            formatRow(ContactEmailSummary.row(registration.contacts.firstContactDetails.emailAddress))
          )
          ++ addIfNot(
            hasFirstContactPhoneChanged,
            formatRow(
              ContactNumberSummary.row(
                registration.contacts.firstContactDetails.telephoneNumber
              )
            )
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def secondContactDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIfNot(
          hasSecondContactNameChanged,
          formatRow(SecondContactNameSummary.row(registration.contacts.secondContactDetails.name))
        )
          ++ addIfNot(
            hasSecondContactRoleChanged,
            formatRow(SecondContactRoleSummary.row(registration.contacts.secondContactDetails.role))
          )
          ++ addIfNot(
            hasSecondContactEmailChanged,
            formatRow(SecondContactEmailSummary.row(registration.contacts.secondContactDetails.emailAddress))
          )
          ++ addIfNot(
            hasSecondContactPhoneChanged,
            formatRow(SecondContactNumberSummary.row(registration.contacts.secondContactDetails.telephoneNumber))
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def otherEntityDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(
          isInitialRegistration,
          formatRow(BusinessNameSummary.row(registration.otherEntityJourneyData.businessName))
        )
          ++ addIf(
            isInitialRegistration,
            formatRow(
              CharityRegistrationNumberSummary.row(registration.otherEntityJourneyData.charityRegistrationNumber)
            )
          )
          ++ addIf(
            isInitialRegistration,
            formatRow(DoYouHaveCrnSummary.row(registration.otherEntityJourneyData.isUkCrnPresent))
          )
          ++ addIf(
            isInitialRegistration,
            formatRow(
              CompanyRegistrationNumberSummary.row(
                registration.otherEntityJourneyData.companyRegistrationNumber,
                registration.entityType
              )
            )
          )
          ++ addIf(
            isInitialRegistration,
            formatRow(
              DoYouHaveCtUtrSummary.row(registration.otherEntityJourneyData.isCtUtrPresent, registration.entityType)
            )
          )
          ++ addIf(isInitialRegistration, formatRow(UtrTypeSummary.row(registration.otherEntityJourneyData.utrType)))
          ++ addIf(isInitialRegistration, formatRow(OtherEntitySaUtrSummary.row(registration.saUtr)))
          ++ addIf(
            isInitialRegistration,
            formatRow(OtherEntityCtUtrSummary.row(registration.ctUtr, registration.entityType))
          )
          ++ addIf(
            isInitialRegistration,
            formatRow(OtherEntityPostcodeSummary.row(registration.otherEntityJourneyData.postcode))
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def organisationDetails(liabilityRow: Option[SummaryListRow] = None)(implicit
    messages: Messages
  ): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(
          isInitialRegistration,
          formatRow(AmlRegulatedActivitySummary.row(registration.carriedOutAmlRegulatedActivityInCurrentFy))
        )
          ++ addIfNot(
            hasAmlSupervisorChanged,
            formatRow(AmlSupervisorSummary.row(registration.amlSupervisor, registrationType))
          )
          ++ addIf(isInitialRegistration, formatRow(RelevantAp12MonthsSummary.row(registration.relevantAp12Months)))
          ++ addIf(isInitialRegistration, formatRow(RelevantApLengthSummary.row(registration.relevantApLength)))
          ++ addIf(isInitialRegistration, formatRow(UkRevenueSummary.row(registration.relevantApRevenue)))
          ++ addIf(isInitialRegistration, formatRow(EntityTypeSummary.row(registration.entityType)))
          ++ addIf(
            isInitialRegistration,
            formatRow(EntityNameSummary.row(registration.entityName, registration.entityType))
          )
          ++ addIf(isInitialRegistration, formatRow(CompanyNumberSummary.row(registration.companyNumber)))
          ++ addIf(isInitialRegistration, formatRow(CtUtrSummary.row(registration.ctUtr)))
          ++ addIf(isInitialRegistration, formatRow(SaUtrSummary.row(registration.saUtr)))
          ++ addIf(isInitialRegistration, formatRow(NinoSummary.row(registration.nino)))
          ++ addIf(isInitialRegistration, formatRow(DateOfBirthSummary.row(registration.dateOfBirth)))
          ++ addIf(isInitialRegistration, formatRow(liabilityRow))
          ++ addIf(
            isInitialRegistration || isAmendRegistration,
            getLiabilityRow
          )
          ++ addIfNot(hasBusinessSectorChanged, formatRow(BusinessSectorSummary.row(registration.businessSector)))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def getLiabilityRow(implicit messages: Messages) = {
    val liabilityStartDate = additionalInfo.flatMap(additionalInfo => additionalInfo.liabilityStartDate)
    if (liabilityStartDate.isDefined) {
      LiabilityDateSummary.row(additionalInfo.flatMap(additionalInfo => additionalInfo.liabilityStartDate))
    } else {
      LiabilityDateSummary.row(
        getSubscriptionResponse.map(response => LocalDate.parse(response.additionalDetails.liabilityStartDate))
      )
    }
  }

  def eclDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = addIf(isAmendRegistration, formatRow(EclReferenceNumberSummary.row(eclReference))).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendReasonDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        formatRow(AmendReasonSummary.row(registration.amendReason))
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def amendedAnswersDetails()(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = (
        addIf(hasBusinessSectorChanged, formatRow(BusinessSectorSummary.row(registration.businessSector)))
          ++ addIf(
            hasAddressChanged,
            formatRow(
              ContactAddressSummary.row(
                registration.useRegisteredOfficeAddressAsContactAddress,
                registration.contactAddress
              )
            )
          )
          ++ addIf(
            hasAmlSupervisorChanged,
            formatRow(AmlSupervisorSummary.row(registration.amlSupervisor, registrationType))
          )
          ++ addIf(
            hasFirstContactNameChanged,
            formatRow(
              FirstContactNameSummary.row(registration.contacts.firstContactDetails.name)
            )
          )
          ++ addIf(
            hasFirstContactRoleChanged,
            formatRow(
              FirstContactRoleSummary.row(registration.contacts.firstContactDetails.role)
            )
          )
          ++ addIf(
            hasFirstContactEmailChanged,
            formatRow(
              FirstContactEmailSummary.row(
                registration.contacts.firstContactDetails.emailAddress
              )
            )
          )
          ++ addIf(
            hasFirstContactPhoneChanged,
            formatRow(
              FirstContactNumberSummary.row(
                registration.contacts.firstContactDetails.telephoneNumber
              )
            )
          )
          ++ addIf(
            hasSecondContactDetailsPresentChanged,
            formatRow(SecondContactSummary.row(registration.contacts.secondContact))
          )
          ++ addIf(
            hasSecondContactNameChanged,
            formatRow(SecondContactNameSummary.row(registration.contacts.secondContactDetails.name))
          )
          ++ addIf(
            hasSecondContactRoleChanged,
            formatRow(SecondContactRoleSummary.row(registration.contacts.secondContactDetails.role))
          )
          ++ addIf(
            hasSecondContactPhoneChanged,
            formatRow(SecondContactNumberSummary.row(registration.contacts.secondContactDetails.telephoneNumber))
          )
          ++ addIf(
            hasSecondContactEmailChanged,
            formatRow(SecondContactEmailSummary.row(registration.contacts.secondContactDetails.emailAddress))
          )
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def addIf[T](condition: Boolean, value: T): Seq[T]    = if (condition) Seq(value) else Seq.empty
  private def addIfNot[T](condition: Boolean, value: T): Seq[T] = if (!condition) Seq(value) else Seq.empty

  val registrationType: Option[RegistrationType]                             = registration.registrationType
  private def formatRow(row: Option[SummaryListRow]): Option[SummaryListRow] = row.map(_.copy(actions = None))
}

object PdfViewModel {
  implicit val format: OFormat[PdfViewModel] = Json.format[PdfViewModel]

  implicit val contentType: ContentTypeOf[PdfViewModel] =
    ContentTypeOf[PdfViewModel](Some(ContentTypes.JSON))
  implicit val writes: Writeable[PdfViewModel]          = Writeable(
    Writeable.writeableOf_JsValue.transform.compose(format.writes)
  )
}
