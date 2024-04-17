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

case class EclTaxYear(startYear: Int) {

  private val dayDue: Int   = 30
  private val monthDue: Int = 9

  lazy val finishYear: Int = startYear + 1

  lazy val dateDue: LocalDate = LocalDate.of(finishYear, monthDue, dayDue)

  lazy val previous: EclTaxYear = EclTaxYear(startYear - 1)

  lazy val startDate: LocalDate  = LocalDate.of(startYear, EclTaxYear.eclStartMonth, EclTaxYear.eclStartDay)
  lazy val finishDate: LocalDate = LocalDate.of(finishYear, EclTaxYear.eclFinishMonth, EclTaxYear.eclFinishDay)

  def isBetweenStartDateAndDateDue(date: LocalDate): Boolean = {
    val startDateMinusOne = startDate.minusDays(1)
    val dateDuePlusOne    = dateDue.plusDays(1)
    date.isAfter(startDateMinusOne) && date.isBefore(dateDuePlusOne)
  }

}

object EclTaxYear {
  private val postEclDateDueStartDay   = 1
  private val postEclDateDueStartMonth = 10

  val eclFinishDay: Int   = 31
  val eclFinishMonth: Int = 3
  val eclInitialYear: Int = 2022
  val eclStartMonth: Int  = 4
  val eclStartDay: Int    = 1
  val yearInDays: Int     = 365

  private def calculateTaxYear(date: LocalDate, startMonth: Int, startDay: Int): EclTaxYear = {
    val startOfTaxYear = MonthDay.of(startMonth, startDay)
    if (date isBefore startOfTaxYear.atYear(date.getYear)) {
      EclTaxYear(startYear = date.getYear - 1)
    } else {
      EclTaxYear(startYear = date.getYear)
    }
  }

  def fromCurrentDate(currentDate: LocalDate = LocalDate.now()): EclTaxYear = {
    val startMonth: Int = postEclDateDueStartMonth
    val startDay: Int   = postEclDateDueStartDay
    calculateTaxYear(currentDate, startMonth, startDay)
  }

  def fromDate(date: LocalDate): EclTaxYear = {
    val startMonth: Int = eclStartMonth
    val startDay: Int   = eclStartDay
    calculateTaxYear(date, startMonth, startDay)
  }

  def fromStartYear(startYear: Int): EclTaxYear = EclTaxYear(startYear)
}
