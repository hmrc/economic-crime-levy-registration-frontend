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

import org.scalacheck.{Arbitrary, Gen}
import play.api.data.{Form, FormError}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.OptionFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.models._

class AmlSupervisorFormProviderSpec extends OptionFieldBehaviours with SpecBase {
  val form: Form[AmlSupervisor] = new AmlSupervisorFormProvider()(appConfig)

  "value" should {
    val fieldName   = "value"
    val requiredKey = "amlSupervisor.error.required"

    behave like optionsField[AmlSupervisorType](
      form,
      fieldName,
      AmlSupervisorType.values,
      FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(form, fieldName, FormError(fieldName, requiredKey))
  }

  "otherProfessionalBody" should {
    "produce a form error if no professional body is provided when the Other option is selected" in {
      val result: Form[AmlSupervisor] = form.bind(Map("value" -> Other.toString, "otherProfessionalBody" -> ""))

      result.errors.map(_.key) should contain("otherProfessionalBody")
    }

    "always be None if any option other than Other is selected" in forAll(
      Gen.oneOf(appConfig.amlProfessionalBodySupervisors),
      Gen.oneOf(Hmrc, GamblingCommission, FinancialConductAuthority)
    ) { (otherProfessionalBody, supervisor) =>
      val result: Form[AmlSupervisor] =
        form.bind(Map("value" -> supervisor.toString, "otherProfessionalBody" -> otherProfessionalBody))

      result.value shouldBe Some(AmlSupervisor(supervisor, None))
    }

    "produce a form error if an option is provided that is not in the list of professional bodies" in forAll(
      Arbitrary.arbitrary[String].retryUntil(s => !appConfig.amlProfessionalBodySupervisors.contains(s))
    ) { invalidProfessionalBody =>
      val result: Form[AmlSupervisor] =
        form.bind(Map("value" -> Other.toString, "otherProfessionalBody" -> invalidProfessionalBody))

      result.errors.map(_.key) should contain("otherProfessionalBody")
    }

    "be the selected professional body when the Other option is selected" in forAll(
      Gen.oneOf(appConfig.amlProfessionalBodySupervisors)
    ) { otherProfessionalBody =>
      val result: Form[AmlSupervisor] =
        form.bind(Map("value" -> Other.toString, "otherProfessionalBody" -> otherProfessionalBody))

      result.value shouldBe Some(AmlSupervisor(Other, Some(otherProfessionalBody)))
    }
  }
}
