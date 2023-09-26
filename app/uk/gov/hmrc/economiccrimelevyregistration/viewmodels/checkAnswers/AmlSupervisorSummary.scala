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
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models.CheckMode
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

object AmlSupervisorSummary {

  def row()(implicit messages: Messages, request: RegistrationDataRequest[_]): Option[SummaryListRow] =
    request.registration.amlSupervisor.flatMap { answer =>
      val supervisor: Option[String] = answer.supervisorType match {
        case Hmrc  => Some(Hmrc.toString)
        case Other => answer.otherProfessionalBody
        case _     => None
      }

      supervisor.map { s =>
        val value = ValueViewModel(HtmlContent(HtmlFormat.escape(messages(s"amlSupervisor.$s"))))

        SummaryListRowViewModel(
          key = Key("checkYourAnswers.amlSupervisor.label"),
          value = value,
          actions = Seq(
            ActionItemViewModel(
              "site.change",
              routes.AmlSupervisorController
                .onPageLoad(CheckMode, request.registration.registrationType.getOrElse(Some(Initial)))
                .url
            )
              .withVisuallyHiddenText(
                messages("checkYourAnswers.amlSupervisor.label")
              )
          )
        )
      }
    }

}
