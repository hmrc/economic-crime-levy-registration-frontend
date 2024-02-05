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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EntityType}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{GeneralPartnership, ScottishPartnership}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{ActionItem, Key, SummaryListRow}

object EntityNameSummary {

  def row(entityName: Option[String], entityType: Option[EntityType])(implicit
    messages: Messages
  ): Option[SummaryListRow] =
    entityName.map { answer =>
      val value = ValueViewModel(HtmlContent(HtmlFormat.escape(messages(answer))))

      val changeAction: Seq[ActionItem] = entityType match {
        case Some(GeneralPartnership | ScottishPartnership) =>
          Seq(
            ActionItemViewModel("site.change", routes.PartnershipNameController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(
                messages("checkYourAnswers.entityName.label")
              )
          )
        case _                                              => Seq.empty
      }

      SummaryListRowViewModel(
        key = Key("checkYourAnswers.entityName.label"),
        value = value,
        actions = changeAction
      )
    }

}
