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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.data.Form
import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.all.FluentLabel
import uk.gov.hmrc.govukfrontend.views.Implicits.RichSelect
import uk.gov.hmrc.govukfrontend.views.html.components.GovukSelect
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage
import uk.gov.hmrc.govukfrontend.views.viewmodels.hint.Hint
import uk.gov.hmrc.govukfrontend.views.viewmodels.label.Label
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem
import uk.gov.hmrc.govukfrontend.views.viewmodels.select.{Select, SelectItem}
import uk.gov.hmrc.hmrcfrontend.views.viewmodels.accessibleautocomplete.AccessibleAutocomplete

sealed trait AmlSupervisorType

object AmlSupervisorType {
  case object FinancialConductAuthority extends AmlSupervisorType
  case object GamblingCommission extends AmlSupervisorType
  case object Hmrc extends AmlSupervisorType
  case object Other extends AmlSupervisorType

  val values: Seq[AmlSupervisorType] = Seq(
    FinancialConductAuthority,
    GamblingCommission,
    Hmrc,
    Other
  )

  val amendValues: Seq[AmlSupervisorType] = Seq(
    Hmrc,
    Other
  )

  def options(
    appConfig: AppConfig,
    govukSelect: GovukSelect,
    form: Form[AmlSupervisor]
  )(implicit
    messages: Messages
  ): Seq[RadioItem] = {
    val amlProfessionalBodySupervisorOptions: Seq[SelectItem] =
      SelectItem() +: appConfig.amlProfessionalBodySupervisors.map { opb =>
        SelectItem(
          value = Some(opb),
          text = messages(s"amlSupervisor.$opb"),
          selected = form.value match {
            case Some(AmlSupervisor(_, Some(value))) => value == opb
            case _                                   => false
          }
        )
      }

    val selectAmlProfessionalBody = govukSelect(
      Select(
        attributes = Map("aria-label" -> messages("amlSupervisor.selectFromList.label")),
        id = "otherProfessionalBody",
        name = "otherProfessionalBody",
        items = amlProfessionalBodySupervisorOptions,
        label = Label(
          content = Text(messages("amlSupervisor.selectFromList.label"))
        ).withCssClass("govuk-!-font-weight-bold"),
        hint = Some(Hint(content = Text(messages("amlSupervisor.selectFromList.hint")))),
        errorMessage = if (form.errors.exists(_.key == "otherProfessionalBody")) {
          Some(
            ErrorMessage.errorMessageWithDefaultStringsTranslated(
              content = Text(messages("amlSupervisor.selectFromList.error"))
            )
          )
        } else { None }
      ).asAccessibleAutocomplete(
        Some(AccessibleAutocomplete(defaultValue = Some(""), showAllValues = true, autoSelect = true))
      )
    )

    values.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"amlSupervisor.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index"),
        conditionalHtml = if (value == Other) {
          Some(selectAmlProfessionalBody)
        } else {
          None
        }
      )
    }
  }

  def optionsAmend(
    appConfig: AppConfig,
    govukSelect: GovukSelect,
    form: Form[AmlSupervisor]
  )(implicit
    messages: Messages
  ): Seq[RadioItem] = {
    val amlProfessionalBodySupervisorOptions: Seq[SelectItem] =
      SelectItem() +: appConfig.amlProfessionalBodySupervisors.map { opb =>
        SelectItem(
          value = Some(opb),
          text = messages(s"amlSupervisor.$opb"),
          selected = form.value match {
            case Some(AmlSupervisor(_, Some(value))) => value == opb
            case _                                   => false
          }
        )
      }

    val selectAmlProfessionalBody = govukSelect(
      Select(
        attributes = Map("aria-label" -> messages("amlSupervisor.selectFromList.label")),
        id = "otherProfessionalBody",
        name = "otherProfessionalBody",
        items = amlProfessionalBodySupervisorOptions,
        label = Label(
          content = Text(messages("amlSupervisor.selectFromList.label"))
        ).withCssClass("govuk-!-font-weight-bold"),
        hint = Some(Hint(content = Text(messages("amlSupervisor.selectFromList.hint")))),
        errorMessage = if (form.errors.exists(_.key == "otherProfessionalBody")) {
          Some(
            ErrorMessage.errorMessageWithDefaultStringsTranslated(
              content = Text(messages("amlSupervisor.selectFromList.error"))
            )
          )
        } else {
          None
        }
      ).asAccessibleAutocomplete(
        Some(AccessibleAutocomplete(defaultValue = Some(""), showAllValues = true, autoSelect = true))
      )
    )

    amendValues.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"amlSupervisor.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index"),
        conditionalHtml = if (value == Other) {
          Some(selectAmlProfessionalBody)
        } else {
          None
        }
      )
    }
  }

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

final case class AmlSupervisor(supervisorType: AmlSupervisorType, otherProfessionalBody: Option[String])

object AmlSupervisor {
  implicit val format: OFormat[AmlSupervisor] = Json.format[AmlSupervisor]
}
