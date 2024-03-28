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

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.UtrTypeController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrTypeController.onPageLoad(mode))

      "respond with 200 status and the company registration number HTML view" in {
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

        val result = callRoute(FakeRequest(routes.UtrTypeController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is your UK UTR?")
      }
    }

    s"POST ${routes.UtrTypeController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.UtrTypeController.onSubmit(mode))

      "save the selected UTR type then redirect to correct page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        val utrType = random[UtrType]

        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val otherEntityJourneyData = registration.otherEntityJourneyData.copy(utrType = Some(utrType))
        val updatedRegistration    = registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        val call = utrType match {
          case SaUtr => routes.SaUtrController.onPageLoad(mode).url
          case CtUtr => routes.CtUtrController.onPageLoad(mode).url
        }

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.UtrTypeController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", utrType.toString))
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode => redirectLocation(result) shouldBe Some(call)
          case CheckMode  =>
            utrType match {
              case CtUtr if otherEntityJourneyData.ctUtr.isEmpty => redirectLocation(result) shouldBe Some(call)
              case SaUtr if otherEntityJourneyData.saUtr.isEmpty => redirectLocation(result) shouldBe Some(call)
              case _                                             => redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
            }

        }
      }

    }
  }

}
