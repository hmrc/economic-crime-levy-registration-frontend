package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.OrganisationNameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class PartnershipNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.PartnershipNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.PartnershipNameController.onPageLoad(NormalMode))

    "respond with 200 status and the partnership name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubSessionForStoreUrl(routes.PartnershipNameController.onPageLoad(NormalMode))

      val result = callRoute(FakeRequest(routes.PartnershipNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is the partnership name?")
    }
  }

  s"POST ${routes.PartnershipNameController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.PartnershipNameController.onSubmit(NormalMode))

    "save the provided partnership name then redirect to the business sector page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration    = random[Registration]
      val partnershipName = stringsWithMaxLength(OrganisationNameMaxLength).sample.get
      val additionalInfo  = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(partnershipName = Some(partnershipName))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.PartnershipNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", partnershipName))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
    }
  }
}
