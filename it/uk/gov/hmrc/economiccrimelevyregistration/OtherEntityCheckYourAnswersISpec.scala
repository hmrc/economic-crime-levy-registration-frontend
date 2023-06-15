package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class OtherEntityCheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.OtherEntityCheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.OtherEntityCheckYourAnswersController.onPageLoad())

    "respond with 200 status and the other check your answers HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.OtherEntityCheckYourAnswersController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Other entity details")
    }
  }

  s"POST ${routes.OtherEntityCheckYourAnswersController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.OtherEntityCheckYourAnswersController.onSubmit())

    "redirect to the Business sector page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.OtherEntityCheckYourAnswersController.onSubmit())
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
    }
  }

}
