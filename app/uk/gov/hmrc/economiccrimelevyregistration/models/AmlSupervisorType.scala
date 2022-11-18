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
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.govukfrontend.views.html.components.GovukSelect
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.{Select, SelectItem}
import uk.gov.hmrc.govukfrontend.views.Implicits.RichSelect
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.accessibleautocomplete.AccessibleAutocomplete

sealed trait AmlSupervisorType

case object Hmrc extends AmlSupervisorType
case object GamblingCommission extends AmlSupervisorType
case object FinancialConductAuthority extends AmlSupervisorType
case object Other extends AmlSupervisorType

object AmlSupervisorType {

  val values: Seq[AmlSupervisorType] = Seq(
    Hmrc,
    GamblingCommission,
    FinancialConductAuthority,
    Other
  )

  def options(appConfig: AppConfig, govukSelect: GovukSelect)(implicit messages: Messages): Seq[RadioItem] = {
    val amlProfessionalBodySuperviserOptions: Seq[SelectItem] =
      SelectItem(
        text = ""
      ) +: appConfig.amlProfessionalBodySupervisors.map { opb =>
        SelectItem(
          value = Some(opb),
          text = messages(s"amlSupervisor.opb.$opb")
        )
      }

    val selectAmlProfessionalBody = govukSelect(
      Select(
        id = "otherProfessionalBody",
        name = "otherProfessionalBody",
        items = amlProfessionalBodySuperviserOptions,
        label = Label(
          content = Text(messages("amlSupervisor.selectFromList"))
        )
      ).asAccessibleAutocomplete(
        Some(AccessibleAutocomplete(defaultValue = Some(""), showAllValues = true, autoSelect = false))
      )
    )

    values.zipWithIndex.map { case (value, index) =>
      value match {
        case Other =>
          RadioItem(
            content = Text(messages(s"amlSupervisor.${value.toString}")),
            value = Some(value.toString),
            id = Some(s"value_$index"),
            conditionalHtml = Some(selectAmlProfessionalBody)
          )
        case _     =>
          RadioItem(
            content = Text(messages(s"amlSupervisor.${value.toString}")),
            value = Some(value.toString),
            id = Some(s"value_$index")
          )
      }

    }
  }

  implicit val enumerable: Enumerable[AmlSupervisorType] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val format: Format[AmlSupervisorType] = new Format[AmlSupervisorType] {
    override def reads(json: JsValue): JsResult[AmlSupervisorType] = json.validate[String] match {
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

    override def writes(o: AmlSupervisorType): JsValue = JsString(o.toString)
  }
}

case class AmlSupervisor(supervisorType: AmlSupervisorType, otherProfessionalBody: Option[String])
