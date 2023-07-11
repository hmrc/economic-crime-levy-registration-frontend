package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class OverseasTaxIdentifierISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.OverseasTaxIdentifierController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.OverseasTaxIdentifierController.onPageLoad(NormalMode))

    "respond with 200 status and the company registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.OverseasTaxIdentifierController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your overseas tax identifier?")
    }
  }

  s"POST ${routes.OverseasTaxIdentifierController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.OverseasTaxIdentifierController.onSubmit(NormalMode))

    "save the overseas tax identifier then redirect to the check your answers page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      val taxIdentifier = stringsLongerThan(1).sample.get

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(overseasTaxIdentifier = Some(taxIdentifier))
      val updatedRegistration = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.OverseasTaxIdentifierController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", taxIdentifier))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.OtherEntityCheckYourAnswersController.onPageLoad().url)
    }
  }

}
