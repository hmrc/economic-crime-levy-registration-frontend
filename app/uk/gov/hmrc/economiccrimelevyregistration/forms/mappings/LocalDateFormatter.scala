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
        Left(
          Seq(
            FormError(s"$key.day", invalidKey, args),
            FormError(s"$key.month", invalidKey, args),
            FormError(s"$key.year", invalidKey, args)
          )
        )
    }

  private def validateDayMonthYear(
    key: String,
    day: Option[String],
    month: Option[String],
    year: Option[String]
  ): Seq[FormError] = {

    def validateDay: Boolean =
      Try(ChronoField.DAY_OF_MONTH.checkValidIntValue(day.get.toInt)) match {
        case Success(_) => true
        case Failure(_) => false
      }

    def validateMonth: Boolean =
      Try(ChronoField.MONTH_OF_YEAR.checkValidIntValue(month.get.toInt)) match {
        case Success(_) => true
        case Failure(_) => false
      }

    def validateYear: Boolean =
      Try(ChronoField.YEAR.checkValidIntValue(year.get.toInt)) match {
        case Success(_) => true
        case Failure(_) => false
      }

    (day, month, year) match {
      case (Some(d), Some(m), Some(y)) =>
        (validateDay, validateMonth, validateYear) match {
          case (true, true, true)   => Nil
          case (true, false, false) =>
            Seq(
              FormError(s"$key.month", "error.monthYear.required"),
              FormError(s"$key.year", "error.monthYear.required")
            )
          case (true, true, false)  =>
            Seq(
              FormError(s"$key.year", "error.year.invalid")
            )
          case (true, false, true)  =>
            Seq(
              FormError(s"$key.month", "error.month.invalid")
            )
          case (false, true, true)  =>
            Seq(
              FormError(s"$key.day", "error.day.invalid")
            )
          case (false, true, false) =>
            Seq(
              FormError(s"$key.day", "error.dayYear.required"),
              FormError(s"$key.year", "error.dayYear.required")
            )
          case (false, false, true) =>
            Seq(
              FormError(s"$key.day", "error.dayMonth.required"),
              FormError(s"$key.month", "error.dayMonth.required")
            )

          case (false, false, false) =>
            Seq(
              FormError(s"$key.day", invalidKey),
              FormError(s"$key.month", invalidKey),
              FormError(s"$key.year", invalidKey)
            )

        }
      case (Some(d), Some(m), None)    => Seq(FormError(s"$key.year", "error.year.required"))
      case (Some(d), None, Some(y))    => Seq(FormError(s"$key.month", "error.month.required"))
      case (Some(d), None, None)       =>
        Seq(FormError(s"$key.month", "error.monthYear.required"), FormError(s"$key.year", "error.monthYear.required"))
      case (None, Some(m), Some(y))    => Seq(FormError(s"$key.day", "error.day.required"))
      case (None, Some(m), None)       =>
        Seq(FormError(s"$key.day", "error.dayYear.required"), FormError(s"$key.year", "error.dayYear.required"))
      case (None, None, Some(y))       =>
        Seq(
          FormError(s"$key.day", "error.dayMonth.required"),
          FormError(s"$key.month", "error.dayMonth.required")
        )
      case _                           =>
        Seq(
          FormError(s"$key.day", requiredKey),
          FormError(s"$key.month", requiredKey),
          FormError(s"$key.year", requiredKey)
        )

    }

  }

  override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], LocalDate] = {
    val dayKey: String   = s"$key.day"
    val monthKey: String = s"$key.month"
    val yearKey: String  = s"$key.year"

    val day: Option[String]   = data.get(dayKey).filter(_.nonEmpty)
    val month: Option[String] = data.get(monthKey).filter(_.nonEmpty)
    val year: Option[String]  = data.get(yearKey).filter(_.nonEmpty)

    val errors = validateDayMonthYear(key, day, month, year)
    errors match {
      case Nil          =>
        toDate(key, day.get.toInt, month.get.toInt, year.get.toInt) match {
          case Right(date) =>
            val minMaxErrors = Seq(minDateConstraint, maxDateConstraint)
              .map(_.toSeq)
              .flatMap(_.flatMap { c =>
                c(date) match {
                  case Invalid(e) =>
                    e.map(error =>
                      Seq(
                        FormError(dayKey, error.message, error.args),
                        FormError(monthKey, error.message, error.args),
                        FormError(yearKey, error.message, error.args)
                      )
                    )
                  case _          => Nil
                }
              })
            Either.cond(minMaxErrors.isEmpty, date, minMaxErrors.head)
          case err         => err
        }
      case error :: Nil => Left(Seq(error))
      case _            => Left(errors)
    }
  }

  override def unbind(key: String, value: LocalDate): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}
