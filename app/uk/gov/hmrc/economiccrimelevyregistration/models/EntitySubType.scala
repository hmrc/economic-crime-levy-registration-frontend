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

import play.api.i18n.Messages
import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.radios.RadioItem

sealed trait EntitySubType

object EntitySubType {
  case object Charity extends EntitySubType
  case object Trust extends EntitySubType
  case object RegisteredSociety extends EntitySubType
  case object NonUKEstablishment extends EntitySubType
  case object UnincorporatedAssociation extends EntitySubType

  val values: Seq[EntitySubType] = Seq(
    Charity,
    Trust,
    RegisteredSociety,
    NonUKEstablishment,
    UnincorporatedAssociation
  )

  def options(appConfig: AppConfig)(implicit messages: Messages): Seq[RadioItem] = {
    val radioItems = values.zipWithIndex.map { case (value, index) =>
      RadioItem(
        content = Text(messages(s"entitySubType.${value.toString}")),
        value = Some(value.toString),
        id = Some(s"value_$index")
      )
    }

    radioItems
  }

  implicit val enumerable: Enumerable[EntitySubType] = Enumerable(values.map(v => (v.toString, v)): _*)

  implicit val format: Format[EntitySubType] = new Format[EntitySubType] {
    override def reads(json: JsValue): JsResult[EntitySubType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Charity"                   => JsSuccess(Charity)
          case "Trust"                     => JsSuccess(Trust)
          case "RegisteredSociety"         => JsSuccess(RegisteredSociety)
          case "NonUKEstablishment"        => JsSuccess(NonUKEstablishment)
          case "UnincorporatedAssociation" => JsSuccess(UnincorporatedAssociation)
          case s                           => JsError(s"$s is not a valid EntitySubType")
        }
      case e: JsError          => e
    }

    override def writes(o: EntitySubType): JsValue = JsString(o.toString)
  }
}
