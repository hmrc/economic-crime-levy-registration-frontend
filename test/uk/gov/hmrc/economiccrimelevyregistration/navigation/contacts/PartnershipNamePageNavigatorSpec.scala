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

package uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode, Registration}

class PartnershipNamePageNavigatorSpec extends SpecBase {

  val pageNavigator = new PartnershipNamePageNavigator()

  "nextPage" should {
    "return a Call to the business sector page in NormalMode" in forAll {
      (registration: Registration, partnershipName: String) =>
        val updatedRegistration: Registration =
          registration.copy(partnershipName = Some(partnershipName))

        pageNavigator.nextPage(NormalMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.BusinessSectorController.onPageLoad(NormalMode)
    }

    "return a Call to the check your answers page in CheckMode" in forAll {
      (registration: Registration, partnershipName: String) =>
        val updatedRegistration: Registration =
          registration.copy(partnershipName = Some(partnershipName))

        pageNavigator.nextPage(CheckMode, EclRegistrationModel(updatedRegistration)) shouldBe
          routes.CheckYourAnswersController.onPageLoad()
    }

    Seq(NormalMode, CheckMode).foreach { mode =>
      s"return a call to the answers are invalid page when there is no partnership name present in $mode " in forAll {
        (registration: Registration) =>
          val updatedRegistration: Registration =
            registration.copy(partnershipName = None)

          pageNavigator.nextPage(mode, EclRegistrationModel(updatedRegistration)) shouldBe
            routes.NotableErrorController.answersAreInvalid()
      }
    }

  }

}
