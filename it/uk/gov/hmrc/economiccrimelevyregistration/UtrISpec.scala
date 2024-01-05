package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.UtrLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class UtrISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.UtrController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrController.onPageLoad(NormalMode))

    "respond with 200 status and the company registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)
      stubGetRegistrationAdditionalInfo(
        RegistrationAdditionalInfo.apply(
          registration.internalId,
          None,
          None
        )
      )

      val result = callRoute(FakeRequest(routes.UtrController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your unique taxpayer reference?")
    }
  }

  s"POST ${routes.UtrController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrController.onSubmit(NormalMode))

    "save the UTR then redirect to the company registration number page page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      val Utr = numStringsWithConcreteLength(UtrLength).sample.get

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          ctUtr = Some(Utr)
        )
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.UtrController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", Utr))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CompanyRegistrationNumberController.onPageLoad(NormalMode).url)
    }
  }

}
