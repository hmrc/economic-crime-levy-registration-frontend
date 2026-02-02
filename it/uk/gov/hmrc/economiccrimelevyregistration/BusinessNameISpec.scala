package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, NonUKEstablishment, Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class BusinessNameISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.BusinessNameController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onPageLoad(mode))

      "respond with 200 status and the business name HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(businessName = Some(alphaNumericString))

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            optOtherEntityJourneyData = Some(otherEntityJourneyData),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.BusinessNameController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is the name of your business?")
      }
    }

    s"POST ${routes.BusinessNameController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onSubmit(mode))

      def nextPage(entityType: EntityType) = entityType match {
        case Charity                   => routes.CharityRegistrationNumberController.onPageLoad(mode)
        case UnincorporatedAssociation => routes.DoYouHaveCrnController.onPageLoad(mode)
        case Trust                     => routes.CtUtrController.onPageLoad(mode)
        case NonUKEstablishment        => routes.DoYouHaveCrnController.onPageLoad(mode)
        case _                         => routes.NotableErrorController.answersAreInvalid()
      }

      "save the business name then redirect to the correct page" in {
        stubAuthorisedWithNoGroupEnrolment()
        val businessName: String = alphaNumericString

        val registration = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )

        val otherEntityJourneyData = registration.otherEntityJourneyData.copy(businessName = Some(businessName))
        val updatedRegistration    = registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
        stubUpsertSession()

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.BusinessNameController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", businessName))
        )

        status(result) shouldBe SEE_OTHER

        val isNextFieldEmpty = updatedRegistration.entityType match {
          case Some(value) =>
            value match {
              case Charity                   => otherEntityJourneyData.charityRegistrationNumber.isEmpty
              case UnincorporatedAssociation => otherEntityJourneyData.companyRegistrationNumber.isEmpty
              case Trust                     => otherEntityJourneyData.ctUtr.isEmpty
              case NonUKEstablishment        => otherEntityJourneyData.companyRegistrationNumber.isEmpty
              case _                         => false
            }
          case _           => false
        }

        mode match {
          case NormalMode => redirectLocation(result) shouldBe Some(nextPage(updatedRegistration.entityType.get).url)
          case CheckMode  =>
            if (isNextFieldEmpty) {
              redirectLocation(result) shouldBe Some(nextPage(updatedRegistration.entityType.get).url)
            } else {
              redirectLocation(result) shouldBe Some(
                routes.CheckYourAnswersController.onPageLoad().url
              )
            }
        }
      }
    }
  }
}
