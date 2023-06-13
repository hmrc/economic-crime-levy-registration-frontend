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

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.http.HttpVerbs.GET

class CharityRegistrationNumberPageNavigatorSpec extends SpecBase {

  val pageNavigator = new CharityRegistrationNumberPageNavigator()

  "nextPage" should {
    "return a Call to the business sector page" in forAll { (registration: Registration, mode: Mode) =>
      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          entityType = Some(Charity),
          charityRegistrationNumber = Some("test")
        )

      val updatedRegistration: Registration =
        registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

      pageNavigator.nextPage(mode, updatedRegistration) shouldBe Call(
        GET,
        mode match {
          case NormalMode => routes.CompanyRegistrationNumberController.onPageLoad(mode).url
          case CheckMode  => routes.OtherEntityCheckYourAnswersController.onPageLoad().url
        }
      )
    }
  }

}
