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

import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import play.api.data.Form

class ExampleNumberFormProvider @Inject() extends Mappings {

  def apply(): Form[Int] =
    Form(
      "value" -> int(
        "exampleNumber.error.required",
        "exampleNumber.error.wholeNumber",
        "exampleNumber.error.nonNumeric"
      )
        .verifying(inRange(1, 999, "exampleNumber.error.outOfRange"))
    )
}
