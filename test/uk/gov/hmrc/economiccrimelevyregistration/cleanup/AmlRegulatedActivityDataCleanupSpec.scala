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

package uk.gov.hmrc.economiccrimelevyregistration.cleanup

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

class AmlRegulatedActivityDataCleanupSpec extends SpecBase {

  val dataCleaner = new AmlRegulatedActivityDataCleanup

  "cleanup" should {
    "return a registration with no answers apart from the AML regulated activity answer when the answer is no" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(false))

        dataCleaner.cleanup(updatedRegistration) shouldBe Registration
          .empty(registration.internalId)
          .copy(carriedOutAmlRegulatedActivityInCurrentFy =
            updatedRegistration.carriedOutAmlRegulatedActivityInCurrentFy
          )
    }

    "return a registration with all existing answers when the answer is yes" in forAll { registration: Registration =>
      val updatedRegistration = registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true))

      dataCleaner.cleanup(updatedRegistration) shouldBe updatedRegistration
    }
  }

}
