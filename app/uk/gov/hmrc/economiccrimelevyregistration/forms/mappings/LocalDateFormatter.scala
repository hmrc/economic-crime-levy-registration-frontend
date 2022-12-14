/*
 * Copyright 2022 HM Revenue & Customs
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

import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.data.validation.{Constraint, Invalid}

import java.time.LocalDate
import java.time.temporal.ChronoField
import scala.util.{Failure, Success, Try}

private[mappings] class LocalDateFormatter(
  invalidKey: String,
  requiredKey: String,
  minDateConstraint: Option[Constraint[LocalDate]] = None,
  maxDateConstraint: Option[Constraint[LocalDate]] = None,
  args: Seq[String] = Seq.empty
) extends Formatter[LocalDate]
    with Formatters {

  private def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        Right(date)
      case Failure(_)    =>
        Left(Seq(FormError(s"$key.day", invalidKey, args)))
    }

  private def validateDayMonthYear(
    key: String,
    day: String,
    month: String,
    year: String
  ): Seq[FormError] = {
    def validateDay: Seq[FormError] =
      Try(ChronoField.DAY_OF_MONTH.checkValidIntValue(day.toInt)) match {
        case Success(_) => Nil
        case Failure(_) => Seq(FormError(s"$key.day", "error.day.invalid"))
      }

    def validateMonth: Seq[FormError] =
      Try(ChronoField.MONTH_OF_YEAR.checkValidIntValue(month.toInt)) match {
        case Success(_) => Nil
        case Failure(_) => Seq(FormError(s"$key.month", "error.month.invalid"))
      }

    def validateYear: Seq[FormError] =
      Try(ChronoField.YEAR.checkValidIntValue(year.toInt)) match {
        case Success(_) => Nil
        case Failure(_) => Seq(FormError(s"$key.year", "error.year.invalid"))
      }

    validateDay ++ validateMonth ++ validateYear
  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val dayKey: String   = s"$key.day"
    val monthKey: String = s"$key.month"
    val yearKey: String  = s"$key.year"

    val day: Option[String]   = data.get(dayKey).filter(_.nonEmpty)
    val month: Option[String] = data.get(monthKey).filter(_.nonEmpty)
    val year: Option[String]  = data.get(yearKey).filter(_.nonEmpty)

    (day, month, year) match {
      case (Some(day), Some(month), Some(year)) =>
        validateDayMonthYear(key, day, month, year) match {
          case Nil          =>
            toDate(key, day.toInt, month.toInt, year.toInt) match {
              case Right(date) =>
                val minMaxErrors = Seq(minDateConstraint, maxDateConstraint)
                  .map(_.toSeq)
                  .flatMap(_.flatMap { c =>
                    c(date) match {
                      case Invalid(e) => e.map(err => FormError(dayKey, err.message, err.args))
                      case _          => Nil
                    }
                  })
                Either.cond(minMaxErrors.isEmpty, date, Seq(minMaxErrors.head))
              case err         => err
            }
          case error :: Nil => Left(Seq(error))
          case _            => Left(Seq(FormError(dayKey, invalidKey)))
        }
      case (Some(_), None, Some(_))             => Left(Seq(FormError(monthKey, "error.month.required")))
      case (None, Some(_), Some(_))             => Left(Seq(FormError(dayKey, "error.day.required")))
      case (Some(_), Some(_), None)             => Left(Seq(FormError(yearKey, "error.year.required")))
      case (None, None, Some(_))                => Left(Seq(FormError(dayKey, "error.dayMonth.required")))
      case (None, Some(_), None)                => Left(Seq(FormError(dayKey, "error.dayYear.required")))
      case (Some(_), None, None)                => Left(Seq(FormError(monthKey, "error.monthYear.required")))
      case _                                    => Left(Seq(FormError(dayKey, requiredKey, args)))
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}
