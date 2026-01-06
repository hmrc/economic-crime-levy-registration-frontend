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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

case class LiabilityYear(value: Int) {
  def now(): LocalDate             = LocalDate.now()
  def currentTaxYear(): EclTaxYear = EclTaxYear.fromCurrentDate(now())
  val followingYear: String        = (value + 1).toString
  val asString: String             = value.toString
  val isCurrentFY: Boolean         = value == currentTaxYear().startYear
  val isNotCurrentFY: Boolean      = !isCurrentFY
}

object LiabilityYear {
  implicit val format: Format[LiabilityYear] = Json.format[LiabilityYear]
}
