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
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.DateBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class AmlRegulatedActivityStartDateFormProviderSpec extends DateBehaviours {
  val form = new AmlRegulatedActivityStartDateFormProvider()()

  "value" should {
    val fieldName   = "value"
    val requiredKey = "error.date.required"

    behave like dateField(
      form,
      fieldName,
      datesBetween(EclTaxYear.currentFinancialYearStartDate, EclTaxYear.currentFinancialYearEndDate)
    )

    behave like mandatoryDateField(
      form,
      s"$fieldName.day",
      requiredKey
    )

    behave like dateFieldWithMin(
      form,
      fieldName,
      EclTaxYear.currentFinancialYearStartDate,
      FormError(
        s"$fieldName.day",
        "amlStartDate.error.notWithinFinancialYear",
        Seq(
          EclTaxYear.currentFinancialYearStartDate.getYear.toString,
          EclTaxYear.currentFinancialYearEndDate.getYear.toString
        )
      )
    )

    behave like dateFieldWithMax(
      form,
      fieldName,
      EclTaxYear.currentFinancialYearEndDate,
      FormError(
        s"$fieldName.day",
        "amlStartDate.error.notWithinFinancialYear",
        Seq(
          EclTaxYear.currentFinancialYearStartDate.getYear.toString,
          EclTaxYear.currentFinancialYearEndDate.getYear.toString
        )
      )
    )
  }
}
