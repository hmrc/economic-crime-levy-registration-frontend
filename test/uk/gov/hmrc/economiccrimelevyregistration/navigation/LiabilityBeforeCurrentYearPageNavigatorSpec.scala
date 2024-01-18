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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import scala.concurrent.Future

class LiabilityBeforeCurrentYearPageNavigatorSpec extends SpecBase {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val pageNavigator = new LiabilityBeforeCurrentYearPageNavigator(mockAuditConnector)

  "nextPage" should {
    "return a Call to the not liable page if selected option is 'No' and previous screen was aml activity" in forAll {
      registration: Registration =>
        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        pageNavigator.nextPage(NormalMode, NavigationData(registration, "", false, false, false.toString)) shouldBe
          routes.NotLiableController.youDoNotNeedToRegister()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return a Call to the not liable page if selected option is 'No' and revenue does not meet threshold" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            revenueMeetsThreshold = Some(false)
          )

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        pageNavigator.nextPage(
          NormalMode,
          NavigationData(updatedRegistration, "", true, false, false.toString)
        ) shouldBe
          routes.NotLiableController.youDoNotNeedToRegister()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return a Call to the entity type page if selected option is 'No' and revenue meets threshold" in forAll {
      registration: Registration =>
        val updatedRegistration =
          registration.copy(
            revenueMeetsThreshold = Some(true)
          )

        pageNavigator.nextPage(
          NormalMode,
          NavigationData(updatedRegistration, "", true, false, false.toString)
        ) shouldBe
          routes.EntityTypeController.onPageLoad(NormalMode)
    }

    "return a Call to the AML supervisor page if selected option is 'Yes' and you do not come from revenue page" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          registrationType = Some(Initial)
        )

        pageNavigator.nextPage(
          NormalMode,
          NavigationData(updatedRegistration, "", false, false, true.toString)
        ) shouldBe
          routes.AmlSupervisorController.onPageLoad(NormalMode, Initial, true)
    }

    "return a Call to the entity type page if selected option is 'Yes' abd you come from revenue page" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          registrationType = Some(Initial)
        )

        pageNavigator.nextPage(NormalMode, NavigationData(updatedRegistration, "", true, false, true.toString)) shouldBe
          routes.EntityTypeController.onPageLoad(NormalMode)
    }
  }

}
