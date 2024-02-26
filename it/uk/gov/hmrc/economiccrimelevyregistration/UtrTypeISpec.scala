package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.UtrType.{CtUtr, SaUtr}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class UtrTypeISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.UtrTypeController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrTypeController.onPageLoad(NormalMode))

    "respond with 200 status and the company registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubGetRegistration(registration)
      stubSessionForStoreUrl(routes.UtrTypeController.onPageLoad(NormalMode), registration.internalId)

      val result = callRoute(FakeRequest(routes.UtrTypeController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your UK UTR?")
    }
  }

  s"POST ${routes.UtrTypeController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrTypeController.onSubmit(NormalMode))

    "save the selected UTR type then redirect to correct page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val utrType = random[UtrType]

      stubGetRegistration(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(utrType = Some(utrType))
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      val call = utrType match {
        case SaUtr => routes.SaUtrController.onPageLoad(NormalMode).url
        case CtUtr => routes.CtUtrController.onPageLoad(NormalMode).url
      }

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.UtrTypeController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", utrType.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(call)
    }
  }

}
