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

import play.api.data.FormError
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.DateBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MinMaxValues

import java.time.LocalDate

class DeregisterDateFormProviderSpec extends DateBehaviours {
  val form = new DeregisterDateFormProvider()()

  "value" should {
    val fieldName   = "value"
    val requiredKey = "deregisterDate.error.required"
    val pastKey     = "deregisterDate.error.past"
    val futureKey   = "deregisterDate.error.future"

    behave like dateField(
      form,
      fieldName,
      datesBetween(MinMaxValues.eclStartDate, LocalDate.now())
    )

    behave like mandatoryDateField(
      form,
      fieldName,
      requiredKey
    )

    behave like dateFieldWithMin(
      form,
      fieldName,
      MinMaxValues.eclStartDate,
      Seq(
        FormError(s"$fieldName.day", pastKey),
        FormError(s"$fieldName.month", pastKey),
        FormError(s"$fieldName.year", pastKey)
      )
    )

    behave like dateFieldWithMax(
      form,
      fieldName,
      LocalDate.now(),
      Seq(
        FormError(s"$fieldName.day", futureKey),
        FormError(s"$fieldName.month", futureKey),
        FormError(s"$fieldName.year", futureKey)
      )
    )
  }
}
