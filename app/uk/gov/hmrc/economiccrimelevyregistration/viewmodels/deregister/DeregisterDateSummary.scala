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

package uk.gov.hmrc.economiccrimelevyregistration.viewmodels.deregister

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.economiccrimelevyregistration.models.CheckMode
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.DeregisterReason
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

import java.time.LocalDate

object DeregisterDateSummary {

  def row(date: Option[LocalDate])(implicit messages: Messages): Option[SummaryListRow] =
    date.map { answer =>
      val value = ValueViewModel(HtmlContent(HtmlFormat.escape(ViewUtils.formatLocalDate(answer))))

      SummaryListRowViewModel(
        key = Key("deregisterCys.t2.l2"),
        value = value,
        actions = Seq(
          ActionItemViewModel(
            "site.change",
            uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterDateController
              .onPageLoad(CheckMode)
              .url
          )
            .withVisuallyHiddenText(
              messages("deregisterCys.t2.l2")
            )
        )
      )
    }

}
