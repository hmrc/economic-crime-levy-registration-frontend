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

package uk.gov.hmrc.economiccrimelevyregistration.models.grs

import play.api.i18n.MessagesApi
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.models.Languages._

final case class ServiceNameLabels(en: OptServiceName, cy: OptServiceName)

object ServiceNameLabels {
  def apply()(implicit messagesApi: MessagesApi): ServiceNameLabels =
    ServiceNameLabels(
      OptServiceName(optServiceName = messagesApi("service.name")(english)),
      OptServiceName(optServiceName = messagesApi("service.name")(welsh))
    )

  implicit val format: OFormat[ServiceNameLabels] =
    Json.format[ServiceNameLabels]
}

final case class OptServiceName(optServiceName: String)

object OptServiceName {
  implicit val format: OFormat[OptServiceName] =
    Json.format[OptServiceName]
}
