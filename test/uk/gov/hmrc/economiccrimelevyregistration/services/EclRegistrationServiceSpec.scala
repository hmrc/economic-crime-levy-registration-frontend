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

package uk.gov.hmrc.economiccrimelevyregistration.services

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector

import scala.concurrent.Future

class EclRegistrationServiceSpec extends SpecBase {
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val service = new EclRegistrationService(mockEclRegistrationConnector)

  "getOrCreateRegistration" should {
    "return a created registration when one does not exist" in {
      when(mockEclRegistrationConnector.getRegistration(any())(any()))
        .thenReturn(Future.successful(None))

      when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
        .thenReturn(Future.successful(emptyRegistration))

      val result = await(service.getOrCreateRegistration(internalId))
      result shouldBe emptyRegistration
    }

    "return an existing registration" in {
      when(mockEclRegistrationConnector.getRegistration(any())(any()))
        .thenReturn(Future.successful(Some(emptyRegistration)))

      val result = await(service.getOrCreateRegistration(internalId))
      result shouldBe emptyRegistration
    }
  }
}
