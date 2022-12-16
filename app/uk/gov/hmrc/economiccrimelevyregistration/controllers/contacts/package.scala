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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

package object contacts {
  val firstContactName: RegistrationDataRequest[_] => String = request =>
    request.registration.contacts.firstContactDetails.name
      .getOrElse(throw new IllegalStateException("First contact name not found in registration data"))

  val secondContactName: RegistrationDataRequest[_] => String = request =>
    request.registration.contacts.secondContactDetails.name
      .getOrElse(throw new IllegalStateException("Second contact name not found in registration data"))
}
