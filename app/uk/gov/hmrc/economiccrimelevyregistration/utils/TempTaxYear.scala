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

import java.time.{LocalDate, MonthDay}

case class TempTaxYear(startYear: Int) {

  private val dayDue: Int      = 30
  private val monthDue: Int    = 9

  val finishYear: Int = startYear + 1

  val dateDue: LocalDate = LocalDate.of(finishYear, monthDue, dayDue)
  
  val previous: TempTaxYear = TempTaxYear.fromStartYear(startYear - 1)
}

object TempTaxYear {
  private def calculateTaxYear(date: LocalDate, startMonth: Int, startDay: Int): TempTaxYear = {
    val startOfTaxYear = MonthDay.of(startMonth, startDay)
    if (date isBefore startOfTaxYear.atYear(date.getYear)) {
      TempTaxYear(startYear = date.getYear - 1)
    } else {
      TempTaxYear(startYear = date.getYear)
    }
  }

  def fromCurrentDate(currentDate: LocalDate = LocalDate.now()): TempTaxYear = {
    val startMonth: Int  = 10
    val startDay: Int    = 1
    calculateTaxYear(currentDate, startMonth, startDay)
  }

  def fromDate(date: LocalDate): TempTaxYear = {
    val startMonth: Int  = 4
    val startDay: Int    = 1
    calculateTaxYear(date, startMonth, startDay)
  }

  def fromStartYear(startYear: Int): TempTaxYear = TempTaxYear(startYear)
}
