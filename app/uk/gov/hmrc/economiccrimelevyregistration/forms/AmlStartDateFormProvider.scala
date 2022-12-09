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
import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

class AmlStartDateFormProvider extends Mappings {
  val dateIsWithinCurrentFinancialYear: Constraint[LocalDate] = Constraint[LocalDate] { date: LocalDate =>
    if (
      date.isBefore(EclTaxYear.currentFinancialYearStartDate) || date.isAfter(EclTaxYear.currentFinancialYearEndDate)
    ) {
      Invalid("amlStartDate.error.notWithinFinancialYear")
    } else {
      Valid
    }
  }

  def apply(): Form[LocalDate] = Form(
    (
      "value",
      localDate(
        "amlStartDate.error.invalid",
        "amlStartDate.error.allRequired",
        "amlStartDate.error.twoRequired",
        "amlStartDate.error.required"
      ).verifying(dateIsWithinCurrentFinancialYear)
    )
  )
}
