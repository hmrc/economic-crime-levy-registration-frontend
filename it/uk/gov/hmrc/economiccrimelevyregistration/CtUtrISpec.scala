package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.UtrLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.UnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CtUtrISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CtUtrController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CtUtrController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the CtUtr HTML view" in {
      stubAuthorised()

      val registration: Registration = random[Registration].copy(entityType = Some(UnincorporatedAssociation))

      val additionalInfo: RegistrationAdditionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.CtUtrController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your corporation tax unique taxpayer reference?")
    }
  }

  s"POST ${routes.CtUtrController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CtUtrController.onSubmit(NormalMode))

    "save the postcode then redirect to the CtUtrPostcode controller page" in {
      stubAuthorised()

      val registration   = random[Registration]
        .copy(
          entityType = Some(UnincorporatedAssociation),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      val ctUtr = numStringsWithConcreteLength(UtrLength).sample.get

      val otherEntityJourneyData =
        OtherEntityJourneyData.empty().copy(isCtUtrPresent = Some(true), ctUtr = Some(ctUtr))
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )
      println(updatedRegistration.entityType)

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CtUtrController.onSubmit(NormalMode))
          .withFormUrlEncodedBody("value" -> ctUtr)
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.CtUtrPostcodeController.onPageLoad(NormalMode).url)
    }
  }

}
