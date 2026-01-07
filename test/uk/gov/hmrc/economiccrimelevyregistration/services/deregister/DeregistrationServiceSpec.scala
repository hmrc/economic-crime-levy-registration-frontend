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

package uk.gov.hmrc.economiccrimelevyregistration.services.deregister

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.deregister.DeregistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.http.UpstreamErrorResponse
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class DeregistrationServiceSpec extends SpecBase {
  val mockDeregistrationConnector: DeregistrationConnector = mock[DeregistrationConnector]
  val service                                              = new DeregistrationService(
    mockDeregistrationConnector
  )

  val e = new Exception("error")

  "getOrCreate" should {
    "return a created deregistration when one does not exist" in forAll { (internalId: String) =>
      val emptyRegistration = Deregistration.empty(internalId)
      when(mockDeregistrationConnector.getDeregistration(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND)))

      when(mockDeregistrationConnector.upsertDeregistration(any())(any()))
        .thenReturn(Future.successful(()))

      val result = await(service.getOrCreate(internalId).value)
      result shouldBe Right(emptyRegistration)
    }

    "return an existing deregistration" in forAll { (deregistration: Deregistration) =>
      when(mockDeregistrationConnector.getDeregistration(any())(any()))
        .thenReturn(Future.successful(deregistration))

      val result = await(service.getOrCreate(deregistration.internalId).value)
      result shouldBe Right(deregistration)
    }

    "return an error if failed" in forAll { (internalId: String) =>
      when(mockDeregistrationConnector.getDeregistration(any())(any()))
        .thenReturn(Future.failed(e))

      val result = await(service.getOrCreate(internalId).value)
      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(e.getMessage, Some(e)))
    }
  }

  "upsert" should {
    "return an upserted deregistration" in forAll { (deregistration: Deregistration) =>
      when(mockDeregistrationConnector.upsertDeregistration(any())(any()))
        .thenReturn(Future.successful(()))

      val result = await(service.upsert(deregistration).value)
      result shouldBe Right(())
    }

    "return an error if connector returns an Upstream5xxResponse" in forAll { (deregistration: Deregistration) =>
      when(mockDeregistrationConnector.upsertDeregistration(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

      val result = await(service.upsert(deregistration).value)
      result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return an error if connector returns an Upstream4xxResponse" in forAll { (deregistration: Deregistration) =>
      when(mockDeregistrationConnector.upsertDeregistration(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", NOT_FOUND)))

      val result = await(service.upsert(deregistration).value)
      result shouldBe Left(DataRetrievalError.BadGateway("Error", NOT_FOUND))
    }

    "return an error if failed" in forAll { (deregistration: Deregistration) =>
      when(mockDeregistrationConnector.upsertDeregistration(any())(any()))
        .thenReturn(Future.failed(e))

      val result = await(service.upsert(deregistration).value)
      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(e.getMessage, Some(e)))
    }
  }

  "delete" should {
    "return normally" in forAll { (internalId: String) =>
      when(mockDeregistrationConnector.deleteDeregistration(any())(any()))
        .thenReturn(Future.successful(()))

      val result = await(service.delete(internalId).value)
      result shouldBe Right(())
    }

    "return an error if failed" in forAll { (internalId: String) =>
      when(mockDeregistrationConnector.deleteDeregistration(any())(any()))
        .thenReturn(Future.failed(e))

      val result = await(service.delete(internalId).value)
      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(e.getMessage, Some(e)))
    }
  }

  "submit" should {
    "return a unit when the submission is successful" in {
      when(mockDeregistrationConnector.submitDeregistration(any())(any()))
        .thenReturn(Future.successful(Right(())))

      val result = await(service.submit(testInternalId).value)
      result shouldBe Right(())
    }

    "return an error if connector returns an Upstream5xxResponse" in {
      when(mockDeregistrationConnector.submitDeregistration(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR)))

      val result = await(service.submit(testInternalId).value)
      result shouldBe Left(DataRetrievalError.BadGateway("Error", INTERNAL_SERVER_ERROR))
    }

    "return an error if connector returns an Upstream4xxResponse" in {
      when(mockDeregistrationConnector.submitDeregistration(any())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error", NOT_FOUND)))

      val result = await(service.submit(testInternalId).value)
      result shouldBe Left(DataRetrievalError.BadGateway("Error", NOT_FOUND))
    }

    "return an error if an exception is thrown" in forAll { (internalId: String) =>
      when(mockDeregistrationConnector.submitDeregistration(any())(any()))
        .thenReturn(Future.failed(e))

      val result = await(service.submit(internalId).value)
      result shouldBe Left(DataRetrievalError.InternalUnexpectedError(e.getMessage, Some(e)))
    }

  }
}
