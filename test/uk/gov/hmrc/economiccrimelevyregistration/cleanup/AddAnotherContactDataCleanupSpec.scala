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
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, Registration}

class AddAnotherContactDataCleanupSpec extends SpecBase {

  val dataCleanup = new AddAnotherContactDataCleanup

  "cleanup" should {
    "return a registration with second contact details set to none when add another contact is false" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          contacts = registration.contacts.copy(secondContact = Some(false))
        )

        dataCleanup.cleanup(updatedRegistration) shouldBe updatedRegistration.copy(contacts =
          updatedRegistration.contacts
            .copy(secondContactDetails = ContactDetails.empty)
        )
    }

    "return a registration with second contact details preserved when add another contact is true" in forAll {
      registration: Registration =>
        val updatedRegistration = registration.copy(
          contacts = registration.contacts.copy(secondContact = Some(true))
        )

        dataCleanup.cleanup(updatedRegistration) shouldBe updatedRegistration
    }
  }

}
