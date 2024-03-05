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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, GetSubscriptionResponse}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.deregister.{CompanyNameSummary, DeregisterDateSummary, DeregisterEmailSummary, DeregisterNameSummary, DeregisterNumberSummary, DeregisterReasonSummary, DeregisterRoleSummary, EclReferenceSummary}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.all.FluentSummaryList
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregistrationPdfView
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist.SummaryListViewModel

import java.util.Base64

trait DeregisterPdfEncoder {

  def organisation(eclReference: Option[String], companyName: Option[String])(implicit
    messages: Messages
  ): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        EclReferenceSummary.row(eclReference),
        CompanyNameSummary.row(companyName)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def additionalInfo(deregistration: Deregistration)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        DeregisterReasonSummary.row(deregistration.reason),
        DeregisterDateSummary.row(deregistration.date)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def contact(contactDetails: ContactDetails)(implicit messages: Messages): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        DeregisterNameSummary.row(contactDetails.name),
        DeregisterRoleSummary.row(contactDetails.role),
        DeregisterEmailSummary.row(contactDetails.emailAddress),
        DeregisterNumberSummary.row(contactDetails.telephoneNumber)
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  def createPdfView(
    subscription: GetSubscriptionResponse,
    pdfView: DeregistrationPdfView,
    eclRegistrationReference: Option[String],
    deregistration: Deregistration
  )(implicit messages: Messages) =
    pdfView(
      organisation(
        eclRegistrationReference,
        subscription.legalEntityDetails.organisationName
      ),
      additionalInfo(deregistration),
      contact(deregistration.contactDetails)
    )

  def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

}
