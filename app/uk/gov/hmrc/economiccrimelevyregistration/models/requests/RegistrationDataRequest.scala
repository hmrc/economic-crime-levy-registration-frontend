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

package uk.gov.hmrc.economiccrimelevyregistration.models.requests

import play.api.mvc.{Request, WrappedRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, EntityType, Registration, RegistrationAdditionalInfo}

case class RegistrationDataRequest[A](
  request: Request[A],
  internalId: String,
  registration: Registration,
  additionalInfo: Option[RegistrationAdditionalInfo],
  eclRegistrationReference: Option[String]
) extends WrappedRequest[A](request) {

  val firstContactNameOrError: Either[DataRetrievalError, String] =
    registration.contacts.firstContactDetails.name match {
      case Some(value) => Right(value)
      case None        =>
        Left(DataRetrievalError.InternalUnexpectedError("No first contact name found in registration data", None))
    }

  val secondContactNameOrError: Either[DataRetrievalError, String] =
    registration.contacts.secondContactDetails.name match {
      case Some(value) => Right(value)
      case None        =>
        Left(DataRetrievalError.InternalUnexpectedError("No second contact name found in registration data", None))
    }

  val eclAddressOrError: Either[DataRetrievalError, EclAddress] =
    registration.grsAddressToEclAddress match {
      case Some(value) => Right(value)
      case None        =>
        Left(
          DataRetrievalError.InternalUnexpectedError("No registered office address found in registration data", None)
        )
    }

  val entityTypeOrError: Either[DataRetrievalError, EntityType] =
    registration.entityType match {
      case Some(value) => Right(value)
      case None        => Left(DataRetrievalError.InternalUnexpectedError("Entity type not found in registration data", None))
    }

}
