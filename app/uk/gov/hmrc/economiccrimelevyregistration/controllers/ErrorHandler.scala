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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import cats.data.EitherT
import play.api.Logging
import uk.gov.hmrc.economiccrimelevyregistration.models.errors._

import scala.concurrent.{ExecutionContext, Future}

trait ErrorHandler extends Logging {

  implicit class EitherTErrorConvertor[E, R](value: EitherT[Future, E, R]) {

    def asResponseError(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, ResponseError, R] =
      value.leftMap(c.convert)
  }

  implicit class EitherErrorConvertor[E, R](value: Either[E, R]) {
    def asResponseError(implicit converter: Converter[E]): Either[ResponseError, R] =
      value.left.map(converter.convert)
  }

  trait Converter[E] {
    def convert(error: E): ResponseError
  }

  implicit val enrolmentStoreErrorConverter: Converter[AddressLookupContinueError] = {
    case AddressLookupContinueError.BadGateway(cause, statusCode)           =>
      ResponseError.badGateway(cause, statusCode)
    case AddressLookupContinueError.InternalUnexpectedError(message, cause) =>
      ResponseError.internalServiceError(message = message, cause = cause)
  }

  implicit val auditErrorConverter: Converter[AuditError] = { case AuditError.InternalUnexpectedError(message, cause) =>
    ResponseError.internalServiceError(message = message, cause = cause)
  }

  implicit val dataRetrievalErrorConverter: Converter[DataRetrievalError] = {
    case DataRetrievalError.BadGateway(cause, statusCode)           =>
      ResponseError.badGateway(cause, statusCode)
    case DataRetrievalError.InternalUnexpectedError(message, cause) =>
      ResponseError.internalServiceError(message = message, cause = cause)
  }

  implicit val sessionErrorConverter: Converter[SessionError] = {
    case SessionError.BadGateway(cause, statusCode)           =>
      ResponseError.badGateway(cause, statusCode)
    case SessionError.InternalUnexpectedError(message, cause) =>
      ResponseError.internalServiceError(message = message, cause = cause)
  }

}
