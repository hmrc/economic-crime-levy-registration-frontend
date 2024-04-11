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
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclAddress, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

class AmendmentRequestedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmendmentRequestedController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.AmendmentRequestedController.onPageLoad())

    "respond with 200 status and the registration submitted HTML view" in {
      stubAuthorisedWithEclEnrolment()

      stubDeleteRegistration()
      stubDeleteRegistrationAdditionalInfo()

      val email      = random[String]
      val eclAddress = random[EclAddress]

      val result = callRoute(
        FakeRequest(routes.AmendmentRequestedController.onPageLoad()).withSession(
          (SessionKeys.firstContactEmail -> email),
          (SessionKeys.contactAddress    -> Json.stringify(Json.toJson(eclAddress)))
        )
      )

      status(result) shouldBe OK
      html(result)     should include("Economic Crime Levy registration amendment requested")
    }

    "respond with 500 status if the contact address is not present in the request" in {
      stubAuthorisedWithEclEnrolment()

      stubDeleteRegistration()
      stubDeleteRegistrationAdditionalInfo()

      val email = random[String]

      val result = callRoute(
        FakeRequest(routes.AmendmentRequestedController.onPageLoad()).withSession(
          (SessionKeys.firstContactEmail -> email)
        )
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "respond with 500 status if the email address is not present in the request" in {
      stubAuthorisedWithEclEnrolment()

      stubDeleteRegistration()
      stubDeleteRegistrationAdditionalInfo()

      val eclAddress = random[EclAddress]

      val result = callRoute(
        FakeRequest(routes.AmendmentRequestedController.onPageLoad()).withSession(
          (SessionKeys.contactAddress -> Json.stringify(Json.toJson(eclAddress)))
        )
      )

      status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
