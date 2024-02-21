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

package uk.gov.hmrc.economiccrimelevyregistration.models.deregister

import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyregistration.models.Enumerable
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait DeregisterReason

object DeregisterReason {
  case object NoAmlActivity extends DeregisterReason
  case object RegulatedByFcaOrGa extends DeregisterReason
  case object NoLongerMeetsThreshold extends DeregisterReason

  implicit val format: Format[DeregisterReason] = new Format[DeregisterReason] {
    override def reads(json: JsValue): JsResult[DeregisterReason] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "NoAmlActivity"          => JsSuccess(NoAmlActivity)
          case "RegulatedByFcaOrGa"     => JsSuccess(RegulatedByFcaOrGa)
          case "NoLongerMeetsThreshold" => JsSuccess(NoLongerMeetsThreshold)
          case s                        => JsError(s"$s is not a valid DeregisterReason")
        }
      case e: JsError          => e
    }

    override def writes(o: DeregisterReason): JsValue = JsString(o.toString)
  }

  val values: Seq[DeregisterReason] = Seq(
    NoAmlActivity,
    RegulatedByFcaOrGa,
    NoLongerMeetsThreshold
  )

  def options()(implicit messages: Messages): Seq[RadioItem] =
    values.zipWithIndex
      .map { case (value, index) =>
        RadioItem(
          content = Text(messages(s"deregisterReason.${value.toString}")),
          value = Some(value.toString),
          id = Some(s"value_$index")
        )
      }
      .sortBy(_.content.toString)

  implicit val enumerable: Enumerable[DeregisterReason] = Enumerable(values.map(v => (v.toString, v)): _*)
}
