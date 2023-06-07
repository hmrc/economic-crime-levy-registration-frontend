package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CharityRegistrationNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CharityRegistrationNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CharityRegistrationNumberController.onPageLoad(NormalMode))

    "respond with 200 status and the select charity registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.CharityRegistrationNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your charity registration number (CHRN)?")
    }
  }

  s"POST ${routes.CharityRegistrationNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CharityRegistrationNumberController.onSubmit(NormalMode))

    "save the charity number then redirect to the business sector page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(charityRegistrationNumber = Some("01234567"))
      val updatedRegistration = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CharityRegistrationNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "01234567"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(mode = NormalMode).url)
    }
  }

}
