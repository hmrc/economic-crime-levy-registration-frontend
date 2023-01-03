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
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.BooleanFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class UkRevenueFormProviderSpec extends BooleanFieldBehaviours {
  val form = new UkRevenueFormProvider()()

  "value" should {
    val fieldName   = "value"
    val requiredKey = "ukRevenue.error.required"

    behave like booleanField(
      form,
      fieldName,
      FormError(fieldName, "error.boolean", args = Seq(EclTaxYear.currentFinancialYear))
    )

    behave like mandatoryField(
      form,
      fieldName,
      FormError(fieldName, requiredKey, args = Seq(EclTaxYear.currentFinancialYear))
    )
  }
}
