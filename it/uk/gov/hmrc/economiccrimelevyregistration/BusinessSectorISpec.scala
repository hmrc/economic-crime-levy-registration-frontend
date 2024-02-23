package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class BusinessSectorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.BusinessSectorController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessSectorController.onPageLoad(NormalMode))

    "respond with 200 status and the business sector HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubSessionForStoreUrl(registration.internalId, routes.BusinessSectorController.onPageLoad(NormalMode))

      val result = callRoute(FakeRequest(routes.BusinessSectorController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your business sector?")
    }
  }

  s"POST ${routes.BusinessSectorController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessSectorController.onSubmit(NormalMode))

    "save the selected business sector option then redirect to the contact name page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val businessSector = random[BusinessSector]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(businessSector = Some(businessSector))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.BusinessSectorController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", businessSector.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(contacts.routes.FirstContactNameController.onPageLoad(NormalMode).url)
    }
  }
}
