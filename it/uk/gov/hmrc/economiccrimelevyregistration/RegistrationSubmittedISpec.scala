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
class RegistrationSubmittedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RegistrationSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.RegistrationSubmittedController.onPageLoad())

    "respond with 200 status and the registration submitted HTML view" in {
      stubAuthorisedWithEclEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistration(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubSessionForStoreUrl(routes.RegistrationSubmittedController.onPageLoad())

      stubDeleteRegistrationAdditionalInfo()
      stubDeleteRegistration()

      val result = callRoute(
        FakeRequest(routes.RegistrationSubmittedController.onPageLoad())
      )

      status(result) shouldBe OK
      html(result)     should include("Registration submitted")
    }
  }

}
