package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import java.time.LocalDate

class LiabilityBeforeCurrentYearISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.LiabilityBeforeCurrentYearController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.LiabilityBeforeCurrentYearController.onPageLoad(mode)
      )

      "respond with 200 status and the liability before current year HTML view" in {
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

        val result = callRoute(FakeRequest(routes.LiabilityBeforeCurrentYearController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Are you liable for any previous financial years?")
      }
    }
  }

  s"POST ${routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode)
    )

    "save the selected option then redirect to the liability date page when the answer is yes" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = true
      val registration            = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue()),
          businessSector = None
        )
      val additionalInfo          = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val info = RegistrationAdditionalInfo(
        registration.internalId,
        None,
        None,
        None,
        None,
        None
      )

      stubUpsertRegistrationAdditionalInfo(info)
      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.LiabilityDateController.onPageLoad(NormalMode).url)
    }

    "save the answer then redirect to the Entity Type page when the answer is no and the revenue meets the threshold" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = false
      val registration            = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue()),
          businessSector = None,
          revenueMeetsThreshold = Some(true)
        )
      val additionalInfo          = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val info = RegistrationAdditionalInfo(
        registration.internalId,
        None,
        None,
        None,
        None,
        None
      )

      stubUpsertRegistrationAdditionalInfo(info)
      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.EntityTypeController.onPageLoad(NormalMode).url)
    }

    Seq(None, Some(false)).foreach { revenueMeetsThreshold =>
      s"save the answer then redirect to the Not liable page when the answer is no and revenueMeetsThreshold is $revenueMeetsThreshold" in {
        stubAuthorisedWithNoGroupEnrolment()

        val liableBeforeCurrentYear = false
        val registration            = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            registrationType = Some(Initial),
            relevantApRevenue = Some(randomApRevenue()),
            businessSector = None,
            revenueMeetsThreshold = revenueMeetsThreshold
          )
        val additionalInfo          = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val info = RegistrationAdditionalInfo(
          registration.internalId,
          None,
          None,
          None,
          None,
          None
        )

        stubUpsertRegistrationAdditionalInfo(info)
        stubUpsertRegistration(registration)

        val result = callRoute(
          FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode))
            .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.NotLiableController.youDoNotNeedToRegister().url)
      }
    }
  }

  s"POST ${routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode)
    )

    "redirect to check your answers page if nothing changed" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = true
      val registration            = arbitrary[Registration].sample.get
        .copy(
          internalId = testInternalId,
          entityType = Some(arbitrary[EntityType].sample.get),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue()),
          businessSector = None,
          revenueMeetsThreshold = Some(true),
          carriedOutAmlRegulatedActivityInCurrentFy = Some(true)
        )
      val additionalInfo          =
        arbitrary[RegistrationAdditionalInfo].sample.get.copy(
          internalId = registration.internalId,
          liableForPreviousYears = Some(liableBeforeCurrentYear),
          liabilityStartDate = Some(arbitrary[LocalDate].sample.get),
          liabilityYear = Some(LiabilityYear(testEclTaxYear.previous.startYear)),
          eclReference = None
        )

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      stubUpsertRegistrationAdditionalInfo(additionalInfo)
      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad().url
      )
    }

    "redirect to liability date page if change to yes" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = true
      val registration            = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          registrationType = Some(Initial),
          relevantApRevenue = Some(randomApRevenue()),
          businessSector = None
        )
      val additionalInfo          =
        arbitrary[RegistrationAdditionalInfo].sample.get.copy(
          liableForPreviousYears = Some(!liableBeforeCurrentYear),
          liabilityStartDate = None
        )

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val info = RegistrationAdditionalInfo(
        registration.internalId,
        None,
        None,
        None,
        None,
        None
      )

      stubUpsertRegistrationAdditionalInfo(info)
      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(CheckMode))
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.LiabilityDateController.onPageLoad(CheckMode).url)
    }
  }
}
