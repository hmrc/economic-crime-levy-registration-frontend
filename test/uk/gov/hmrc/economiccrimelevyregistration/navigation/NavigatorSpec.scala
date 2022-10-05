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

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.pages.Page

class NavigatorSpec extends SpecBase {

  val navigator = new Navigator

  "nextPage" should {
    "go from a page that doesn't exist in the route map to Index in NormalMode" in {
      case object UnknownPage extends Page
      navigator.nextPage(UnknownPage, NormalMode, testRegistration) shouldBe routes.StartController.onPageLoad()
    }

    "go from a page that doesn't exist in the edit route map to CheckYourAnswers in CheckMode" in {
      case object UnknownPage extends Page
      navigator.nextPage(
        UnknownPage,
        CheckMode,
        testRegistration
      ) shouldBe routes.CheckYourAnswersController.onPageLoad()
    }
  }
}
