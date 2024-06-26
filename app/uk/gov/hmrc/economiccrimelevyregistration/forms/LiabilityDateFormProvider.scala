/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.{Mappings, MinMaxValues}
import uk.gov.hmrc.economiccrimelevyregistration.services.LocalDateService

import java.time.LocalDate

class LiabilityDateFormProvider extends Mappings {

  def removeSpaces(value: Option[String]): Option[String] =
    if (value.isDefined) {
      Some(value.get.replaceAll(" ", ""))
    } else {
      None
    }

  def apply(localDateService: LocalDateService): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey = "liability.date.error.invalid",
        requiredKey = "liability.date.error.required",
        removeSpaces,
        minDateConstraint = Some(minDate(MinMaxValues.eclStartDate, "liability.date.error.early.date")),
        maxDateConstraint = Some(maxDate(localDateService.now(), "liability.date.error.future.date"))
      ).verifying(isBeforeCurrentTaxYearStart(localDateService, "liability.date.error.before"))
    )
}
