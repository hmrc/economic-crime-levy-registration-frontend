package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models._

class OtherEntityTypeISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.OtherEntityTypeController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.OtherEntityTypeController.onPageLoad(NormalMode))

    "respond with 200 status and the select entity type HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.OtherEntityTypeController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Tell us your entity type")
    }
  }

  s"POST ${routes.OtherEntityTypeController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.OtherEntityTypeController.onSubmit(NormalMode))

    "save the selected entity type then redirect to the dummy page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(entityType = Some(Charity))
      val updatedRegistration = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.OtherEntityTypeController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "Charity"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessNameController.onPageLoad(mode = NormalMode).url)
    }
  }

}
