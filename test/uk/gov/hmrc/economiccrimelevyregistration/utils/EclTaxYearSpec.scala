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

import org.scalatest.prop.{TableFor3, TableFor6}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

import java.time.LocalDate

class EclTaxYearSpec extends SpecBase {

  private val dueDay                    = 30
  private val dueDayPlusOne             = 1
  private val dueMonth                  = 9
  private val dueMonthPlusOne           = 10
  private val taxYearStartMonth         = 4
  private val taxYearStartMonthMinusOne = 3
  private val taxYearStartDay           = 1
  private val taxYearStartDayMinusOne   = 31

  val fromCurrentDateTestParameters: TableFor6[LocalDate, Int, Int, LocalDate, LocalDate, LocalDate] = Table(
    (
      "currentDate",
      "expectedStartYear",
      "expectedFinishYear",
      "expectedDateDue",
      "expectedStartDate",
      "expectedFinishDate"
    ),
    (
      LocalDate.of(2023, dueMonth, dueDay),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonth, dueDay),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2025, dueMonth, dueDay),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2026, dueMonth, dueDay),
      2025,
      2026,
      LocalDate.of(2026, dueMonth, dueDay),
      LocalDate.of(2025, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2026, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2027, dueMonth, dueDay),
      2026,
      2027,
      LocalDate.of(2027, dueMonth, dueDay),
      LocalDate.of(2026, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2027, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2025, dueMonthPlusOne, dueDayPlusOne),
      2025,
      2026,
      LocalDate.of(2026, dueMonth, dueDay),
      LocalDate.of(2025, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2026, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2026, dueMonthPlusOne, dueDayPlusOne),
      2026,
      2027,
      LocalDate.of(2027, dueMonth, dueDay),
      LocalDate.of(2026, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2027, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2027, dueMonthPlusOne, dueDayPlusOne),
      2027,
      2028,
      LocalDate.of(2028, dueMonth, dueDay),
      LocalDate.of(2027, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2028, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2028, dueMonthPlusOne, dueDayPlusOne),
      2028,
      2029,
      LocalDate.of(2029, dueMonth, dueDay),
      LocalDate.of(2028, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2029, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    )
  )

  val previousFromCurrentDateTestParameters: TableFor6[LocalDate, Int, Int, LocalDate, LocalDate, LocalDate] = Table(
    (
      "currentDate",
      "expectedPreviousStartYear",
      "expectedPreviousFinishYear",
      "expectedPreviousDateDue",
      "expectedStartDate",
      "expectedFinishDate"
    ),
    (
      LocalDate.of(2023, dueMonth, dueDay),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonth, dueDay),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2025, dueMonth, dueDay),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2026, dueMonth, dueDay),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2027, dueMonth, dueDay),
      2025,
      2026,
      LocalDate.of(2026, dueMonth, dueDay),
      LocalDate.of(2025, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2026, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2025, dueMonthPlusOne, dueDayPlusOne),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2026, dueMonthPlusOne, dueDayPlusOne),
      2025,
      2026,
      LocalDate.of(2026, dueMonth, dueDay),
      LocalDate.of(2025, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2026, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2027, dueMonthPlusOne, dueDayPlusOne),
      2026,
      2027,
      LocalDate.of(2027, dueMonth, dueDay),
      LocalDate.of(2026, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2027, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2028, dueMonthPlusOne, dueDayPlusOne),
      2027,
      2028,
      LocalDate.of(2028, dueMonth, dueDay),
      LocalDate.of(2027, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2028, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    )
  )

  "fromCurrentDate" should {
    "return expected start and finish year and dateDue" in forAll(fromCurrentDateTestParameters) {
      (
        currentDate: LocalDate,
        expectedStartYear: Int,
        expectedFinishYear: Int,
        expectedDateDue: LocalDate,
        expectedStartDate: LocalDate,
        expectedFinishDate: LocalDate
      ) =>
        val eclTaxYear: EclTaxYear = EclTaxYear.fromCurrentDate(currentDate)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
        expectedStartDate  shouldBe eclTaxYear.startDate
        expectedFinishDate shouldBe eclTaxYear.finishDate
    }

    "return expected previous start and finish year and dateDue" in forAll(previousFromCurrentDateTestParameters) {
      (
        currentDate: LocalDate,
        expectedPreviousStartYear: Int,
        expectedPreviousFinishYear: Int,
        expectedPreviousDateDue: LocalDate,
        expectedStartDate: LocalDate,
        expectedFinishDate: LocalDate
      ) =>
        val eclTaxYear: EclTaxYear = EclTaxYear.fromCurrentDate(currentDate).previous

        expectedPreviousStartYear  shouldBe eclTaxYear.startYear
        expectedPreviousFinishYear shouldBe eclTaxYear.finishYear
        expectedPreviousDateDue    shouldBe eclTaxYear.dateDue
        expectedStartDate          shouldBe eclTaxYear.startDate
        expectedFinishDate         shouldBe eclTaxYear.finishDate
    }
  }

  val fromDateTestParameters: TableFor6[LocalDate, Int, Int, LocalDate, LocalDate, LocalDate] = Table(
    ("date", "expectedStartYear", "expectedFinishYear", "expectedDueDate", "expectedStartDate", "expectedFinishDate"),
    (
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2022, dueMonth, dueDay),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2022, dueMonthPlusOne, dueDayPlusOne),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, dueMonth, dueDay),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, dueMonthPlusOne, dueDayPlusOne),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonth, dueDay),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne),
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    )
  )

  val previousFromDateTestParameters: TableFor6[LocalDate, Int, Int, LocalDate, LocalDate, LocalDate] = Table(
    ("date", "expectedStartYear", "expectedFinishYear", "expectedDueDate", "expectedStartDate", "expectedFinishDate"),
    (
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2020,
      2021,
      LocalDate.of(2021, dueMonth, dueDay),
      LocalDate.of(2020, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2021, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2022, dueMonth, dueDay),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2022, dueMonthPlusOne, dueDayPlusOne),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, dueMonth, dueDay),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2023, dueMonthPlusOne, dueDayPlusOne),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonth, dueDay),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    )
  )

  "fromDate" should {
    "return expected start and finish year and dateDue" in forAll(fromDateTestParameters) {
      (
        date: LocalDate,
        expectedStartYear: Int,
        expectedFinishYear: Int,
        expectedDateDue: LocalDate,
        expectedStartDate: LocalDate,
        expectedFinishDate: LocalDate
      ) =>
        val eclTaxYear: EclTaxYear = EclTaxYear.fromDate(date)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
        expectedStartDate  shouldBe eclTaxYear.startDate
        expectedFinishDate shouldBe eclTaxYear.finishDate
    }

    "return expected previous start and finish year and dateDue" in forAll(previousFromDateTestParameters) {
      (
        date: LocalDate,
        expectedStartYear: Int,
        expectedFinishYear: Int,
        expectedDateDue: LocalDate,
        expectedStartDate: LocalDate,
        expectedFinishDate: LocalDate
      ) =>
        val eclTaxYear: EclTaxYear = EclTaxYear.fromDate(date).previous

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
        expectedStartDate  shouldBe eclTaxYear.startDate
        expectedFinishDate shouldBe eclTaxYear.finishDate
    }
  }

  val fromStartYearTestParameters: TableFor6[Int, Int, Int, LocalDate, LocalDate, LocalDate] = Table(
    (
      "startYear",
      "expectedStartYear",
      "expectedFinishYear",
      "expectedDateDue",
      "expectedStartDate",
      "expectedFinishDate"
    ),
    (
      2022,
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2023,
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2024,
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2025,
      2025,
      2026,
      LocalDate.of(2026, dueMonth, dueDay),
      LocalDate.of(2025, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2026, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2026,
      2026,
      2027,
      LocalDate.of(2027, dueMonth, dueDay),
      LocalDate.of(2026, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2027, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2027,
      2027,
      2028,
      LocalDate.of(2028, dueMonth, dueDay),
      LocalDate.of(2027, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2028, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2028,
      2028,
      2029,
      LocalDate.of(2029, dueMonth, dueDay),
      LocalDate.of(2028, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2029, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2029,
      2029,
      2030,
      LocalDate.of(2030, dueMonth, dueDay),
      LocalDate.of(2029, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2030, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2030,
      2030,
      2031,
      LocalDate.of(2031, dueMonth, dueDay),
      LocalDate.of(2030, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2031, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2031,
      2031,
      2032,
      LocalDate.of(2032, dueMonth, dueDay),
      LocalDate.of(2031, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2032, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    )
  )

  val fromPreviousStartYearTestParameters: TableFor6[Int, Int, Int, LocalDate, LocalDate, LocalDate] = Table(
    (
      "startYear",
      "expectedStartYear",
      "expectedFinishYear",
      "expectedDateDue",
      "expectedStartDate",
      "expectedFinishDate"
    ),
    (
      2022,
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay),
      LocalDate.of(2021, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2023,
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay),
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2024,
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay),
      LocalDate.of(2023, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2025,
      2024,
      2025,
      LocalDate.of(2025, dueMonth, dueDay),
      LocalDate.of(2024, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2025, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2026,
      2025,
      2026,
      LocalDate.of(2026, dueMonth, dueDay),
      LocalDate.of(2025, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2026, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2027,
      2026,
      2027,
      LocalDate.of(2027, dueMonth, dueDay),
      LocalDate.of(2026, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2027, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2028,
      2027,
      2028,
      LocalDate.of(2028, dueMonth, dueDay),
      LocalDate.of(2027, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2028, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2029,
      2028,
      2029,
      LocalDate.of(2029, dueMonth, dueDay),
      LocalDate.of(2028, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2029, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2030,
      2029,
      2030,
      LocalDate.of(2030, dueMonth, dueDay),
      LocalDate.of(2029, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2030, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    ),
    (
      2031,
      2030,
      2031,
      LocalDate.of(2031, dueMonth, dueDay),
      LocalDate.of(2030, taxYearStartMonth, taxYearStartDay),
      LocalDate.of(2031, taxYearStartMonthMinusOne, taxYearStartDayMinusOne)
    )
  )

  "fromStartYear" should {
    "return expected start and finish year and dateDue" in forAll(fromStartYearTestParameters) {
      (
        startYear: Int,
        expectedStartYear: Int,
        expectedFinishYear: Int,
        expectedDateDue: LocalDate,
        expectedStartDate: LocalDate,
        expectedFinishDate: LocalDate
      ) =>
        val eclTaxYear: EclTaxYear = EclTaxYear.fromStartYear(startYear)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
        expectedStartDate  shouldBe eclTaxYear.startDate
        expectedFinishDate shouldBe eclTaxYear.finishDate
    }

    "return expected previous start and finish year and dateDue" in forAll(fromPreviousStartYearTestParameters) {
      (
        startYear: Int,
        expectedStartYear: Int,
        expectedFinishYear: Int,
        expectedDateDue: LocalDate,
        expectedStartDate: LocalDate,
        expectedFinishDate: LocalDate
      ) =>
        val eclTaxYear: EclTaxYear = EclTaxYear.fromStartYear(startYear).previous

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
        expectedStartDate  shouldBe eclTaxYear.startDate
        expectedFinishDate shouldBe eclTaxYear.finishDate
    }
  }

  val isBetweenStartDateAndDateDueParameters: TableFor3[LocalDate, LocalDate, Boolean] = Table(
    ("fromDate", "testDate", "isBetweenStartDateAndDateDue"),
    (LocalDate.of(2024, 4, 17), LocalDate.of(2024, 4, 1), true),
    (LocalDate.of(2024, 10, 1), LocalDate.of(2024, 1, 1), false),
    (LocalDate.of(2024, 10, 1), LocalDate.of(2023, 1, 1), false),
    (LocalDate.of(2024, 10, 1), LocalDate.of(2024, 10, 1), false)
  )

  "isBetweenStartDateAndDateDue" should {
    "return whether the test date is between current tax year start date and date due" in forAll(
      isBetweenStartDateAndDateDueParameters
    ) {
      (
        fromDate: LocalDate,
        testDate: LocalDate,
        expectedResult: Boolean
      ) =>
        val eclTaxYear = EclTaxYear.fromDate(fromDate)
        val result     = eclTaxYear.isBetweenStartDateAndPreviousDateDue(testDate)
        result shouldEqual expectedResult
    }
  }
}
