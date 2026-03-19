package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.charityRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class CharityRegistrationNumberISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CharityRegistrationNumberController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CharityRegistrationNumberController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the charity registration number HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.CharityRegistrationNumberController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your charity registration number (CHRN)?")
    }
  }

  s"POST ${routes.CharityRegistrationNumberController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CharityRegistrationNumberController.onSubmit(NormalMode))

    "save the charity registration number then redirect to the Do you have a Utr page" in {
      stubAuthorisedWithNoGroupEnrolment()

      val charityNumber = stringsWithMaxLength(charityRegistrationNumberMaxLength).sample.get

      val registration   = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(charityRegistrationNumber = Some(charityNumber))
      val updatedRegistration    = registration.copy(
        optOtherEntityJourneyData = Some(otherEntityJourneyData)
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CharityRegistrationNumberController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", charityNumber))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.DoYouHaveUtrController.onPageLoad(mode = NormalMode).url
      )
    }
  }

  s"POST ${routes.CharityRegistrationNumberController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.CharityRegistrationNumberController.onSubmit(CheckMode))

    "save the charity registration number then redirect to the Do you have a Utr page if isCtUtrPresent value is empty" in {
      stubAuthorisedWithNoGroupEnrolment()

      val charityNumber = stringsWithMaxLength(charityRegistrationNumberMaxLength).sample.get
      val data          = OtherEntityJourneyData.empty().copy(isCtUtrPresent = None)

      val registration = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue()),
          optOtherEntityJourneyData = Some(data)
        )

      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(charityRegistrationNumber = Some(charityNumber))
      val updatedRegistration    = registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CharityRegistrationNumberController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", charityNumber))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.DoYouHaveUtrController.onPageLoad(mode = CheckMode).url
      )
    }

    "save the charity registration number then redirect to the Check Your Answers page if isCtUtrPresent value is present" in {
      stubAuthorisedWithNoGroupEnrolment()

      val charityNumber  = stringsWithMaxLength(charityRegistrationNumberMaxLength).sample.get
      val isCtUtrPresent = arbitrary[Boolean].sample.get
      val data           = OtherEntityJourneyData.empty().copy(isCtUtrPresent = Some(isCtUtrPresent))

      val registration = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          relevantApRevenue = Some(randomApRevenue()),
          optOtherEntityJourneyData = Some(data)
        )

      val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(charityRegistrationNumber = Some(charityNumber))
      val updatedRegistration    = registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.CharityRegistrationNumberController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", charityNumber))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad().url
      )
    }

  }

}
