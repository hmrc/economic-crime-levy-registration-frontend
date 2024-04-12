package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.companyRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{NonUKEstablishment, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class NonUkCrnISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.NonUkCrnController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.NonUkCrnController.onPageLoad(mode))

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

        val result = callRoute(FakeRequest(routes.NonUkCrnController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is your company registration number?")
      }
    }

    s"POST ${routes.NonUkCrnController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.NonUkCrnController.onSubmit(mode))

      "save the company registration number then redirect to the correct page" in {
        stubAuthorisedWithNoGroupEnrolment()
        val entityType = Gen.oneOf(UnincorporatedAssociation, NonUKEstablishment).sample.get

        val registration   = random[Registration]
          .copy(
            entityType = Some(entityType),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)

        val companyNumber = stringsWithMaxLength(companyRegistrationNumberMaxLength).sample.get

        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val otherEntityJourneyData =
          registration.otherEntityJourneyData.copy(companyRegistrationNumber = Some(companyNumber))
        val updatedRegistration    = registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.NonUkCrnController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", companyNumber))
        )

        val navigateToNextPage = if (registration.entityType.contains(UnincorporatedAssociation)) {
          Some(routes.DoYouHaveUtrController.onPageLoad(mode).url)
        } else {
          Some(routes.UtrTypeController.onPageLoad(mode).url)
        }

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode => redirectLocation(result) shouldBe navigateToNextPage
          case CheckMode  =>
            if (
              (registration.isUnincorporatedAssociation && registration.otherEntityJourneyData.isCtUtrPresent.isEmpty)
              || (!registration.isUnincorporatedAssociation && registration.otherEntityJourneyData.utrType.isEmpty)
            ) { redirectLocation(result) shouldBe navigateToNextPage }
            else {
              redirectLocation(result) shouldBe Some(
                routes.CheckYourAnswersController.onPageLoad().url
              )
            }
        }
      }
    }
  }
}
