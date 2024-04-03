package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.OrganisationNameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class PartnershipNameISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.PartnershipNameController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.PartnershipNameController.onPageLoad(mode))

      "respond with 200 status and the partnership name HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.PartnershipNameController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is the partnership name?")
      }
    }
  }

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"POST ${routes.PartnershipNameController.onSubmit(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.PartnershipNameController.onSubmit(mode))

      "save the provided partnership name then redirect to the business sector page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration    = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val partnershipName = stringsWithMaxLength(OrganisationNameMaxLength).sample.get
        val additionalInfo  = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val updatedRegistration = registration.copy(partnershipName = Some(partnershipName))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.PartnershipNameController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", partnershipName))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
          case CheckMode  =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
            )
        }

      }
    }
  }
}
