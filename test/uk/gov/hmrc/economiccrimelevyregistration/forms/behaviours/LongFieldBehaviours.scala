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

package uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours

import play.api.data.{Form, FormError}

trait LongFieldBehaviours extends FieldBehaviours {

  def longField(form: Form[_], fieldName: String, nonNumericError: FormError, wholeNumberError: FormError): Unit =
    "bind" should {
      "not bind non-numeric numbers" in {
        forAll(nonNumerics -> "nonNumeric") { nonNumeric =>
          val result = form.bind(Map(fieldName -> nonNumeric)).apply(fieldName)
          result.errors should contain only nonNumericError
        }
      }

      "not bind decimals" in {
        forAll(decimals -> "decimal") { decimal =>
          val result = form.bind(Map(fieldName -> decimal)).apply(fieldName)
          result.errors should contain only wholeNumberError
        }
      }
    }

  def longFieldWithRange(
    form: Form[_],
    fieldName: String,
    minimum: Long,
    maximum: Long,
    expectedError: FormError
  ): Unit =
    "bind" should {
      s"not bind longs outside the range $minimum to $maximum" in {

        forAll(longsOutsideRange(minimum, maximum) -> "longOutsideRange") { number =>
          val result = form.bind(Map(fieldName -> number.toString)).apply(fieldName)
          result.errors should contain only expectedError
        }
      }
    }
}
