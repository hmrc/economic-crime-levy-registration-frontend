package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.UtrLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{NonUKEstablishment, Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CtUtrISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.CtUtrController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.CtUtrController.onPageLoad(mode)
      )

      "respond with 200 status and the CtUtr HTML view" in {
        stubAuthorised()

        val registration: Registration = random[Registration].copy(entityType = Some(UnincorporatedAssociation))

        val additionalInfo: RegistrationAdditionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.CtUtrController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is your corporation tax unique taxpayer reference?")
      }
    }

    s"POST ${routes.CtUtrController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.CtUtrController.onSubmit(mode))

      "save the postcode then redirect to the CtUtrPostcode controller page" in {
        stubAuthorised()
        val entityType = generateOtherEntityType.sample.get

        val registration = random[Registration]
          .copy(entityType = Some(entityType), relevantApRevenue = Some(randomApRevenue()))

        val additionalInfo = random[RegistrationAdditionalInfo]

        val ctUtr = numStringsWithConcreteLength(UtrLength).sample.get

        val otherEntityJourneyData =
          registration.otherEntityJourneyData.copy(isCtUtrPresent = Some(true), ctUtr = Some(ctUtr), saUtr = None)

        val updatedRegistration = registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.CtUtrController.onSubmit(mode))
            .withFormUrlEncodedBody("value" -> ctUtr)
        )

        status(result) shouldBe SEE_OTHER

        mode match {
          case NormalMode =>
            updatedRegistration.entityType match {
              case None                     => redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
              case Some(Trust)              =>
                redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
              case Some(NonUKEstablishment) =>
                redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
              case Some(_)                  =>
                redirectLocation(result) shouldBe Some(routes.CtUtrPostcodeController.onPageLoad(NormalMode).url)
            }

          case CheckMode =>
            updatedRegistration.entityType match {
              case None                     => redirectLocation(result) shouldBe Some(routes.NotableErrorController.answersAreInvalid().url)
              case Some(Trust)              =>
                redirectLocation(result) shouldBe Some(
                  routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
                )
              case Some(NonUKEstablishment) =>
                redirectLocation(result) shouldBe Some(
                  routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
                )
              case Some(_)                  =>
                updatedRegistration.otherEntityJourneyData.postcode match {
                  case None    =>
                    redirectLocation(result) shouldBe Some(routes.CtUtrPostcodeController.onPageLoad(CheckMode).url)
                  case Some(_) =>
                    redirectLocation(result) shouldBe Some(
                      routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
                    )
                }
            }
        }
      }
    }
  }
}
