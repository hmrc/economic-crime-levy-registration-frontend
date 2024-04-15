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

import org.scalatest.prop.TableFor4
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

import java.time.LocalDate

class TempTaxYearSpec extends SpecBase {

  private val dueDay                    = 30
  private val dueDayPlusOne             = 1
  private val dueMonth                  = 9
  private val dueMonthPlusOne           = 10
  private val taxYearStartMonth         = 4
  private val taxYearStartMonthMinusOne = 3
  private val taxYearStartDay           = 1
  private val taxYearStartDayMinusOne   = 3

  val fromCurrentDateTestParameters: TableFor4[LocalDate, Int, Int, LocalDate] = Table(
    ("currentDate", "expectedStartyear", "expectedFinishYear", "expectedDateDue"),
    (LocalDate.of(2023, dueMonth, dueDay), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonth, dueDay), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2025, dueMonth, dueDay), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (LocalDate.of(2026, dueMonth, dueDay), 2025, 2026, LocalDate.of(2026, dueMonth, dueDay)),
    (LocalDate.of(2027, dueMonth, dueDay), 2026, 2027, LocalDate.of(2027, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (LocalDate.of(2025, dueMonthPlusOne, dueDayPlusOne), 2025, 2026, LocalDate.of(2026, dueMonth, dueDay)),
    (LocalDate.of(2026, dueMonthPlusOne, dueDayPlusOne), 2026, 2027, LocalDate.of(2027, dueMonth, dueDay)),
    (LocalDate.of(2027, dueMonthPlusOne, dueDayPlusOne), 2027, 2028, LocalDate.of(2028, dueMonth, dueDay)),
    (LocalDate.of(2028, dueMonthPlusOne, dueDayPlusOne), 2028, 2029, LocalDate.of(2029, dueMonth, dueDay))
  )

  val previousFromCurrentDateTestParameters: TableFor4[LocalDate, Int, Int, LocalDate] = Table(
    ("currentDate", "expectedPreviousStartyear", "expectedPreviousFinishYear", "expectedPreviousDateDue"),
    (LocalDate.of(2023, dueMonth, dueDay), 2021, 2022, LocalDate.of(2022, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonth, dueDay), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (LocalDate.of(2025, dueMonth, dueDay), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2026, dueMonth, dueDay), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (LocalDate.of(2027, dueMonth, dueDay), 2025, 2026, LocalDate.of(2026, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2025, dueMonthPlusOne, dueDayPlusOne), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (LocalDate.of(2026, dueMonthPlusOne, dueDayPlusOne), 2025, 2026, LocalDate.of(2026, dueMonth, dueDay)),
    (LocalDate.of(2027, dueMonthPlusOne, dueDayPlusOne), 2026, 2027, LocalDate.of(2027, dueMonth, dueDay)),
    (LocalDate.of(2028, dueMonthPlusOne, dueDayPlusOne), 2027, 2028, LocalDate.of(2028, dueMonth, dueDay))
  )

  "fromCurrentDate" should {
    "return expected start and finish year and dateDue" in forAll(fromCurrentDateTestParameters) {
      (currentDate: LocalDate, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromCurrentDate(currentDate)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }

    "return expected previous start and finish year and dateDue" in forAll(previousFromCurrentDateTestParameters) {
      (
        currentDate: LocalDate,
        expectedPreviousStartyear: Int,
        expectedPreviousFinishYear: Int,
        expectedPreviousDateDue: LocalDate
      ) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromCurrentDate(currentDate).previous

        expectedPreviousStartyear  shouldBe eclTaxYear.startYear
        expectedPreviousFinishYear shouldBe eclTaxYear.finishYear
        expectedPreviousDateDue    shouldBe eclTaxYear.dateDue
    }
  }

  val fromDateTestParameters: TableFor4[LocalDate, Int, Int, LocalDate] = Table(
    ("date", "expectedStartyear", "expectedFinishYear", "expectedDueDate"),
    (
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay)
    ),
    (LocalDate.of(2022, taxYearStartMonth, taxYearStartDay), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (LocalDate.of(2022, dueMonth, dueDay), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (LocalDate.of(2022, dueMonthPlusOne, dueDayPlusOne), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay)
    ),
    (LocalDate.of(2023, taxYearStartMonth, taxYearStartDay), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2023, dueMonth, dueDay), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2023, dueMonthPlusOne, dueDayPlusOne), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2023,
      2024,
      LocalDate.of(2024, dueMonth, dueDay)
    ),
    (LocalDate.of(2024, taxYearStartMonth, taxYearStartDay), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonth, dueDay), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne), 2024, 2025, LocalDate.of(2025, dueMonth, dueDay))
  )

