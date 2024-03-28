package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial

class AmlRegulatedActivityISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AmlRegulatedActivityController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AmlRegulatedActivityController.onPageLoad(NormalMode))

    "respond with 200 status and the Aml regulated HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val expectedTaxYearStart = EclTaxYear.currentFyStartYear.toString
      val expectedTaxYearEnd   = EclTaxYear.currentFyFinishYear.toString
      val registration         = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo       = RegistrationAdditionalInfo(registration.internalId)

      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include(
        s"Did you carry out AML-regulated activity between 1 April $expectedTaxYearStart and 31 March $expectedTaxYearEnd?"
      )
    }
  }

  s"GET ${routes.AmlRegulatedActivityController.onPageLoad(CheckMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AmlRegulatedActivityController.onPageLoad(CheckMode))

    "respond with 200 status and the Aml regulated HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val expectedTaxYearStart = EclTaxYear.currentFyStartYear.toString
      val expectedTaxYearEnd   = EclTaxYear.currentFyFinishYear.toString
      val additionalInfo       = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )

      stubGetRegistrationWithEmptyAdditionalInfo(registration)
      stubSessionForStoreUrl()

      val result = callRoute(FakeRequest(routes.AmlRegulatedActivityController.onPageLoad(CheckMode)))

      status(result) shouldBe OK

      html(result) should include(
        s"Did you carry out AML-regulated activity between 1 April $expectedTaxYearStart and 31 March $expectedTaxYearEnd?"
      )
    }
  }

  s"POST ${routes.AmlRegulatedActivityController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AmlRegulatedActivityController.onSubmit(NormalMode))

    "save the selected AML regulated activity option then redirect to the AML supervisor page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue())
        )
      stubGetRegistrationWithEmptyAdditionalInfo(registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = None))

      val updatedRegistration = registration.copy(
        carriedOutAmlRegulatedActivityInCurrentFy = Some(true),
        amlSupervisor = None
      )

      stubUpsertRegistration(updatedRegistration)
      val additionalInfo = RegistrationAdditionalInfo(testInternalId)

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubUpsertRegistrationAdditionalInfo(additionalInfo.copy(liableForPreviousYears = None))

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlSupervisorController.onPageLoad(NormalMode, Initial).url)
    }

    "save the selected AML regulated activity option then redirect to the liable for previous year page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          internalId = testInternalId,
          relevantApRevenue = Some(randomApRevenue())
        )

      stubGetRegistrationWithEmptyAdditionalInfo(registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = None))
      val additionalInfo = RegistrationAdditionalInfo(testInternalId)

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val updatedRegistration = registration.copy(
        carriedOutAmlRegulatedActivityInCurrentFy = Some(false),
        amlSupervisor = None
      )

      stubUpsertRegistration(updatedRegistration)
      stubUpsertRegistrationAdditionalInfo(additionalInfo.copy(liableForPreviousYears = None))

      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url
      )
    }
  }

  s"POST ${routes.AmlRegulatedActivityController.onSubmit(CheckMode).url}"   should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.AmlRegulatedActivityController.onSubmit(CheckMode))

    "save the selected AML regulated activity option then redirect to the AML supervisor page in Normal Mode when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue())
        )
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration = registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(true))

      stubUpsertRegistration(updatedRegistration)
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.AmlSupervisorController.onPageLoad(NormalMode, Initial).url)
    }

    "save the selected AML regulated activity option then redirect to the LiabilityBeforeCurrentYear page in Normal Mode when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue())
        )
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration = registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(false))

      stubUpsertRegistration(updatedRegistration)
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      val result = callRoute(
        FakeRequest(routes.AmlRegulatedActivityController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url)
    }

  }
}
