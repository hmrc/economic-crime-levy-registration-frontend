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
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

class RelevantApLengthPageNavigatorSpec extends SpecBase {

  val pageNavigator = new RelevantApLengthPageNavigator

  "nextPage" should {
    "return a Call to the UK revenue page in NormalMode" in forAll { (registration: Registration, length: Int) =>
      val updatedRegistration = registration.copy(relevantApLength = Some(length))

      pageNavigator.nextPage(NormalMode, updatedRegistration) shouldBe routes.UkRevenueController.onPageLoad()
    }
  }

}
