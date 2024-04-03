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

import org.scalatest.prop.TableFor3
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

import java.time.LocalDate

class EclTaxYearSpec extends SpecBase {

  val testParameters: TableFor3[LocalDate, Int, Int] = Table(
    ("currentDate", "startOfEclTaxYear", "expectedYearDue"),
    (LocalDate.of(2023, 9, 29), 2022, 2023),
    (LocalDate.of(2024, 9, 29), 2023, 2024),
    (LocalDate.of(2025, 9, 29), 2024, 2025),
    (LocalDate.of(2026, 9, 29), 2025, 2026),
    (LocalDate.of(2027, 9, 29), 2026, 2027),
    (LocalDate.of(2024, 10, 1), 2024, 2025),
    (LocalDate.of(2025, 10, 1), 2025, 2026),
    (LocalDate.of(2026, 10, 1), 2026, 2027),
    (LocalDate.of(2027, 10, 1), 2027, 2028),
    (LocalDate.of(2028, 10, 1), 2028, 2029)
  )

  "calculateYearDue" should {
    "return expected year due" in forAll(testParameters) {
      (currentDate: LocalDate, startOfEclTaxYear, expectedYearDue: Int) =>
        val yearDue: Int = EclTaxYear.calculateYearDue(yearDue = startOfEclTaxYear, currentDate = currentDate)
        expectedYearDue shouldBe yearDue
    }
  }
}
