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

package uk.gov.hmrc.economiccrimelevyregistration.forms.deregister

import play.api.data.{Form, FormError}
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.StringFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.TelephoneNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Regex

class DeregisterContactNumberFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "deregisterContactNumber.error.required"
  val lengthKey   = "deregisterContactNumber.error.length"

  val form = new DeregisterContactNumberFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      telephoneNumber(TelephoneNumberMaxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = TelephoneNumberMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(TelephoneNumberMaxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    behave like removeAllWhitespace(
      form,
      fieldName,
      telephoneNumber(TelephoneNumberMaxLength)
    )

    "fail to bind an invalid telephone number" in forAll(
      stringsWithMaxLength(TelephoneNumberMaxLength).retryUntil(!_.matches(Regex.TelephoneNumberRegex))
    ) { invalidNumber: String =>
      val result: Form[String] = form.bind(Map("value" -> invalidNumber))

      result.errors.map(_.message) should contain only "deregisterContactNumber.error.invalid"
    }
  }
}
