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

import play.api.data.validation.{Constraint, Invalid, Valid}
import uk.gov.hmrc.economiccrimelevyregistration.services.LocalDateService
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

trait Constraints {

  protected def firstError[A](constraints: Constraint[A]*): Constraint[A] =
    Constraint { input =>
      constraints
        .map(_.apply(input))
        .find(_ != Valid)
        .getOrElse(Valid)
    }

  protected def minimumValue[A](minimum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint { input =>
      import ev._

      if (input >= minimum) {
        Valid
      } else {
        Invalid(errorKey, minimum)
      }
    }

  protected def maximumValue[A](maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint { input =>
      import ev._

      if (input <= maximum) {
        Valid
      } else {
        Invalid(errorKey, maximum)
      }
    }

  protected def inRange[A](minimum: A, maximum: A, errorKey: String)(implicit ev: Ordering[A]): Constraint[A] =
    Constraint { input =>
      import ev._

      if (input >= minimum && input <= maximum) {
        Valid
      } else {
        Invalid(errorKey, minimum, maximum)
      }
    }

  protected def regexp(regex: String, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.matches(regex) =>
        Valid
      case _                         =>
        Invalid(errorKey, regex)
    }

  protected def maxLength(maximum: Int, errorKey: String): Constraint[String] =
    Constraint {
      case str if str.length <= maximum =>
        Valid
      case _                            =>
        Invalid(errorKey, maximum)
    }

  protected def maxDate(maximum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isAfter(maximum) =>
        Invalid(errorKey, args: _*)
      case _                             =>
        Valid
    }

  protected def minDate(minimum: LocalDate, errorKey: String, args: Any*): Constraint[LocalDate] =
    Constraint {
      case date if date.isBefore(minimum) =>
        Invalid(errorKey, args: _*)
      case _                              =>
        Valid
    }

  protected def isBeforeCurrentTaxYearStart(
    localDateService: LocalDateService,
    errorKey: String
  ): Constraint[LocalDate] = {
    val now        = localDateService.now()
    val eclTaxYear = EclTaxYear.fromDate(now)

    Constraint {
      case date
          if eclTaxYear
            .isBetweenStartDateAndPreviousDateDue(date) &&
            eclTaxYear.isBetweenStartDateAndPreviousDateDue(now) =>
        Invalid(errorKey, eclTaxYear.startYear.toString)
      case _ =>
        Valid
    }
  }

  protected def telephoneNumber(max: Int, maxLengthKey: String, invalidKey: String): Constraint[String] = Constraint {
    s =>
      maxLength(max, maxLengthKey)(s) match {
        case Valid   => regexp(Regex.telephoneNumberRegex, invalidKey)(s)
        case invalid => invalid
      }
  }

  protected def emailAddress(max: Int, maxLengthKey: String, invalidKey: String): Constraint[String] = Constraint { s =>
    maxLength(max, maxLengthKey)(s) match {
      case Valid   => regexp(Regex.emailRegex, invalidKey)(s)
      case invalid => invalid
    }
  }

  protected def exactLength(length: Int, invalidKey: String): Constraint[String] = Constraint {
    case input if input.length != length =>
      Invalid(invalidKey, length)
    case _                               => Valid
  }

  protected def areAllElementsNumbersOfExactLength(length: Int, invalidKey: String): Constraint[String] = Constraint {
    case input if input.exists(!_.isDigit) || input.length != length =>
      Invalid(invalidKey, length)
    case _                                                           => Valid
  }
}
