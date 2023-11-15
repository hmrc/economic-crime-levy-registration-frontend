package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.UtrLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class SaUtrISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.SaUtrController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.SaUtrController.onPageLoad(NormalMode))

    "respond with 200 status and the company registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.SaUtrController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your self assessment unique taxpayer reference?")
    }
  }

  s"POST ${routes.SaUtrController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.SaUtrController.onSubmit(NormalMode))

    "save the SA UTR then redirect to the overseas tax identifier page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      val saUtr = numStringsWithConcreteLength(UtrLength).sample.get

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          saUtr = Some(saUtr),
          ctUtr = None
        )
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.SaUtrController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", saUtr))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
    }
  }

}
