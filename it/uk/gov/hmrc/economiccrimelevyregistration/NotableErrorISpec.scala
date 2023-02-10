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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

class NotableErrorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.NotableErrorController.answersAreInvalid().url}"             should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.NotableErrorController.answersAreInvalid())

    "respond with 200 status and the answers are invalid HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.NotableErrorController.answersAreInvalid()))

      status(result) shouldBe OK
      html(result)     should include("The answers you provided are not valid")
    }
  }

  s"GET ${routes.NotableErrorController.userAlreadyEnrolled().url}"           should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.NotableErrorController.userAlreadyEnrolled())

    "respond with 200 status and the user already enrolled HTML view" in {
      stubAuthorisedWithEclEnrolment()

      val result = callRoute(FakeRequest(routes.NotableErrorController.userAlreadyEnrolled()))

      status(result) shouldBe OK
      html(result)     should include("You have already registered for the Economic Crime Levy")
    }
  }

  s"GET ${routes.NotableErrorController.groupAlreadyEnrolled().url}"          should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(routes.NotableErrorController.groupAlreadyEnrolled())

    "respond with 200 status and the org already registered HTML view" in {
      stubAuthorised()
      stubWithGroupEclEnrolment()

      val result = callRoute(FakeRequest(routes.NotableErrorController.groupAlreadyEnrolled()))

      status(result) shouldBe OK
      html(result)     should include("Your organisation is already registered for the Economic Crime Levy")
    }
  }

  s"GET ${routes.NotableErrorController.agentCannotRegister().url}"           should {
    behave like authorisedActionAgentsAllowedRoute(routes.NotableErrorController.agentCannotRegister())

    "respond with 200 status and the agent cannot register HTML view" in {
      stubAuthorisedWithAgentAffinityGroup()

      val result = callRoute(FakeRequest(routes.NotableErrorController.agentCannotRegister()))

      status(result) shouldBe OK
      html(result)     should include("You cannot use this service to register for the Economic Crime Levy")
    }
  }

  s"GET ${routes.NotableErrorController.assistantCannotRegister().url}"       should {
    behave like authorisedActionAssistantsAllowedRoute(routes.NotableErrorController.assistantCannotRegister())

    "respond with 200 status and the assistant cannot register HTML view" in {
      stubAuthorisedWithAssistantCredentialRole()

      val result = callRoute(FakeRequest(routes.NotableErrorController.assistantCannotRegister()))

      status(result) shouldBe OK
      html(result)     should include("You must be an administrator to register for this service")
    }
  }

  s"GET ${routes.NotableErrorController.organisationAlreadyRegistered().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      routes.NotableErrorController.organisationAlreadyRegistered()
    )

    "respond with 200 status and the organisation already registered HTML view" in {
      stubAuthorisedWithEclEnrolment()

      val result = callRoute(FakeRequest(routes.NotableErrorController.organisationAlreadyRegistered()))

      status(result) shouldBe OK
      html(result)     should include("Your organisation is already registered for the Economic Crime Levy")
    }
  }

}
