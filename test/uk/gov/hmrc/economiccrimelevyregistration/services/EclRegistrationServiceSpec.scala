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
import org.scalacheck.Arbitrary
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NOT_FOUND}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration}
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial

class EclRegistrationServiceSpec extends SpecBase {
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val mockAuditService: AuditService                         = mock[AuditService]
  val mockIncorporatedEntityIdentificationFrontendConnector  = mock[IncorporatedEntityIdentificationFrontendConnector]
  val mockSoleTraderIdentificationFrontendConnector          = mock[SoleTraderIdentificationFrontendConnector]
  val mockPartnershipIdentificationFrontendConnector         = mock[PartnershipIdentificationFrontendConnector]
  val service                                                = new EclRegistrationService(
    mockEclRegistrationConnector,
    mockIncorporatedEntityIdentificationFrontendConnector,
    mockSoleTraderIdentificationFrontendConnector,
    mockPartnershipIdentificationFrontendConnector,
    mockAuditService,
    appConfig
  )

  "getOrCreateRegistration" should {
    "return a created registration when one does not exist" in forAll {
      (internalId: String, registration: Registration) =>
        val emptyRegistration = Registration.empty(internalId).copy(registrationType = Some(Initial))
        when(mockEclRegistrationConnector.getRegistration(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("Not found", NOT_FOUND)))

        when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
          .thenReturn(Future.successful(emptyRegistration))

        val result = await(service.getOrCreate(internalId).value)
        result shouldBe Right(emptyRegistration)
    }

    "return an existing registration" in forAll { (internalId: String, registration: Registration) =>
      when(mockEclRegistrationConnector.getRegistration(any())(any()))
        .thenReturn(Future.successful(registration))

      val result = await(service.getOrCreate(internalId).value)
      result shouldBe Right(registration)

    }
  }

  "getSubscription" should {
    "return valid subscription response when eclReference is passed" in forAll(
      (Arbitrary.arbitrary[GetSubscriptionResponse]),
      nonEmptyString
    ) { (getSubscriptionResponse: GetSubscriptionResponse, eclReference: String) =>
      when(mockEclRegistrationConnector.getSubscription(eclReference))
        .thenReturn(Future.successful(getSubscriptionResponse))

      val result = await(service.getSubscription(eclReference).value)

      result shouldBe Right(getSubscriptionResponse)
    }

    "return error when call to connector fails" in forAll(
      nonEmptyString
    ) { (eclReference: String) =>
      when(mockEclRegistrationConnector.getSubscription(ArgumentMatchers.eq(eclReference))(any()))
        .thenReturn((Future.failed(UpstreamErrorResponse("Error", INTERNAL_SERVER_ERROR))))

      val result = await(
        service
          .getSubscription(eclReference)
          .value
      )

      result shouldBe Left(DataRetrievalError.BadGateway("Error", 500))
    }
  }
}
