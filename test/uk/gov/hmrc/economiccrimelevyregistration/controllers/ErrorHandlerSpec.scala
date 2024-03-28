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
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{AddressLookupContinueError, AuditError, DataRetrievalError, ErrorCode, ResponseError, SessionError}

class ErrorHandlerSpec extends SpecBase with ErrorHandler {

  "dataRetrievalErrorConverter" should {

    "return ResponseError.badGateway when DataRetrievalError.BadGateway is converted" in { (errorMessage: String) =>
      val dataRetrievalError = DataRetrievalError.BadGateway(errorMessage, BAD_GATEWAY)

      val result: ResponseError = dataRetrievalErrorConverter.convert(dataRetrievalError)

      result shouldBe ResponseError.badGateway(errorMessage, BAD_GATEWAY)
    }

    "return ResponseError.internalServiceError when DataRetrievalError.InternalUnexpectedError is converted" in {
      val dataRetrievalError = DataRetrievalError.InternalUnexpectedError("Internal server error", None)

      val result: ResponseError = dataRetrievalErrorConverter.convert(dataRetrievalError)

      result shouldBe ResponseError.internalServiceError("Internal server error", ErrorCode.InternalServerError, None)
    }
  }

  "enrolmentStoreErrorConverter" should {
    "return ResponseError.badRequestError when AddressLookupContinueError.BadGateway is converted" in forAll {
      (errorMessage: String) =>
        val addressLookupError = AddressLookupContinueError.BadGateway(errorMessage, BAD_GATEWAY)

        val result: ResponseError = enrolmentStoreErrorConverter.convert(addressLookupError)

        result shouldBe ResponseError.badGateway(errorMessage, BAD_GATEWAY)
    }

    "return ResponseError.internalServiceError when AddressLookupContinueError.InternalUnexpectedError is converted" in {
      val addressLookupError = AddressLookupContinueError.InternalUnexpectedError("Internal server error", None)

      val result: ResponseError = enrolmentStoreErrorConverter.convert(addressLookupError)

      result shouldBe ResponseError.internalServiceError("Internal server error", ErrorCode.InternalServerError, None)
    }

    "auditErrorConverter" should {
      "return ResponseError.internalServiceError when AuditError.InternalUnexpectedError is converted" in forAll {
        (errorMessage: String) =>
          val auditError = AuditError.InternalUnexpectedError(errorMessage, None)

          val result: ResponseError = auditErrorConverter.convert(auditError)

          result shouldBe ResponseError.internalServiceError(errorMessage, ErrorCode.InternalServerError, None)
      }
    }

    "sessionErrorConverter" should {
      "return ResponseError.internalServiceError when SessionError.InternalUnexpectedError is converted" in forAll {
        (errorMessage: String) =>
          val sessionError = SessionError.InternalUnexpectedError(errorMessage, None)

          val result: ResponseError = sessionErrorConverter.convert(sessionError)

          result shouldBe ResponseError.internalServiceError(errorMessage, ErrorCode.InternalServerError, None)
      }

      "return ResponseError.badGateway when SessionError.BadGateway is converted" in forAll { (errorMessage: String) =>
        val sessionError = SessionError.BadGateway(errorMessage, BAD_GATEWAY)

        val result: ResponseError = sessionErrorConverter.convert(sessionError)

        result shouldBe ResponseError.badGateway(errorMessage, BAD_GATEWAY)
      }
    }

  }
}
