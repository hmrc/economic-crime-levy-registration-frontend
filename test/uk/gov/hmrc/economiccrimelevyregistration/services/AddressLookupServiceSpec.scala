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

package uk.gov.hmrc.economiccrimelevyregistration.services

import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.AddressLookupContinueError
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup.AlfAddressData
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class AddressLookupServiceSpec extends SpecBase {
  val mockAddressLookupFrontEndConnector: AddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]
  val service                                                            = new AddressLookupService(mockAddressLookupFrontEndConnector)

  "initJourney" should {
    "return a string when the call to the connector is successful" in forAll {
      (journeyUrl: String, isUkAddress: Boolean, mode: Mode) =>
        when(mockAddressLookupFrontEndConnector.initJourney(any(), any())(any()))
          .thenReturn(Future.successful(journeyUrl))

        val result = await(service.initJourney(isUkAddress, mode).value)

        result shouldBe Right(journeyUrl)
    }

    "return an error when the call to the connector returns an Upstream5xxResponse" in forAll {
      (isUkAddress: Boolean, mode: Mode) =>
        when(mockAddressLookupFrontEndConnector.initJourney(any(), any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Address look up failed", INTERNAL_SERVER_ERROR)))

        val result = await(service.initJourney(isUkAddress, mode).value)

        result shouldBe Left(AddressLookupContinueError.BadGateway("Address look up failed", INTERNAL_SERVER_ERROR))
    }

    "return an error when the call to the connector returns an Upstream4xxResponse" in forAll {
      (isUkAddress: Boolean, mode: Mode) =>
        when(mockAddressLookupFrontEndConnector.initJourney(any(), any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Address look up failed", NOT_FOUND)))

        val result = await(service.initJourney(isUkAddress, mode).value)

        result shouldBe Left(AddressLookupContinueError.BadGateway("Address look up failed", NOT_FOUND))
    }

    "return an error when the call to the connector throws a non fatal exception" in forAll {
      (isUkAddress: Boolean, mode: Mode) =>
        val exception = new Exception("error")
        when(mockAddressLookupFrontEndConnector.initJourney(any(), any())(any()))
          .thenReturn(Future.failed(exception))

        val result = await(service.initJourney(isUkAddress, mode).value)

        result shouldBe Left(
          AddressLookupContinueError.InternalUnexpectedError(exception.getMessage, Some(exception))
        )
    }

  }

  "getAddress" should {
    "return AlfAddressData when successful" in forAll { (journeyId: String, addressData: AlfAddressData) =>
      when(mockAddressLookupFrontEndConnector.getAddress(anyString())(any()))
        .thenReturn(Future.successful(addressData))

      val result = await(service.getAddress(journeyId).value)

      result shouldBe Right(addressData)
    }

    "return an error when the call to the connector returns an Upstream5xxResponse" in forAll { journeyId: String =>
      when(mockAddressLookupFrontEndConnector.getAddress(anyString())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Failed to get address", INTERNAL_SERVER_ERROR)))

      val result = await(service.getAddress(journeyId).value)

      result shouldBe Left(AddressLookupContinueError.BadGateway("Failed to get address", INTERNAL_SERVER_ERROR))
    }

    "return an error when the call to the connector returns an Upstream4xxResponse" in forAll { journeyId: String =>
      when(mockAddressLookupFrontEndConnector.getAddress(anyString())(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Failed to get address", NOT_FOUND)))

      val result = await(service.getAddress(journeyId).value)

      result shouldBe Left(AddressLookupContinueError.BadGateway("Failed to get address", NOT_FOUND))
    }

    "return an error when the call to the connector throws a non fatal exception" in forAll { journeyId: String =>
      val exception = new Exception("error")
      when(mockAddressLookupFrontEndConnector.getAddress(anyString())(any()))
        .thenReturn(Future.failed(exception))

      val result = await(service.getAddress(journeyId).value)

      result shouldBe Left(
        AddressLookupContinueError.InternalUnexpectedError(exception.getMessage, Some(exception))
      )
    }

  }
}
