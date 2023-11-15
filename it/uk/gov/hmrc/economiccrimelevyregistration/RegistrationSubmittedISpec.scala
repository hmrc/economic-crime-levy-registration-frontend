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
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.EmailMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.models.{SessionData, SessionKeys}

class RegistrationSubmittedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RegistrationSubmittedController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.RegistrationSubmittedController.onPageLoad())

    "respond with 200 status and the registration submitted HTML view" in forAll { sessionData: SessionData =>
      stubAuthorisedWithEclEnrolment()

      stubGetSession(sessionData)

      val eclReference             = random[String]
      val firstContactEmailAddress = emailAddress(EmailMaxLength).sample.get

      val result = callRoute(
        FakeRequest(routes.RegistrationSubmittedController.onPageLoad())
          .withSession(
            (SessionKeys.EclReference, eclReference),
            (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress)
          )
      )

      status(result) shouldBe OK
      html(result)     should include("Registration submitted")
    }
  }

}
