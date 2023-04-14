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

import org.mockito.MockitoSugar.{mock, when}
import play.api.data.FormError
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.StringFieldBehaviours

class PrivateBetaAccessFormProviderSpec extends StringFieldBehaviours {

  val requiredKey              = "privateBetaAccess.error.required"
  val invalidKey               = "privateBetaAccess.error.invalid"
  val mockAppConfig: AppConfig = mock[AppConfig]

  val form = new PrivateBetaAccessFormProvider(mockAppConfig)()

  "value" should {

    val fieldName = "value"

    "bind" should {
      "bind valid data" in {
        forAll(nonBlankString -> "validAccessCode") { accessCode: String =>
          when(mockAppConfig.privateBetaAccessCodes).thenReturn(Seq(accessCode))

          val result = form.bind(Map(fieldName -> accessCode)).apply(fieldName)
          result.value.value shouldBe accessCode
          result.errors      shouldBe empty
        }
      }

      "fail to bind when the access code does not match what is held in config" in {
        when(mockAppConfig.privateBetaAccessCodes).thenReturn(Seq("a-code"))

        val result = form.bind(Map(fieldName -> "a-different-code")).apply(fieldName)

        result.errors should contain(FormError(fieldName, "privateBetaAccess.error.invalid"))
      }
    }

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
