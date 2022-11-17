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

sealed trait AmlSupervisor

case object Hmrc extends AmlSupervisor
case object GamblingCommission extends AmlSupervisor
case object FinancialConductAuthority extends AmlSupervisor
case object Other extends AmlSupervisor

object AmlSupervisor {

  val values: Seq[AmlSupervisor] = Seq(
    Hmrc,
    GamblingCommission,
    FinancialConductAuthority,
    Other
  )

  def options(implicit messages: Messages): Seq[RadioItem] =
    values.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"amlSupervisor.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
    }

  implicit val enumerable: Enumerable[AmlSupervisor] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val format: Format[AmlSupervisor] = new Format[AmlSupervisor] {
    override def reads(json: JsValue): JsResult[AmlSupervisor] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Hmrc"                      => JsSuccess(Hmrc)
          case "GamblingCommission"        => JsSuccess(GamblingCommission)
          case "FinancialConductAuthority" => JsSuccess(FinancialConductAuthority)
          case "Other"                     => JsSuccess(Other)
          case s                           => JsError(s"$s is not a valid AmlSupervisor")
        }
      case e: JsError          => e
    }

    override def writes(o: AmlSupervisor): JsValue = JsString(o.toString)
  }
}
