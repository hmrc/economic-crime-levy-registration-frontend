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
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.audit.http.connector.AuditResult.Success

import scala.concurrent.Future

class UkRevenuePageNavigatorSpec extends SpecBase {

  val mockAuditConnector: AuditConnector = mock[AuditConnector]

  val pageNavigator = new UkRevenuePageNavigator(mockAuditConnector)

  "nextPage" should {
    "return a Call to the entity type page in NormalMode when the revenue meets threshold flag is true" in forAll {
      (registration: Registration, ukRevenue: Long) =>
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue), revenueMeetsThreshold = Some(true))

        await(pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)) shouldBe routes.EntityTypeController
          .onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode when the revenue meets threshold flag is true" in forAll {
      (registration: Registration, ukRevenue: Long) =>
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue), revenueMeetsThreshold = Some(true))

        await(
          pageNavigator.nextPage(CheckMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }

    "return a Call to the not liable page in either mode when the revenue meets threshold flag is false" in forAll {
      (registration: Registration, ukRevenue: Long, mode: Mode) =>
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue), revenueMeetsThreshold = Some(false))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any())).thenReturn(Future.successful(Success))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe routes.NotLiableController
          .notLiable()
    }
  }

}
