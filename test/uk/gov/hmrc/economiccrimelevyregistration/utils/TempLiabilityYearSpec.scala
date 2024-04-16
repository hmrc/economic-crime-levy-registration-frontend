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

import org.scalatest.prop.TableFor2
import org.scalatest.prop.TableFor3
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

import java.time.LocalDate

class TempLiabilityYearSpec extends SpecBase {

  private val dueDay                    = 30
  private val dueDayPlusOne             = 1
  private val dueMonth                  = 9
  private val dueMonthPlusOne           = 10
  private val taxYearStartMonth         = 4
  private val taxYearStartMonthMinusOne = 3
  private val taxYearStartDay           = 1
  private val taxYearStartDayMinusOne   = 31

  val currentTaxYearParameters: TableFor3[LocalDate, Int, TempTaxYear] = Table(
    (
      "currentDate",
      "liabilityYear",
      "expectedTaxYear"
    ),
    (
      LocalDate.of(2022, taxYearStartMonth, taxYearStartDay),
      2021,
      TempTaxYear.fromCurrentDate(LocalDate.of(2022, taxYearStartMonth, taxYearStartDay))
    ),
    (
      LocalDate.of(2022, dueMonthPlusOne, dueDayPlusOne),
      2022,
      TempTaxYear.fromCurrentDate(LocalDate.of(2022, dueMonthPlusOne, dueDayPlusOne))
    )
  )

  "currentTaxYear" should {
    "return current tax year for the specified date and liability year" in forAll(currentTaxYearParameters) {
      (
        currentDate: LocalDate,
        liabilityYear,
        expectedTaxYear: TempTaxYear
      ) =>

        val year: TempLiabilityYear = new TempLiabilityYear(liabilityYear) {
          override def now(): LocalDate = currentDate
        }

        expectedTaxYear shouldBe year.currentTaxYear()
    }
  }

  val followingYearParameters: TableFor2[Int, Int] = Table(
    (
      "liabilityYear",
      "expectedFollowingYear"
    ),
    (
      2021,
      2022
    ),
    (
      2022,
      2023
    )
  )

  "followingYear" should {
    "return following liability year" in forAll(followingYearParameters) {
      (
        liabilityYear: Int,
        expectedFollowingYear: Int
      ) =>

        val year = TempLiabilityYear(liabilityYear)

        expectedFollowingYear.toString shouldBe year.followingYear
    }
  }

  val asStringParameters: TableFor2[Int, String] = Table(
    (
      "liabilityYear",
      "expectedYearAsString"
    ),
    (
      2021,
      "2021"
    ),
    (
      2022,
      "2022"
    )
  )

  "asString" should {
    "return liability year as string" in forAll(asStringParameters) {
      (
        liabilityYear,
        expectedLiabilityYearAsString
      ) =>

        val year = TempLiabilityYear(liabilityYear)

        expectedLiabilityYearAsString shouldBe year.asString
    }

  }

  val isCurrentFYParameters: TableFor3[Int, LocalDate, Boolean] = Table(
    ("liabilityYear", "currentDate", "expectedIsCurrentFy"),
    (2022, LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne), true),
    (2022, LocalDate.of(2023, taxYearStartMonth, taxYearStartDay), true),
    (2023, LocalDate.of(2023, dueMonthPlusOne, dueDayPlusOne), true),
    (2023, LocalDate.of(2023, dueMonth, dueDay), false),
  )

  "isCurrentFY" should {
    "returns the expected isCurrentFY" in forAll (isCurrentFYParameters) {
      (
        liabilityYear: Int,
        currentDate: LocalDate,
        expectedIsCurrentFy: Boolean
        
      ) =>

        val year: TempLiabilityYear = new TempLiabilityYear(liabilityYear) {
          override def now(): LocalDate = currentDate
        }

        expectedIsCurrentFy shouldBe year.isCurrentFY
    }
  }

  val isNotCurrentFYParameters: TableFor3[Int, LocalDate, Boolean] = Table(
    ("liabilityYear", "currentDate", "expectedIsNotCurrentFY"),
    (2022, LocalDate.of(2023, taxYearStartMonthMinusOne, taxYearStartDayMinusOne), false),
    (2022, LocalDate.of(2023, taxYearStartMonth, taxYearStartDay), false),
    (2023, LocalDate.of(2023, dueMonthPlusOne, dueDayPlusOne), false),
    (2023, LocalDate.of(2023, dueMonth, dueDay), true),
  )

  "isNotCurrentFY" should {
    "returns the expected isNotCurrentFY" in forAll (isNotCurrentFYParameters) {
      (
        liabilityYear: Int,
        currentDate: LocalDate,
        expectedIsNotCurrentFY: Boolean
      ) =>

        val year: TempLiabilityYear = new TempLiabilityYear(liabilityYear) {
          override def now(): LocalDate = currentDate
        }

        expectedIsNotCurrentFY shouldBe year.isNotCurrentFY
    }
  }

}
