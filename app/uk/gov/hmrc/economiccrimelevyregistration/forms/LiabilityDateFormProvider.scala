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

package uk.gov.hmrc.economiccrimelevyregistration.forms

import play.api.data.Form
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.{maximumDate, minimumDate}

import java.time.LocalDate

class LiabilityDateFormProvider extends Mappings {

  def apply(): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey = "invalidKey",
        requiredKey = "liability.date.error.required",
        minDateConstraint = Some(minDate(minimumDate, "liability.date.error.early.date")),
        maxDateConstraint = Some(maxDate(maximumDate, "liability.date.error.future.date"))
      )
    )
}