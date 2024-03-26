package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, NonUKEstablishment, Trust, UnincorporatedAssociation, UnlimitedCompany}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class BusinessNameISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.BusinessNameController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onPageLoad(mode))

      "respond with 200 status and the business name HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(businessName = Some(alphaNumericString))

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            optOtherEntityJourneyData = Some(otherEntityJourneyData),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.BusinessNameController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("What is the name of your business?")
      }
    }
  }

  s"POST ${routes.BusinessNameController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onSubmit(NormalMode))

    def nextPage(entityType: EntityType) = entityType match {
      case Charity                   => routes.CharityRegistrationNumberController.onPageLoad(NormalMode)
      case UnincorporatedAssociation => routes.DoYouHaveCrnController.onPageLoad(NormalMode)
      case Trust                     => routes.CtUtrController.onPageLoad(NormalMode)
      case NonUKEstablishment        => routes.DoYouHaveCrnController.onPageLoad(NormalMode)
      case _                         => routes.NotableErrorController.answersAreInvalid()
    }

    "save the business name then redirect to the charity registration number page" in {
      stubAuthorisedWithNoGroupEnrolment()
      val businessName: String = alphaNumericString

      val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(
          businessName = Some(businessName),
          charityRegistrationNumber = Some(alphaNumericString),
          isUkCrnPresent = Some(true),
          ctUtr = Some(alphaNumericString)
        )

      val registration = random[Registration]
        .copy(
          entityType = Some(UnlimitedCompany),
          optOtherEntityJourneyData = Some(otherEntityJourneyData),
          relevantApRevenue = Some(randomApRevenue())
        )

      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubUpsertSession()

      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.BusinessNameController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", businessName))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        nextPage(registration.entityType.get).url
      )
    }
  }

  s"POST ${routes.BusinessNameController.onSubmit(CheckMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onSubmit(CheckMode))

    def nextPage(entityType: EntityType) = entityType match {
      case Charity                   => routes.CharityRegistrationNumberController.onPageLoad(CheckMode)
      case UnincorporatedAssociation => routes.DoYouHaveCrnController.onPageLoad(CheckMode)
      case Trust                     => routes.CtUtrController.onPageLoad(CheckMode)
      case NonUKEstablishment        => routes.DoYouHaveCrnController.onPageLoad(CheckMode)
      case _                         => routes.NotableErrorController.answersAreInvalid()
    }

    "save the business name then redirect to the correct page" in {
      stubAuthorisedWithNoGroupEnrolment()
      val businessName: String = alphaNumericString

//      val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
//        .empty()
//        .copy(
//          businessName = Some(businessName),
//          charityRegistrationNumber = Some(alphaNumericString),
//          isUkCrnPresent = Some(true),
//          ctUtr = Some(alphaNumericString)
//        )

      val registration                                   = random[Registration]
      val otherEntityJourneyData: OtherEntityJourneyData = registration.otherEntityJourneyData
        .copy(businessName = Some(businessName))

      val updatedRegistration = registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

//        .copy(
//          entityType = Some(UnlimitedCompany),
//          optOtherEntityJourneyData =
//          relevantApRevenue = Some(randomApRevenue())
//        )

      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(updatedRegistration)
      stubUpsertSession()

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.BusinessNameController.onSubmit(CheckMode))
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

      if (isNextFieldEmpty) {
        redirectLocation(result) shouldBe Some(nextPage(registration.entityType.get).url)
      } else {
        redirectLocation(result) shouldBe Some(routes.CheckYourAnswersController.onPageLoad().url)
      }
    }
  }

}
