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

class ConfirmContactAddressDataCleanupSpec extends SpecBase {

  val dataCleanup = new ConfirmContactAddressDataCleanup

  "cleanup" should {
    "return a registration with the address set to none when use registered office address is false" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = Some(false)
        )

        dataCleanup.cleanup(updatedRegistration, false) shouldBe updatedRegistration.copy(contactAddress = None)
    }

    "return a registration with address preserved when use registered office address is true" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = Some(true)
        )

        dataCleanup.cleanup(updatedRegistration, false) shouldBe updatedRegistration
    }
  }

}
