package uk.gov.hmrc.economiccrimelevyregistration.behaviours

import play.api.mvc.{Call, Result}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase

import scala.concurrent.Future

trait AuthorisedBehaviour {
  self: ISpecBase =>

  def authorisedActionRoute(call: Call): Unit =
    "authorisedAction" should {
      "go to already registered page if the user has the ECL enrolment" in {
        stubAuthorisedWithEclEnrolment()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Already registered - user already has enrolment"
      }

      "go to the group already registered if the user does not have the ECL enrolment but the group does" in {
        stubAuthorised()
        stubWithGroupEclEnrolment()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Group already has the enrolment - assign the enrolment to the user"
      }

      "go to the agent not supported page if the user has an agent affinity group" in {
        stubAuthorisedWithAgentAffinityGroup()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Agent account not supported - must be an organisation or individual"
      }

      "go to the assistant not supported page if the user has an assistant credential role" in {
        stubAuthorisedWithAssistantCredentialRole()

        val result: Future[Result] = callRoute(FakeRequest(call))

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "User is not an Admin - request an admin to perform registration"
      }
    }

}
