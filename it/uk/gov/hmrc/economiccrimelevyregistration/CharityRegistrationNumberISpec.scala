package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.CharityRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CharityRegistrationNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CharityRegistrationNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CharityRegistrationNumberController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the charity registration number HTML view" in {
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

    "save the charity registration number then redirect to the business sector page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val charityNumber = stringsWithMaxLength(CharityRegistrationNumberMaxLength).sample.get

      val registration = random[Registration]

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(charityRegistrationNumber = Some(charityNumber))
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CharityRegistrationNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", charityNumber))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CompanyRegistrationNumberController.onPageLoad(mode = NormalMode).url
      )
    }
  }

}
