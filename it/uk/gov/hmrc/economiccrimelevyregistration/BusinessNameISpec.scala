package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.{Charity, NonUKEstablishment, Trust, UnincorporatedAssociation}
import uk.gov.hmrc.economiccrimelevyregistration.models._

class BusinessNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.BusinessNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.BusinessNameController.onPageLoad(NormalMode))

    "respond with 200 status and the business name HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
        .empty()
        .copy(businessName = Some(alphaNumericString))

      val registration   = random[Registration]
        .copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.BusinessNameController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is the name of your business?")
    }
  }

  s"POST ${routes.BusinessNameController.onSubmit(NormalMode).url}"  should {
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
        .copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

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

}
