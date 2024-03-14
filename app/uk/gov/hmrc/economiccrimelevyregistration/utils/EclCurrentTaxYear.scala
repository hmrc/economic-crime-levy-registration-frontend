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

package uk.gov.hmrc.economiccrimelevyregistration.utils

import java.time.{LocalDate, MonthDay, ZoneId}

trait EclCurrentTaxYear {

  private val startDay   = 1
  private val startMonth = 4

  final val ukTime: ZoneId = ZoneId.of("Europe/London")

  private val startOfTaxYear = MonthDay.of(startMonth, startDay)

  def now: () => LocalDate

  final def firstDayOfTaxYear(year: Int): LocalDate = startOfTaxYear.atYear(year)

  final def today: LocalDate = now()

  final def taxYearFor(date: LocalDate): EclTaxYear =
    if (date isBefore firstDayOfTaxYear(date.getYear)) {
      EclTaxYear(startYear = date.getYear - 1)
    } else {
      EclTaxYear(startYear = date.getYear)
    }

  final def current: EclTaxYear = taxYearFor(today)
}
