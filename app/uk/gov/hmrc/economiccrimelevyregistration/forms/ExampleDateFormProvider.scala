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

package uk.gov.hmrc.economiccrimelevyregistration.forms

import java.time.LocalDate
import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import play.api.data.Form

class ExampleDateFormProvider @Inject() extends Mappings {

  def apply(): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey = "exampleDate.error.invalid",
        allRequiredKey = "exampleDate.error.required.all",
        twoRequiredKey = "exampleDate.error.required.two",
        requiredKey = "exampleDate.error.required"
      )
    )
}
