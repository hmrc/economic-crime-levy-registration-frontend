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

import play.api.data.Form
import play.api.data.validation.Constraint
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

class AmlRegulatedActivityStartDateFormProvider extends Mappings {

  val minDateConstraint: Constraint[LocalDate] = minDate(
    EclTaxYear.currentFinancialYearStartDate,
    "amlStartDate.error.notWithinFinancialYear",
    EclTaxYear.currentFinancialYearStartDate.getYear.toString,
    EclTaxYear.currentFinancialYearEndDate.getYear.toString
  )

  val maxDateConstraint: Constraint[LocalDate] = maxDate(
    EclTaxYear.currentFinancialYearEndDate,
    "amlStartDate.error.notWithinFinancialYear",
    EclTaxYear.currentFinancialYearStartDate.getYear.toString,
    EclTaxYear.currentFinancialYearEndDate.getYear.toString
  )

  def apply(): Form[LocalDate] = Form(
    (
      "value",
      localDate(
        "error.date.invalid",
        "error.date.required",
        Some(minDateConstraint),
        Some(maxDateConstraint)
      )
    )
  )
}
