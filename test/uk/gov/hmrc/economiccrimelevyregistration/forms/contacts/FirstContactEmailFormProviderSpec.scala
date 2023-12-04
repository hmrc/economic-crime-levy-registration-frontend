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

import play.api.data.{Form, FormError}
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.StringFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Regex

class FirstContactEmailFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "firstContactEmail.error.required"
  val lengthKey   = "firstContactEmail.error.length"

  val form = new FirstContactEmailFormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      emailAddress(EmailMaxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = EmailMaxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(EmailMaxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "remove all whitespace" in forAll(emailAddress(EmailMaxLength)) { dataItem: String =>
      val expectedValue  = dataItem.filterNot(_ == ',').toLowerCase
      val dataWithSpaces = "  " + dataItem.mkString(" ") + "  "
      val result         = form.bind(Map(fieldName -> dataWithSpaces))
      result.value.value shouldBe expectedValue
      result.errors      shouldBe empty
    }

    "fail to bind an invalid email address" in forAll(
      stringsWithMaxLength(EmailMaxLength).retryUntil(!_.matches(Regex.EmailRegex))
    ) { invalidEmail: String =>
      val result: Form[String] = form.bind(Map("value" -> invalidEmail))

      result.errors.map(_.message) should contain("firstContactEmail.error.invalid")
    }
  }
}
