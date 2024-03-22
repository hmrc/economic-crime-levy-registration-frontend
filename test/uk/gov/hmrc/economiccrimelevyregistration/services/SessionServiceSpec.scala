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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, NOT_FOUND}
import play.api.mvc.Session
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.SessionDataConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionData
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SessionError
import uk.gov.hmrc.http.client.RequestBuilder

import scala.concurrent.Future
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.http.UpstreamErrorResponse

class SessionServiceSpec extends SpecBase {

  val mockSessionDataConnector           = mock[SessionDataConnector]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val service: SessionService            = new SessionService(mockSessionDataConnector)
  val testKey: String                    = "testKey"
  val testValue: String                  = "testValue"
  val validData: Map[String, String]     = Map(testKey -> testValue)
  val invalidData: Map[String, String]   = Map("test" -> "empty")

  "get" should {
    "return a value from the key if the key exists in the session" in forAll {
      (sessionData: SessionData, session: Session, internalId: String) =>
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(sessionData))

        val updatedSession                       = session.copy(validData)
        val result: Either[SessionError, String] = await(service.get(updatedSession, internalId, testKey).value)

        result shouldBe Right(testValue)
    }

    "return a value from the key from the SessionData if the key exists in the SessionData but not in the Session" in forAll {
      (sessionData: SessionData, session: Session, internalId: String) =>
        val updatedSessionData = sessionData.copy(internalId, validData)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(updatedSessionData))

        val result: Either[SessionError, String] = await(service.get(session, internalId, testKey).value)

