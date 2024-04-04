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

import java.time.LocalDate
import scala.annotation.tailrec

case class EclTaxYear(startYear: Int) {

  lazy val finishYear: Int = startYear + 1

  def back(years: Int): EclTaxYear = EclTaxYear(startYear - years)

  lazy val previous: EclTaxYear = back(1)

  lazy val currentYear: Int = startYear

  override def toString = s"$startYear to $finishYear"
}

object EclTaxYear extends EclCurrentTaxYear with (Int => EclTaxYear) {

  private val dayDue           = 30
  private val finishDay: Int   = 31
  private val finishMonth: Int = 3
  private val monthDue         = 9

  val startMonth: Int  = 4
  val startDay: Int    = 1
  val initialYear: Int = 2022
  val yearInDays: Int  = 365

  override def now: () => LocalDate = () => LocalDate.now(ukTime)

  @tailrec
  def calculateYearDue(yearDue: Int = currentStartYear(), currentDate: LocalDate = LocalDate.now()): Int =
    if (currentDate.isAfter(LocalDate.of(yearDue, monthDue, dayDue))) {
      calculateYearDue(yearDue + 1, currentDate)
    } else {
      yearDue
    }

  def currentFinishYear(): Int = EclTaxYear.current.finishYear

  def currentStartYear(): Int = EclTaxYear.current.startYear

  def currentDueDateAdjustedStartYear(): Int =
    if (now().isBefore(LocalDate.of(currentStartYear(), monthDue, dayDue).plusDays(1))) {
      currentStartYear() - 1
    } else {
      currentStartYear()
    }

  def currentDueDateAdjustedFinishYear(): Int =
    if (now().isBefore(LocalDate.of(currentStartYear(), monthDue, dayDue).plusDays(1))) {
      currentFinishYear() - 1
    } else {
      currentFinishYear()
    }

  def currentDueDateAdjustedFinancialYearFinishDate: LocalDate =
    LocalDate.of(currentDueDateAdjustedFinishYear(), finishMonth, finishDay)

  def currentDueDateAdjustedFinancialYearStartDate: LocalDate =
    LocalDate.of(currentDueDateAdjustedStartYear(), startMonth, startDay)

  def currentFinancialYearFinishDate: LocalDate =
    LocalDate.of(currentFinishYear(), finishMonth, finishDay)

  def currentFinancialYearStartDate: LocalDate =
    LocalDate.of(currentStartYear(), startMonth, startDay)

  def currentFyFinishYear: Int = currentFinancialYearFinishDate.getYear

  def currentFyStartYear: Int = currentFinancialYearStartDate.getYear

  def currentDueDateAdjustedFyFinishYear: Int = currentDueDateAdjustedFinancialYearFinishDate.getYear

  def currentDueDateAdjustedFyStartYear: Int = currentDueDateAdjustedFinancialYearStartDate.getYear

  def dueDate: LocalDate = LocalDate.of(calculateYearDue(), monthDue, dayDue)

  def yearDue: Int = calculateYearDue()
}
