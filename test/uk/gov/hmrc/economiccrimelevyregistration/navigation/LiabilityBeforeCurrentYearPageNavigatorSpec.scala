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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class LiabilityBeforeCurrentYearPageNavigatorSpec extends SpecBase {

  val mockAuditConnector = mock[AuditConnector]

  val pageNavigator = new LiabilityBeforeCurrentYearPageNavigator(mockAuditConnector)

  "nextPage" should {
    "return a Call to the not liable page if selected option is 'No' and no revenue has been entered" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            relevantApRevenue = None
          )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        pageNavigator.nextPage(false, updatedRegistration) shouldBe routes.NotLiableController.youDoNotNeedToRegister()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return a Call to the not liable page if selected option is 'No' and amount does not meet threshold" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            relevantApRevenue = Some(random[Long]),
            revenueMeetsThreshold = Some(false)
          )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        pageNavigator.nextPage(false, updatedRegistration) shouldBe routes.NotLiableController.youDoNotNeedToRegister()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return a Call to the entity type page if selected option is 'No' and amount meets threshold" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            relevantApRevenue = Some(random[Long]),
            revenueMeetsThreshold = Some(true)
          )

        pageNavigator.nextPage(false, updatedRegistration) shouldBe routes.EntityTypeController.onPageLoad(NormalMode)
    }

    "return a Call to the entity type page if selected option is 'Yes'" in forAll { registration: Registration =>
      pageNavigator.nextPage(true, registration) shouldBe routes.EntityTypeController.onPageLoad(NormalMode)
    }
  }

}
