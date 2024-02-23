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

package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

class AmendmentRequestedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmendmentRequestedController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.AmendmentRequestedController.onPageLoad())

    "respond with 200 status and the registration submitted HTML view" in {
      stubAuthorisedWithEclEnrolment()

      val fistContactEmailAddress = random[String]
      val registration            = random[Registration]
      val contacts                = registration.contacts.copy(
        firstContactDetails =
          registration.contacts.firstContactDetails.copy(emailAddress = Some(fistContactEmailAddress))
      )
      val updatedRegistration     = registration.copy(contacts = contacts)

      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistration(updatedRegistration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubDeleteRegistration()
      stubDeleteRegistrationAdditionalInfo()

      val result = callRoute(
        FakeRequest(routes.AmendmentRequestedController.onPageLoad())
      )

      status(result) shouldBe OK
      html(result)     should include("Economic Crime Levy registration amendment requested")
    }
  }

}
