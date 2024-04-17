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

package uk.gov.hmrc.economiccrimelevyregistration.forms.mappings

import org.mockito.Mockito.when
import org.scalacheck.Gen
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.TableFor3
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.data.validation.{Invalid, Valid}
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.{emailMaxLength, telephoneNumberMaxLength, utrLength}
import uk.gov.hmrc.economiccrimelevyregistration.generators.Generators
import uk.gov.hmrc.economiccrimelevyregistration.services.LocalDateService
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

class ConstraintsSpec extends AnyWordSpec with Matchers with ScalaCheckPropertyChecks with Generators with Constraints {

  val mockLocalDateService: LocalDateService = mock[LocalDateService]

  "firstError" should {

    "return Valid when all constraints pass" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("foo")
      result shouldEqual Valid
    }

    "return Invalid when the first constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("a" * 11)
      result shouldEqual Invalid("error.length", 10)
    }

    "return Invalid when the second constraint fails" in {
      val result = firstError(maxLength(10, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result shouldEqual Invalid("error.regexp", """^\w+$""")
    }

    "return Invalid for the first error when both constraints fail" in {
      val result = firstError(maxLength(-1, "error.length"), regexp("""^\w+$""", "error.regexp"))("")
      result shouldEqual Invalid("error.length", -1)
    }
  }

  "minimumValue" should {

    "return Valid for a number greater than the threshold" in {
      val result = minimumValue(1, "error.min").apply(2)
      result shouldEqual Valid
    }

    "return Valid for a number equal to the threshold" in {
      val result = minimumValue(1, "error.min").apply(1)
      result shouldEqual Valid
    }

    "return Invalid for a number below the threshold" in {
      val result = minimumValue(1, "error.min").apply(0)
      result shouldEqual Invalid("error.min", 1)
    }
  }

  "maximumValue" should {

    "return Valid for a number less than the threshold" in {
      val result = maximumValue(1, "error.max").apply(0)
      result shouldEqual Valid
    }

    "return Valid for a number equal to the threshold" in {
      val result = maximumValue(1, "error.max").apply(1)
      result shouldEqual Valid
    }

    "return Invalid for a number above the threshold" in {
      val result = maximumValue(1, "error.max").apply(2)
      result shouldEqual Invalid("error.max", 1)
    }
  }

  "regexp" should {

    "return Valid for an input that matches the expression" in {
      val result = regexp("""^\w+$""", "error.invalid")("foo")
      result shouldEqual Valid
    }

    "return Invalid for an input that does not match the expression" in {
      val result = regexp("""^\d+$""", "error.invalid")("foo")
      result shouldEqual Invalid("error.invalid", """^\d+$""")
    }
  }

  "maxLength" should {

    "return Valid for a string shorter than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 9)
      result shouldEqual Valid
    }

    "return Valid for an empty string" in {
      val result = maxLength(10, "error.length")("")
      result shouldEqual Valid
    }

    "return Valid for a string equal to the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 10)
      result shouldEqual Valid
    }

    "return Invalid for a string longer than the allowed length" in {
      val result = maxLength(10, "error.length")("a" * 11)
      result shouldEqual Invalid("error.length", 10)
    }
  }

  "maxDate" should {

    "return Valid for a date before or equal to the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max  <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), max)
      } yield (max, date)

      forAll(gen) { case (max, date) =>
        val result = maxDate(max, "error.future")(date)
        result shouldEqual Valid
      }
    }

    "return Invalid for a date after the maximum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        max  <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(max.plusDays(1), LocalDate.of(3000, 1, 2))
      } yield (max, date)

      forAll(gen) { case (max, date) =>
        val result = maxDate(max, "error.future", "foo")(date)
        result shouldEqual Invalid("error.future", "foo")
      }
    }
  }

  "minDate" should {

    "return Valid for a date after or equal to the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min  <- datesBetween(LocalDate.of(2000, 1, 1), LocalDate.of(3000, 1, 1))
        date <- datesBetween(min, LocalDate.of(3000, 1, 1))
      } yield (min, date)

      forAll(gen) { case (min, date) =>
        val result = minDate(min, "error.past", "foo")(date)
        result shouldEqual Valid
      }
    }

    "return Invalid for a date before the minimum" in {

      val gen: Gen[(LocalDate, LocalDate)] = for {
        min  <- datesBetween(LocalDate.of(2000, 1, 2), LocalDate.of(3000, 1, 1))
        date <- datesBetween(LocalDate.of(2000, 1, 1), min.minusDays(1))
      } yield (min, date)

      forAll(gen) { case (min, date) =>
        val result = minDate(min, "error.past", "foo")(date)
        result shouldEqual Invalid("error.past", "foo")
      }
    }
  }

  "telephoneNumber" should {
    "return invalid when the telephone number is too long" in forAll(stringsLongerThan(telephoneNumberMaxLength)) {
      s: String =>
        val result = telephoneNumber(telephoneNumberMaxLength, "error.length", "error.invalid")(s)
        result shouldEqual Invalid("error.length", telephoneNumberMaxLength)
    }

    "return invalid when the telephone number is not valid" in forAll(
      stringsWithMaxLength(telephoneNumberMaxLength).retryUntil(s => !s.matches(Regex.telephoneNumberRegex))
    ) { s: String =>
      val result = telephoneNumber(telephoneNumberMaxLength, "error.length", "error.invalid")(s)
      result shouldEqual Invalid("error.invalid", Regex.telephoneNumberRegex)
    }
  }

  "emailAddress" should {
    "return invalid when the email address is too long" in forAll(stringsLongerThan(emailMaxLength)) { s: String =>
      val result = emailAddress(emailMaxLength, "error.length", "error.invalid")(s)
      result shouldEqual Invalid("error.length", emailMaxLength)
    }

    "return invalid when the email address is not valid" in forAll(
      stringsWithMaxLength(emailMaxLength).retryUntil(s => !s.matches(Regex.emailRegex))
    ) { s: String =>
      val result = emailAddress(emailMaxLength, "error.length", "error.invalid")(s)
      result shouldEqual Invalid("error.invalid", Regex.emailRegex)
    }
  }

  "exactLength" should {
    val value = "VALUE"
    val error = "ERROR"

    "return Valid for a string of correct length" in {
      val result = exactLength(value.length, error).apply(value)
      result shouldEqual Valid
    }

    "return Invalid for a string of incorrect length" in {
      val length = value.length / 2
      val result = exactLength(length, error).apply(value)
      result shouldEqual Invalid(error, length)
    }
  }

  "areAllElementsNumbers" should {
    "return Valid for a string of numbers" in {
      val result =
        areAllElementsNumbersOfExactLength(utrLength, "otherEntityType.addCtutr.error.length").apply("0123456789")
      result shouldEqual Valid
    }

    "return Invalid for a string that contains alphanumerical values" in {
      val result =
        areAllElementsNumbersOfExactLength(utrLength, "otherEntityType.addCtutr.error.length").apply("10234567UK")
      result shouldEqual Invalid("otherEntityType.addCtutr.error.length", utrLength)
    }
  }

  val isBeforeCurrentTaxYearStartParameters: TableFor3[LocalDate, LocalDate, Boolean] = Table(
    ("fromDate", "testDate", "isValid"),
    (LocalDate.of(2024, 4, 17), LocalDate.of(2024, 4, 1), false),
    (LocalDate.of(2024, 10, 1), LocalDate.of(2024, 1, 1), true),
    (LocalDate.of(2024, 10, 1), LocalDate.of(2023, 1, 1), true)
  )

  "isBeforeCurrentTaxYearStart" should {
    "disallow dates before current tax year start for dates between the start of the tax year and the due date" in forAll(
      isBeforeCurrentTaxYearStartParameters
    ) {
      (
        fromDate: LocalDate,
        testDate: LocalDate,
        isValid: Boolean
      ) =>
        when(mockLocalDateService.now()).thenReturn(fromDate)
        val result         = isBeforeCurrentTaxYearStart(mockLocalDateService, "liability.date.error.before")(testDate)
        val expectedResult = isValid match {
          case true  => Valid
          case false => Invalid("liability.date.error.before", EclTaxYear.fromDate(fromDate).startYear)
        }
        result shouldEqual expectedResult
    }
  }
}
