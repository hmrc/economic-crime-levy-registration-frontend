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

class RelevantAp12MonthsDataCleanupSpec extends SpecBase {

  val dataCleanup = new RelevantAp12MonthsDataCleanup

  "cleanup" should {
    "return a registration with the relevant AP length and revenue meets threshold flag set to none when relevant AP 12 months is true" in forAll {
      (registration: Registration) =>
        val updatedRegistration = registration.copy(relevantAp12Months = Some(true))

        dataCleanup.cleanup(updatedRegistration) shouldBe updatedRegistration.copy(
          relevantApLength = None,
          revenueMeetsThreshold = None
        )
    }

    "return a registration with the revenue meets threshold flag set to none when relevant AP 12 months is false" in forAll {
      (registration: Registration) =>
        val updatedRegistration = registration.copy(relevantAp12Months = Some(false))

        dataCleanup.cleanup(updatedRegistration) shouldBe updatedRegistration.copy(
          revenueMeetsThreshold = None
        )
    }
  }

}