        result shouldBe Right(testValue)
    }

    "return a SessionError when the key does not exist in the Session OR the SessionData" in forAll {
      (sessionData: SessionData, session: Session, internalId: String) =>
        val updatedSessionData = sessionData.copy(internalId = internalId, values = invalidData)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(updatedSessionData))

        val result: Either[SessionError, String] = await(service.get(session, internalId, testKey).value)

        result shouldBe Left(SessionError.InternalUnexpectedError(s"Key not found in session: $testKey", None))
    }

    "return a SessionError.BadGateway when the call to the SessionDataConnector fails" in forAll {
      (session: Session, internalId: String) =>
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Error retrieving session data", BAD_REQUEST)))

        val result: Either[SessionError, String] = await(service.get(session, internalId, testKey).value)

        result shouldBe Left(SessionError.BadGateway("Error retrieving session data", BAD_REQUEST))
    }

    "return an InternalUnexpectedError when a NonFatal exception is thrown when calling the connector" in forAll {
      (session: Session, internalId: String) =>
        val exception = new Exception("exception thrown while retrieving session data")
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.failed(exception))

        val result: Either[SessionError, String] = await(service.get(session, internalId, testKey).value)

        result shouldBe Left(SessionError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

  }

  "getOptional" should {
    "return Some(value) from the key if the key exists in the session" in forAll {
      (sessionData: SessionData, session: Session, internalId: String) =>
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(sessionData))

        val updatedSession                               = session.copy(data = validData)
        val result: Either[SessionError, Option[String]] =
          await(service.getOptional(updatedSession, internalId, testKey).value)

        result shouldBe Right(Some(testValue))
    }

    "return Some(value) from the key from the SessionData if the key exists in the SessionData but not in the Session" in forAll {
      (sessionData: SessionData, session: Session, internalId: String) =>
        val updatedSession                               = session.copy(data = invalidData)
        val updatedSessionData                           = sessionData.copy(internalId = internalId, values = validData)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(updatedSessionData))
        val result: Either[SessionError, Option[String]] =
          await(service.getOptional(updatedSession, internalId, testKey).value)

        result shouldBe Right(Some("testValue"))
    }

    "return None if the key does not exist in either the SessionData or the Session" in forAll {
      (sessionData: SessionData, session: Session, internalId: String) =>
        val updatedSession     = session.copy(data = invalidData)
        val updatedSessionData = sessionData.copy(internalId = internalId, values = invalidData)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(updatedSessionData))

        val result: Either[SessionError, Option[String]] =
          await(service.getOptional(updatedSession, internalId, testKey).value)

        result shouldBe Right(None)
    }
  }

  "delete" should {
    "return a unit when the connector successfully deletes the session" in forAll { internalId: String =>
      when(mockSessionDataConnector.delete(ArgumentMatchers.eq(internalId))(any()))
        .thenReturn(Future.successful(()))

      val result: Either[SessionError, Unit] = await(service.delete(internalId).value)

      result shouldBe Right(())
    }

    "return a SessionError when the connector fails to delete the session" in forAll { internalId: String =>
      when(mockSessionDataConnector.delete(ArgumentMatchers.eq(internalId))(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Error deleting session", BAD_REQUEST)))

      val result: Either[SessionError, Unit] = await(service.delete(internalId).value)

      result shouldBe Left(SessionError.BadGateway("Error deleting session", BAD_REQUEST))
    }

    "return an InternalUnexpectedError when a non fatal exception is thrown" in forAll { internalId: String =>
      val exception = new Exception("Exception thrown whilst trying to delete the session")
      when(mockSessionDataConnector.delete(ArgumentMatchers.eq(internalId))(any()))
        .thenReturn(Future.failed(exception))

      val result: Either[SessionError, Unit] = await(service.delete(internalId).value)

      result shouldBe Left(SessionError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }

  }

  "upsert" should {
    "return a unit when the connector successfully upserts the session" in forAll {
      (internalId: String, sessionData: SessionData) =>
        val updatedSessionData = sessionData.copy(internalId = internalId)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(updatedSessionData))
        when(mockSessionDataConnector.upsert(ArgumentMatchers.eq(updatedSessionData))(any()))
          .thenReturn(Future.successful(()))

        val result: Either[SessionError, Unit] = await(service.upsert(updatedSessionData).value)

        result shouldBe Right(())
        reset(mockSessionDataConnector)
    }

    "return a SessionError when the connector returns an Upstream5xxResponse" in forAll {
      (internalId: String, sessionData: SessionData) =>
        val updatedSessionData = sessionData.copy(internalId = internalId)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(sessionData))
        when(mockSessionDataConnector.upsert(ArgumentMatchers.eq(updatedSessionData))(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Failed to upsert sessionData", INTERNAL_SERVER_ERROR)))

        val result: Either[SessionError, Unit] = await(service.upsert(updatedSessionData).value)

        result shouldBe Left(SessionError.BadGateway("Failed to upsert sessionData", INTERNAL_SERVER_ERROR))
        reset(mockSessionDataConnector)
    }

    "return a SessionError when the connector returns an Upstream4xxResponse" in forAll {
      (internalId: String, sessionData: SessionData) =>
        val updatedSessionData = sessionData.copy(internalId = internalId)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(sessionData))
        when(mockSessionDataConnector.upsert(ArgumentMatchers.eq(updatedSessionData))(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Failed to upsert session data", BAD_REQUEST)))

        val result: Either[SessionError, Unit] = await(service.upsert(updatedSessionData).value)

        result shouldBe Left(SessionError.BadGateway("Failed to upsert session data", BAD_REQUEST))
        reset(mockSessionDataConnector)
    }

    "return an InternalUnexpectedError when the connector returns an Exception" in forAll {
      (internalId: String, sessionData: SessionData) =>
        val exception          = new Exception("Exception")
        val updatedSessionData = sessionData.copy(internalId = internalId)
        when(mockSessionDataConnector.get(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(Future.successful(sessionData))
        when(mockSessionDataConnector.upsert(ArgumentMatchers.eq(updatedSessionData))(any()))
          .thenReturn(Future.failed(exception))

        val result: Either[SessionError, Unit] = await(service.upsert(updatedSessionData).value)
        result shouldBe Left(SessionError.InternalUnexpectedError(exception.getMessage, Some(exception)))
    }
  }
}
