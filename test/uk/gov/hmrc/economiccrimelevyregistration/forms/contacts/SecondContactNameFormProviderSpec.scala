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

package uk.gov.hmrc.economiccrimelevyregistration.forms.contacts

import play.api.data.FormError
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.StringFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.nameMaxLength

class SecondContactNameFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "secondContactName.error.required"
  val lengthKey   = "secondContactName.error.length"

  val form = new SecondContactNameFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(nameMaxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = nameMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(nameMaxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like stripValue(
      form,
      fieldName,
      stringsWithMaxLength(nameMaxLength)
    )
  }
}
