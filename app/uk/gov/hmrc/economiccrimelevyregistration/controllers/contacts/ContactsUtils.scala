/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts

import uk.gov.hmrc.economiccrimelevyregistration.models.EclAddress
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest

trait ContactsUtils {

  implicit class FirstAndLastNameHelper(request: RegistrationDataRequest[_]) {

    def firstContactName: Either[DataRetrievalError, String] =
      request.registration.contacts.firstContactDetails.name match {
        case Some(value) => Right(value)
        case None        => Left(DataRetrievalError.FieldNotFound("No first contact name found in registration data"))
      }

    def secondContactName: Either[DataRetrievalError, String] =
      request.registration.contacts.secondContactDetails.name match {
        case Some(value) => Right(value)
        case None        => Left(DataRetrievalError.FieldNotFound("No second contact name found in registration data"))
      }
  }

  implicit class ContactAddressHelper(request: RegistrationDataRequest[_]) {

    def contactAddress: Either[DataRetrievalError, EclAddress] =
      request.registration.grsAddressToEclAddress match {
        case Some(value)  => Right(value)
        case None         => Left(DataRetrievalError.FieldNotFound("No registered office address found in registration data"))
      }
  }
}