  val previousFromDateTestParameters: TableFor4[LocalDate, Int, Int, LocalDate] = Table(
    ("date", "expectedStartyear", "expectedFinishYear", "expectedDueDate"),
    (
      LocalDate.of(2022, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2020,
      2021,
      LocalDate.of(2021, dueMonth, dueDay)
    ),
    (LocalDate.of(2022, taxYearStartMonth, taxYearStartDay), 2021, 2022, LocalDate.of(2022, dueMonth, dueDay)),
    (LocalDate.of(2022, dueMonth, dueDay), 2021, 2022, LocalDate.of(2022, dueMonth, dueDay)),
    (LocalDate.of(2022, dueMonthPlusOne, dueDayPlusOne), 2021, 2022, LocalDate.of(2022, dueMonth, dueDay)),
    (
      LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2021,
      2022,
      LocalDate.of(2022, dueMonth, dueDay)
    ),
    (LocalDate.of(2023, taxYearStartMonth, taxYearStartDay), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (LocalDate.of(2023, dueMonth, dueDay), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (LocalDate.of(2023, dueMonthPlusOne, dueDayPlusOne), 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (
      LocalDate.of(2024, taxYearStartMonthMinusOne, taxYearStartDayMinusOne),
      2022,
      2023,
      LocalDate.of(2023, dueMonth, dueDay)
    ),
    (LocalDate.of(2024, taxYearStartMonth, taxYearStartDay), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonth, dueDay), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (LocalDate.of(2024, dueMonthPlusOne, dueDayPlusOne), 2023, 2024, LocalDate.of(2024, dueMonth, dueDay))
  )

  "fromDate" should {
    "return expected start and finish year and dateDue" in forAll(fromDateTestParameters) {
      (date: LocalDate, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromDate(date)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }

    "return expected previous start and finish year and dateDue" in forAll(previousFromDateTestParameters) {
      (date: LocalDate, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromDate(date).previous

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }
  }

  val fromStartYearTestParameters: TableFor4[Int, Int, Int, LocalDate] = Table(
    ("startYear", "expectedStartYear", "expectedFinishYear", "expectedDateDue"),
    (2022, 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (2023, 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (2024, 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (2025, 2025, 2026, LocalDate.of(2026, dueMonth, dueDay)),
    (2026, 2026, 2027, LocalDate.of(2027, dueMonth, dueDay)),
    (2027, 2027, 2028, LocalDate.of(2028, dueMonth, dueDay)),
    (2028, 2028, 2029, LocalDate.of(2029, dueMonth, dueDay)),
    (2029, 2029, 2030, LocalDate.of(2030, dueMonth, dueDay)),
    (2030, 2030, 2031, LocalDate.of(2031, dueMonth, dueDay)),
    (2031, 2031, 2032, LocalDate.of(2032, dueMonth, dueDay))
  )

  val fromPreviousStartYearTestParameters: TableFor4[Int, Int, Int, LocalDate] = Table(
    ("startYear", "expectedStartYear", "expectedFinishYear", "expectedDateDue"),
    (2022, 2021, 2022, LocalDate.of(2022, dueMonth, dueDay)),
    (2023, 2022, 2023, LocalDate.of(2023, dueMonth, dueDay)),
    (2024, 2023, 2024, LocalDate.of(2024, dueMonth, dueDay)),
    (2025, 2024, 2025, LocalDate.of(2025, dueMonth, dueDay)),
    (2026, 2025, 2026, LocalDate.of(2026, dueMonth, dueDay)),
    (2027, 2026, 2027, LocalDate.of(2027, dueMonth, dueDay)),
    (2028, 2027, 2028, LocalDate.of(2028, dueMonth, dueDay)),
    (2029, 2028, 2029, LocalDate.of(2029, dueMonth, dueDay)),
    (2030, 2029, 2030, LocalDate.of(2030, dueMonth, dueDay)),
    (2031, 2030, 2031, LocalDate.of(2031, dueMonth, dueDay))
  )

  "fromStartYear" should {
    "return expected start and finish year and dateDue" in forAll(fromStartYearTestParameters) {
      (startYear: Int, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromStartYear(startYear)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }

    "return expected previous start and finish year and dateDue" in forAll(fromPreviousStartYearTestParameters) {
      (startYear: Int, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromStartYear(startYear).previous

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }
  }
}
