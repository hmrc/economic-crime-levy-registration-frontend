package uk.gov.hmrc.economiccrimelevyregistration.behaviours

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Registration, RegistrationType}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial

import scala.concurrent.Future

trait AuthorisedBehaviour {
  self: ISpecBase =>

  def authorisedActionWithEnrolmentCheckRoute(
    call: Call,
    registrationType: RegistrationType = Initial
  ): Unit =
    "authorisedActionWithEnrolmentCheckRoute" should {
      "redirect to sign in when there is no auth session" in {
        resetWireMock()
        stubUnauthorised()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should startWith(appConfig.signInUrl)
      }

      "go to already registered page if the user has the ECL enrolment" in {
        resetWireMock()
        stubAuthorisedWithEclEnrolment()

        val registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )

        stubGetRegistrationWithEmptyAdditionalInfo(registration.copy(registrationType = Some(registrationType)))

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.userAlreadyEnrolled().url
      }

      "go to the group already registered if the user does not have the ECL enrolment but the group does" in {
        resetWireMock()
        stubAuthorised()
        stubWithGroupEclEnrolment()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.groupAlreadyEnrolled().url
      }

      "go to the agent not supported page if the user has an agent affinity group" in {
        resetWireMock()
        stubAuthorisedWithAgentAffinityGroup()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.agentCannotRegister().url
      }

      "go to the assistant not supported page if the user has an assistant credential role" in {
        resetWireMock()
        stubAuthorisedWithAssistantCredentialRole()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.assistantCannotRegister().url
      }
    }

  def authorisedActionWithoutEnrolmentCheckRoute(call: Call): Unit =
    "authorisedActionWithoutEnrolmentCheckRoute" should {
      "redirect to sign in when there is no auth session" in {
        resetWireMock()
        stubUnauthorised()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should startWith(appConfig.signInUrl)
      }

      "go to the agent not supported page if the user has an agent affinity group" in {
        resetWireMock()
        stubAuthorisedWithAgentAffinityGroup()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.agentCannotRegister().url
      }

      "go to the assistant not supported page if the user has an assistant credential role" in {
        resetWireMock()
        stubAuthorisedWithAssistantCredentialRole()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.assistantCannotRegister().url
      }
    }

  def authorisedActionAgentsAllowedRoute(call: Call): Unit =
    "authorisedActionAgentsAllowedRoute" should {
      "redirect to sign in when there is no auth session" in {
        resetWireMock()
        stubUnauthorised()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should startWith(appConfig.signInUrl)
      }

      "go to the assistant not supported page if the user has an assistant credential role" in {
        resetWireMock()
        stubAuthorisedWithAssistantCredentialRole()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.assistantCannotRegister().url
      }
    }

  def authorisedActionAssistantsAllowedRoute(call: Call): Unit =
    "authorisedActionAssistantsAllowedRoute" should {
      "redirect to sign in when there is no auth session" in {
        resetWireMock()
        stubUnauthorised()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)               shouldBe SEE_OTHER
        redirectLocation(result).value should startWith(appConfig.signInUrl)
      }

      "go to the agent not supported page if the user has an agent affinity group" in {
        resetWireMock()
        stubAuthorisedWithAgentAffinityGroup()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.agentCannotRegister().url
      }
    }

}
