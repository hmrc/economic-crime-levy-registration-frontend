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

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.UtrType.{CtUtr, SaUtr}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class UtrTypeNavigatorSpec extends SpecBase {

  val pageNavigator = new UtrTypePageNavigator()

  "nextPage" should {
    "return a call to the correct UTR entry page in Normal mode" in forAll {
      (registration: Registration, utrType: UtrType) =>
        val otherData           = OtherEntityJourneyData.empty().copy(utrType = Some(utrType))
        val updatedRegistration = registration.copy(optOtherEntityJourneyData = Some(otherData))
        val call                = utrType match {
          case SaUtr => routes.SaUtrController.onPageLoad(NormalMode)
          case CtUtr => routes.CtUtrController.onPageLoad(NormalMode)
        }

        pageNavigator.nextPage(NormalMode, NavigationData(updatedRegistration)) shouldBe
          call
    }

    "return a call to the other entity check your answers page in Check mode" in forAll {
      (registration: Registration) =>
        val otherEntityJourneyData = registration.otherEntityJourneyData
        val call                   = otherEntityJourneyData.utrType match {
          case Some(CtUtr) if otherEntityJourneyData.ctUtr.isEmpty => routes.CtUtrController.onPageLoad(CheckMode)
          case Some(SaUtr) if otherEntityJourneyData.saUtr.isEmpty => routes.SaUtrController.onPageLoad(CheckMode)
          case _                                                   => routes.CheckYourAnswersController.onPageLoad()
        }
        pageNavigator.nextPage(CheckMode, NavigationData(registration)) shouldBe
          call
    }
  }

}
