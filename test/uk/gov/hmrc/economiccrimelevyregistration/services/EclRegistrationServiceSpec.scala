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
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, Registration}
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class EclRegistrationServiceSpec extends SpecBase {
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val mockAuditConnector: AuditConnector                     = mock[AuditConnector]
  val service                                                = new EclRegistrationService(mockEclRegistrationConnector, mockAuditConnector, appConfig)

  "getOrCreateRegistration" should {
    "return a created registration when one does not exist" in forAll {
      (internalId: String, registration: Registration) =>
        when(mockEclRegistrationConnector.getRegistration(any())(any()))
          .thenReturn(Future.successful(None))

        when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
          .thenReturn(Future.successful(registration))

        val result = await(service.getOrCreateRegistration(internalId))
        result shouldBe registration

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return an existing registration" in forAll { (internalId: String, registration: Registration) =>
      when(mockEclRegistrationConnector.getRegistration(any())(any()))
        .thenReturn(Future.successful(Some(registration)))

      val result = await(service.getOrCreateRegistration(internalId))
      result shouldBe registration
    }
  }

  "getSubscription" should {
    "return valid subscription response when eclReference is passed" in forAll(
      (Arbitrary.arbitrary[GetSubscriptionResponse]),
      nonEmptyString
    ) { (getSubscriptionResponse: GetSubscriptionResponse, eclReference: String) =>
      when(mockEclRegistrationConnector.getSubscription(eclReference))
        .thenReturn(Future.successful(getSubscriptionResponse))

      val result = await(service.getSubscription(eclReference))

      result shouldBe getSubscriptionResponse
    }
  }
}
