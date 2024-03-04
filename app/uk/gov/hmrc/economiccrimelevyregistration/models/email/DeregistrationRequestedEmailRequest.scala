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

package uk.gov.hmrc.economiccrimelevyregistration.models.email

import play.api.libs.json.{Json, OFormat}

case class DeregistrationRequestedEmailRequest(
  to: Seq[String],
  templateId: String = DeregistrationRequestedEmailParameters.DeregistrationTemplateId,
  parameters: DeregistrationRequestedEmailParameters,
  force: Boolean = false,
  eventUrl: Option[String] = None
)

object DeregistrationRequestedEmailRequest {
  implicit val format: OFormat[DeregistrationRequestedEmailRequest] =
    Json.format[DeregistrationRequestedEmailRequest]
}

case class DeregistrationRequestedEmailParameters(
  name: String,
  dateSubmitted: String,
  eclReferenceNumber: String,
  addressLine1: Option[String],
  addressLine2: Option[String],
  addressLine3: Option[String],
  addressLine4: Option[String]
)

object DeregistrationRequestedEmailParameters {
  val DeregistrationTemplateId: String                                 = "ecl_deregistration_requested"
  implicit val format: OFormat[DeregistrationRequestedEmailParameters] =
    Json.format[DeregistrationRequestedEmailParameters]
}
