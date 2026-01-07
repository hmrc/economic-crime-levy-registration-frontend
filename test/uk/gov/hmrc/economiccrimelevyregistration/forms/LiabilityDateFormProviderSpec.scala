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

import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import play.api.data.FormError
import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.DateBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MinMaxValues
import uk.gov.hmrc.economiccrimelevyregistration.services.LocalDateService
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

class LiabilityDateFormProviderSpec extends DateBehaviours with MockitoSugar {

  val testCurrentDate: LocalDate = LocalDate.of(2024, 10, 1)

  val mockLocalDateService: LocalDateService = mock[LocalDateService]

  when(mockLocalDateService.now()).thenReturn(testCurrentDate)

  val form = new LiabilityDateFormProvider()(mockLocalDateService)

  "value" should {
    val fieldName   = "value"
    val futureKey   = "liability.date.error.future.date"
    val earlyKey    = "liability.date.error.early.date"
    val requiredKey = "liability.date.error.required"

    behave like dateField(
      form,
      fieldName,
      datesBetween(
        MinMaxValues.eclStartDate,
        EclTaxYear.fromDate(testCurrentDate).startDate.minusDays(1)
      )
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
        FormError(s"$fieldName.day", earlyKey),
        FormError(s"$fieldName.month", earlyKey),
        FormError(s"$fieldName.year", earlyKey)
      )
    )

    behave like dateFieldWithMax(
      form,
      fieldName,
      EclTaxYear.fromDate(testCurrentDate).startDate.minusDays(1),
      Seq(
        FormError(s"$fieldName.day", futureKey),
        FormError(s"$fieldName.month", futureKey),
        FormError(s"$fieldName.year", futureKey)
      )
    )
  }
}
