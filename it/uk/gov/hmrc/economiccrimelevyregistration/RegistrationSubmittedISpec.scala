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

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, Registration, RegistrationAdditionalInfo, SessionKeys}
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class RegistrationSubmittedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RegistrationSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.RegistrationSubmittedController.onPageLoad())

    "respond with 200 status and the registration submitted HTML view when all required details are present" in {
      stubAuthorisedWithEclEnrolment()

      val registration   = arbitrary[Registration].sample.get
      val email          = alphaNumericString
      val liabilityYear  = arbitrary[LiabilityYear].sample.get
      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubDeleteRegistrationAdditionalInfo()
      stubDeleteRegistration()

      val result = callRoute(
        FakeRequest(routes.RegistrationSubmittedController.onPageLoad())
          .withSession(
            SessionKeys.firstContactEmail       -> email,
            SessionKeys.liabilityYear           -> liabilityYear.asString,
            SessionKeys.registeringForCurrentFY -> arbitrary[Boolean].sample.get.toString
          )
      )

      status(result) shouldBe OK
      html(result)     should include("Registration submitted")
    }

    "respond with 303 status and answers are invalid view when the first contact email address is missing from the session data" in {
      val registration  = arbitrary[Registration].sample.get
      val liabilityYear = arbitrary[LiabilityYear].sample.get

      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubAuthorisedWithEclEnrolment()
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubDeleteRegistrationAdditionalInfo()
      stubDeleteRegistration()

      val result = callRoute(
        FakeRequest(routes.RegistrationSubmittedController.onPageLoad())
          .withSession(SessionKeys.liabilityYear -> liabilityYear.asString)
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }

    "respond with 303 status and answers are invalid view when the liability year is missing from the session data" in {
      val registration = arbitrary[Registration].sample.get
      val email        = alphaNumericString

      val additionalInfo =
        arbitrary[RegistrationAdditionalInfo].sample.get.copy(eclReference = Some(testEclRegistrationReference))

      stubAuthorisedWithEclEnrolment()
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubDeleteRegistrationAdditionalInfo()
      stubDeleteRegistration()

      val result = callRoute(
        FakeRequest(routes.RegistrationSubmittedController.onPageLoad())
          .withSession(SessionKeys.firstContactEmail -> email)
      )

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
    }
  }

}
