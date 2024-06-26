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
import play.api.mvc.{JavascriptLiteral, QueryStringBindable}

sealed abstract class RegistrationType(value: String)

object RegistrationType {

  case object Amendment extends RegistrationType("Amendment")
  case object DeRegistration extends RegistrationType("DeRegistration")
  case object Initial extends RegistrationType("Initial")

  lazy val values: Set[RegistrationType] = Set(Initial, Amendment, DeRegistration)

  implicit def queryStringBindable(implicit
    stringBinder: QueryStringBindable[String]
  ): QueryStringBindable[RegistrationType] = new QueryStringBindable[RegistrationType] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, RegistrationType]] =
      stringBinder.bind("registrationType", params).map {
        case Right(mode) =>
          mode match {
            case "Initial"        => Right(Initial)
            case "Amendment"      => Right(Amendment)
            case "DeRegistration" => Right(DeRegistration)
            case _                => Left("Unable to bind to a registration type")
          }
        case _           =>
          Left("Unable to bind to a registration type")
      }

    override def unbind(key: String, registrationType: RegistrationType): String =
      stringBinder.unbind("registrationType", registrationType.toString)
  }

  implicit val format: Format[RegistrationType] = new Format[RegistrationType] {
    override def reads(json: JsValue): JsResult[RegistrationType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "Initial"        => JsSuccess(Initial)
          case "Amendment"      => JsSuccess(Amendment)
          case "DeRegistration" => JsSuccess(DeRegistration)
        }
      case e: JsError          => e
    }

    override def writes(o: RegistrationType): JsValue = o match {
      case Initial        => JsString("Initial")
      case Amendment      => JsString("Amendment")
      case DeRegistration => JsString("DeRegistration")
    }
  }

  implicit val jsLiteral: JavascriptLiteral[RegistrationType] = new JavascriptLiteral[RegistrationType] {
    override def to(value: RegistrationType): String = value match {
      case Initial        => "Initial"
      case Amendment      => "Amendment"
      case DeRegistration => "DeRegistration"
    }
  }
}
