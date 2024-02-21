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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EntityType}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

object DoYouHaveCtUtrSummary {

  def row(
    isCtUtrPresent: Option[Boolean],
    entityType: Option[EntityType]
  )(implicit messages: Messages): Option[SummaryListRow] =
    isCtUtrPresent.map { answer =>
      val value = if (answer) "site.yes" else "site.no"
      val key   = entityType match {
        case Some(Charity) => "otherEntityType.utr.question.label"
        case _             => "otherEntityType.ctutr.question.label"
      }
      val url   = entityType match {
        case Some(Charity) => routes.DoYouHaveUtrController.onPageLoad(CheckMode).url
        case _             => routes.DoYouHaveCtUtrController.onPageLoad(CheckMode).url
      }

      SummaryListRowViewModel(
        key = Key(key),
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", url)
            .withVisuallyHiddenText(
              messages(key)
            )
        )
      )
    }
}
