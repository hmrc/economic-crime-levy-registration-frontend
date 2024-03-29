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

final case class RegistrationSubmittedEmailParameters(
  name: String,
  eclRegistrationReference: String,
  eclRegistrationDate: String,
  dateDue: String,
  isPrimaryContact: String,
  secondContactEmail: Option[String],
  currentFY: Option[String],
  previousFY: Option[String]
)

object RegistrationSubmittedEmailParameters {
  implicit val format: OFormat[RegistrationSubmittedEmailParameters] = Json.format[RegistrationSubmittedEmailParameters]
}

final case class RegistrationSubmittedEmailRequest(
  to: Seq[String],
  templateId: String = RegistrationSubmittedEmailRequest.NormalEntityTemplateId,
  parameters: RegistrationSubmittedEmailParameters,
  force: Boolean = false,
  eventUrl: Option[String] = None
)

object RegistrationSubmittedEmailRequest {
  val NormalEntityTemplateId: String                              = "ecl_registration_submitted"
  val OtherEntityTemplateId: String                               = "ecl_registration_received"
  implicit val format: OFormat[RegistrationSubmittedEmailRequest] = Json.format[RegistrationSubmittedEmailRequest]
}
