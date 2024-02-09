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

import play.api.libs.json._
import play.api.mvc.JavascriptLiteral

sealed abstract class RegistrationType(value: String)

object RegistrationType {

  case object Initial extends RegistrationType("Initial")
  case object Amendment extends RegistrationType("Amendment")
  case object Dereg extends RegistrationType("Dereg")

  lazy val values: Set[RegistrationType] = Set(Initial, Amendment, Dereg)

  implicit val format: Format[RegistrationType] = new Format[RegistrationType] {
    override def reads(json: JsValue): JsResult[RegistrationType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Initial"   => JsSuccess(Initial)
          case "Amendment" => JsSuccess(Amendment)
          case "Dereg"     => JsSuccess(Dereg)
        }
      case e: JsError          => e
    }

    override def writes(o: RegistrationType): JsValue = o match {
      case Initial   => JsString("Initial")
      case Amendment => JsString("Amendment")
      case Dereg     => JsString("Dereg")
    }
  }

  implicit val jsLiteral: JavascriptLiteral[RegistrationType] = new JavascriptLiteral[RegistrationType] {
    override def to(value: RegistrationType): String = value match {
      case Initial   => "Initial"
      case Amendment => "Amendment"
      case Dereg     => "Dereg"
    }
  }
}
