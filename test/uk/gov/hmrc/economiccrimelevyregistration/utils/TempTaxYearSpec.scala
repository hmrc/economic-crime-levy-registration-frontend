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

  val fromCurrentDateTestParameters: TableFor4[LocalDate, Int, Int, LocalDate] = Table(
    ("currentDate", "expectedStartyear", "expectedFinishYear", "expectedDateDue"),
    (LocalDate.of(2023, 9, 29), 2022, 2023, LocalDate.of(2023, 9, 30)),
    (LocalDate.of(2024, 9, 29), 2023, 2024, LocalDate.of(2024, 9, 30)),
    (LocalDate.of(2025, 9, 29), 2024, 2025, LocalDate.of(2025, 9, 30)),
    (LocalDate.of(2026, 9, 29), 2025, 2026, LocalDate.of(2026, 9, 30)),
    (LocalDate.of(2027, 9, 29), 2026, 2027, LocalDate.of(2027, 9, 30)),
    (LocalDate.of(2024, 10, 1), 2024, 2025, LocalDate.of(2025, 9, 30)),
    (LocalDate.of(2025, 10, 1), 2025, 2026, LocalDate.of(2026, 9, 30)),
    (LocalDate.of(2026, 10, 1), 2026, 2027, LocalDate.of(2027, 9, 30)),
    (LocalDate.of(2027, 10, 1), 2027, 2028, LocalDate.of(2028, 9, 30)),
    (LocalDate.of(2028, 10, 1), 2028, 2029, LocalDate.of(2029, 9, 30))
  )

  "fromCurrentDate" should {
    "return expected start and finish year and dateDue" in forAll(fromCurrentDateTestParameters) {
      (currentDate: LocalDate, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromCurrentDate(currentDate)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }
  }

  val fromDateTestParameters: TableFor4[LocalDate, Int, Int, LocalDate] = Table(
    ("date", "expectedStartyear", "expectedFinishYear", "expectedDueDate"),
    (LocalDate.of(2022, 3, 31), 2021, 2022, LocalDate.of(2022, 9, 30)),
    (LocalDate.of(2022, 4, 1), 2022, 2023, LocalDate.of(2023, 9, 30)),
    (LocalDate.of(2022, 9, 30), 2022, 2023, LocalDate.of(2023, 9, 30)),
    (LocalDate.of(2022, 10, 1), 2022, 2023, LocalDate.of(2023, 9, 30)),
    (LocalDate.of(2023, 3, 31), 2022, 2023, LocalDate.of(2023, 9, 30)),
    (LocalDate.of(2023, 4, 1), 2023, 2024, LocalDate.of(2024, 9, 30)),
    (LocalDate.of(2023, 9, 30), 2023, 2024, LocalDate.of(2024, 9, 30)),
    (LocalDate.of(2023, 10, 1), 2023, 2024, LocalDate.of(2024, 9, 30)),
    (LocalDate.of(2024, 3, 31), 2023, 2024, LocalDate.of(2024, 9, 30)),
    (LocalDate.of(2024, 4, 1), 2024, 2025, LocalDate.of(2025, 9, 30)),
    (LocalDate.of(2024, 9, 30), 2024, 2025, LocalDate.of(2025, 9, 30)),
    (LocalDate.of(2024, 10, 1), 2024, 2025, LocalDate.of(2025, 9, 30))
  )

  "fromDate" should {
    "return expected start and finish year" in forAll(fromDateTestParameters) {
      (date: LocalDate, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromDate(date)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }
  }

  val fromStartYearTestParameters: TableFor4[Int, Int, Int, LocalDate] = Table(
    ("startYear", "expectedStartYear", "expectedFinishYear", "expectedDateDue"),
    (2022, 2022, 2023, LocalDate.of(2023, 9, 30)),
    (2023, 2023, 2024, LocalDate.of(2024, 9, 30)),
    (2024, 2024, 2025, LocalDate.of(2025, 9, 30)),
    (2025, 2025, 2026, LocalDate.of(2026, 9, 30)),
    (2026, 2026, 2027, LocalDate.of(2027, 9, 30)),
    (2027, 2027, 2028, LocalDate.of(2028, 9, 30)),
    (2028, 2028, 2029, LocalDate.of(2029, 9, 30)),
    (2029, 2029, 2030, LocalDate.of(2030, 9, 30)),
    (2030, 2030, 2031, LocalDate.of(2031, 9, 30)),
    (2031, 2031, 2032, LocalDate.of(2032, 9, 30))
  )

  "fromStartYear" should {
    "return expected finish year" in forAll(fromStartYearTestParameters) {
      (startYear: Int, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromStartYear(startYear)

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }
  }

  val previousTestParameters: TableFor4[Int, Int, Int, LocalDate] = Table(
    ("startYear", "expectedStartYear", "expectedFinishYear", "expectedDateDue"),
    (2022, 2021, 2022, LocalDate.of(2022, 9, 30)),
    (2023, 2022, 2023, LocalDate.of(2023, 9, 30)),
    (2024, 2023, 2021, LocalDate.of(2024, 9, 30)),
    (2025, 2024, 2025, LocalDate.of(2025, 9, 30)),
    (2026, 2025, 2026, LocalDate.of(2026, 9, 30)),
    (2027, 2026, 2027, LocalDate.of(2027, 9, 30)),
    (2028, 2027, 2028, LocalDate.of(2028, 9, 30)),
    (2029, 2028, 2029, LocalDate.of(2029, 9, 30)),
    (2030, 2029, 2030, LocalDate.of(2030, 9, 30)),
    (2031, 2030, 2031, LocalDate.of(2031, 9, 30))
  )

  "previous" should {
    "return expected finish year" in forAll(previousTestParameters) {
      (startYear: Int, expectedStartYear: Int, expectedFinishYear: Int, expectedDateDue: LocalDate) =>
        val eclTaxYear: TempTaxYear = TempTaxYear.fromStartYear(startYear).previous

        expectedStartYear  shouldBe eclTaxYear.startYear
        expectedFinishYear shouldBe eclTaxYear.finishYear
        expectedDateDue    shouldBe eclTaxYear.dateDue
    }
  }
}
