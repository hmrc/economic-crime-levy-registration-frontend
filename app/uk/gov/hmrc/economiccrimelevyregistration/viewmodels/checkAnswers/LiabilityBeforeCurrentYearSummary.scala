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

import play.api.i18n.Messages
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.CheckMode
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

object LiabilityBeforeCurrentYearSummary {

  def row(
    registeringForCurrentYear: Option[Boolean]
  )(implicit messages: Messages): Option[SummaryListRow] =
    registeringForCurrentYear.map { answer =>
      val value = if (answer) "site.yes" else "site.no"

      SummaryListRowViewModel(
        key = Key("liabilityBeforeCurrentYear.label"),
        value = ValueViewModel(value),
        actions = Seq(
          ActionItemViewModel("site.change", routes.LiabilityBeforeCurrentYearController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(
              messages("liabilityBeforeCurrentYear.label")
            )
        )
      )
    }
}
