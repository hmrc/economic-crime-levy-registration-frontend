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

import play.api.libs.json.{Json, OFormat}

final case class Contacts(
  firstContactDetails: ContactDetails,
  secondContact: Option[Boolean],
  secondContactDetails: ContactDetails
)

object Contacts {
  def empty: Contacts = Contacts(ContactDetails.empty, None, ContactDetails.empty)

  implicit val format: OFormat[Contacts] = Json.format[Contacts]
}

final case class ContactDetails(
  name: Option[String],
  role: Option[String],
  emailAddress: Option[String],
  telephoneNumber: Option[String]
)

object ContactDetails {
  def empty: ContactDetails = ContactDetails(None, None, None, None)

  implicit val format: OFormat[ContactDetails] = Json.format[ContactDetails]
}
