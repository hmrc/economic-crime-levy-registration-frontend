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

package uk.gov.hmrc.economiccrimelevyregistration.forms

import play.api.data.FormError
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.StringFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.charityRegistrationNumberMaxLength

class CharityRegistrationNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "charityRegistrationNumber.error.required"
  val lengthKey   = "charityRegistrationNumber.error.length"

  val form = new CharityRegistrationNumberFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(charityRegistrationNumberMaxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = charityRegistrationNumberMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(charityRegistrationNumberMaxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like removeAllWhitespace(
      form,
      fieldName,
      stringsWithMaxLength(charityRegistrationNumberMaxLength)
    )
  }
}
