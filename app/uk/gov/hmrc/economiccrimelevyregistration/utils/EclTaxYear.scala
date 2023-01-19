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

package uk.gov.hmrc.economiccrimelevyregistration.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.annotation.tailrec

object EclTaxYear {

  private val startYear       = LocalDate.now().getYear
  private val monthDue        = 9
  private val dayDue          = 30
  private val eclFyEndMonth   = 3
  private val eclFyStartMonth = 4
  private val eclFyEndDay     = 31
  private val eclFyStartDay   = 1

  val dueDate: String                      =
    LocalDate.of(calculateYearDue(), monthDue, dayDue).format(DateTimeFormatter.ofPattern("dd MMMM yyyy"))
  val yearDue: String                      = calculateYearDue().toString
  private val currentFinancialYear: String = (yearDue.toInt - 1).toString
  val yearInDays                           = 365

  @tailrec
  def calculateYearDue(yearDue: Int = startYear, currentDate: LocalDate = LocalDate.now()): Int =
    if (currentDate.isAfter(LocalDate.of(yearDue, monthDue, dayDue))) {
      calculateYearDue(yearDue + 1, currentDate)
    } else {
      yearDue
    }

  private val currentFinancialYearStartDate: LocalDate =
    LocalDate.of(currentFinancialYear.toInt, eclFyStartMonth, eclFyStartDay)
  private val currentFinancialYearEndDate: LocalDate   =
    LocalDate.of(currentFinancialYear.toInt + 1, eclFyEndMonth, eclFyEndDay)

  val currentFyStartYear: String = currentFinancialYearStartDate.getYear.toString
  val currentFyEndYear: String   = currentFinancialYearEndDate.getYear.toString
}
