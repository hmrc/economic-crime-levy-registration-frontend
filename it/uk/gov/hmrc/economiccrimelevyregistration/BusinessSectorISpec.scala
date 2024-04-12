package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class BusinessSectorISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.BusinessSectorController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessSectorController.onPageLoad(mode))

      "respond with 200 status and the business sector HTML view" in {
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

        val result = callRoute(FakeRequest(routes.BusinessSectorController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is your business sector?")
      }
    }

    s"POST ${routes.BusinessSectorController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessSectorController.onSubmit(mode))

      s"($mode) save the selected business sector option then redirect to the correct page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val businessSector = random[BusinessSector]
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val updatedRegistration = registration.copy(businessSector = Some(businessSector))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.BusinessSectorController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", businessSector.toString))
        )

        status(result) shouldBe SEE_OTHER

        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              contacts.routes.FirstContactNameController.onPageLoad(NormalMode).url
            )
          case CheckMode  =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad().url
            )
        }
      }
    }
  }
}
