package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CompanyRegistrationNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CompanyRegistrationNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CompanyRegistrationNumberController.onPageLoad(NormalMode))

    "respond with 200 status and the select charity registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.CompanyRegistrationNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your company registration number?")
    }
  }

  s"POST ${routes.CompanyRegistrationNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CompanyRegistrationNumberController.onSubmit(NormalMode))

    "save the charity number then redirect to the business sector page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(companyRegistrationNumber = Some("01234567"))
      val updatedRegistration = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CompanyRegistrationNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "01234567"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.OtherEntityCheckYourAnswersController.onPageLoad().url)
    }
  }

}
