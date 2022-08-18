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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers

import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Registration}
import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._

object ExampleCheckBoxSummary {

  def row(registration: Registration)(implicit messages: Messages): Option[SummaryListRow] =
    registration.exampleCheckBox.map { answers =>
      val value = ValueViewModel(
        HtmlContent(
          answers
            .map { answer =>
              HtmlFormat.escape(messages(s"exampleCheckBox.$answer")).toString
            }
            .mkString(",<br>")
        )
      )

      SummaryListRowViewModel(
        key = "exampleCheckBox.checkYourAnswersLabel",
        value = value,
        actions = Seq(
          ActionItemViewModel("site.change", routes.ExampleCheckBoxController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("exampleCheckBox.change.hidden"))
        )
      )
    }
}
