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

import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.CheckboxFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.models.ExampleCheckBox
import play.api.data.FormError

class ExampleCheckBoxFormProviderSpec extends CheckboxFieldBehaviours {

  val form = new ExampleCheckBoxFormProvider()()

  "value" should {

    val fieldName   = "value"
    val requiredKey = "exampleCheckBox.error.required"

    behave like checkboxField[ExampleCheckBox](
      form,
      fieldName,
      validValues = ExampleCheckBox.values,
      invalidError = FormError(s"$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
