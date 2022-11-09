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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait EntityType

case object UkLimitedCompany extends EntityType
case object SoleTrader extends EntityType
case object GeneralPartnership extends EntityType
case object ScottishPartnership extends EntityType
case object LimitedPartnership extends EntityType
case object ScottishLimitedPartnership extends EntityType
case object LimitedLiabilityPartnership extends EntityType

object EntityType {

  val values: Seq[EntityType] = Seq(
    UkLimitedCompany,
    SoleTrader,
    GeneralPartnership,
    ScottishPartnership,
    LimitedPartnership,
    ScottishLimitedPartnership,
    LimitedLiabilityPartnership
  )

  def options(implicit messages: Messages): Seq[RadioItem] =
    values.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"entityType.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
    }

  implicit val enumerable: Enumerable[EntityType] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val format: Format[EntityType] = new Format[EntityType] {
    override def reads(json: JsValue): JsResult[EntityType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "UkLimitedCompany"            => JsSuccess(UkLimitedCompany)
          case "SoleTrader"                  => JsSuccess(SoleTrader)
          case "GeneralPartnership"          => JsSuccess(GeneralPartnership)
          case "ScottishPartnership"         => JsSuccess(ScottishPartnership)
          case "LimitedPartnership"          => JsSuccess(LimitedPartnership)
          case "ScottishLimitedPartnership"  => JsSuccess(ScottishLimitedPartnership)
          case "LimitedLiabilityPartnership" => JsSuccess(LimitedLiabilityPartnership)
          case s                             => JsError(s"$s is not a valid EntityType")
        }
      case e: JsError          => e
    }

    override def writes(o: EntityType): JsValue = JsString(o.toString)
  }
}
