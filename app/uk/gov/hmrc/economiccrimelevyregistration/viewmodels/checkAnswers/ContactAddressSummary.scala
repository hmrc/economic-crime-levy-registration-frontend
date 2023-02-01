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
import uk.gov.hmrc.economiccrimelevyregistration.models.CheckMode
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.AddressViewModel
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.implicits._
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryListRow}

object ContactAddressSummary {

  def row()(implicit messages: Messages, request: RegistrationDataRequest[_]): Option[SummaryListRow] =
    request.registration.contactAddress.map { answer =>
      SummaryListRowViewModel(
        key = Key("checkYourAnswers.address.label"),
        value = ValueViewModel(AddressViewModel.htmlContent(answer)),
        actions = request.registration.useRegisteredOfficeAddressAsContactAddress match {
          case Some(true)  => Seq.empty
          case Some(false) =>
            Seq(
              ActionItemViewModel("site.change", routes.IsUkAddressController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(
                  messages("checkYourAnswers.address.label")
                )
            )
          case _           => throw new IllegalStateException("Use registered office address as contact address answer not found")
        }
      )
    }

}
