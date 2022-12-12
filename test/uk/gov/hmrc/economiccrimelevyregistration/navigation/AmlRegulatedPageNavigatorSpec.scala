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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

class AmlRegulatedPageNavigatorSpec extends SpecBase {

  val pageNavigator = new AmlRegulatedPageNavigator

  "nextPage" should {
    "return a Call to the Aml start date page from the Aml regulated activity started in current financial year page in NormalMode when the 'Yes' option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(startedAmlRegulatedActivityInCurrentFy = Some(true))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.AmlRegulatedActivityStartDateController
          .onPageLoad()
    }

    "return a Call to the business sector page from the Aml regulated activity started in current financial year page in NormalMode when the 'No' option is selected" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(startedAmlRegulatedActivityInCurrentFy = Some(false))

        pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.BusinessSectorController.onPageLoad()
    }
  }

}
