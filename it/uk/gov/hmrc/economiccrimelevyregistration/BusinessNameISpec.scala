package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.Charity
import uk.gov.hmrc.economiccrimelevyregistration.models._

class BusinessNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.BusinessNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onPageLoad(NormalMode))

    "respond with 200 status and the business name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.BusinessNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is the name of your business?")
    }
  }

  s"POST ${routes.BusinessNameController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onSubmit(NormalMode))

    "save the business name then redirect to the charity registration number page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration: Registration = random[Registration].copy(
        entityType = Some(Charity)
      )
      val additionalInfo             = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val otherEntityJourneyData = registration.otherEntityJourneyData.copy(businessName = Some("Test"))
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.BusinessNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "Test"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CharityRegistrationNumberController.onPageLoad(mode = NormalMode).url
      )
    }
  }

}
