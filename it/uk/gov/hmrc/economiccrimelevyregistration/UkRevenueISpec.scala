package uk.gov.hmrc.economiccrimelevyregistration

import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

  val minRevenue = 0L
  val maxRevenue = 99999999999L

  val revenueGen: Gen[Long] = Gen.chooseNum[Long](minRevenue, maxRevenue)

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.UkRevenueController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.UkRevenueController.onPageLoad(mode))

      "respond with 200 status and the UK revenue view" in {
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

        val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Total UK revenue")
      }
    }

    s"POST ${routes.UkRevenueController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.UkRevenueController.onSubmit(mode))

      s"save the UK revenue then redirect to the liability before current year page if the amount due is more than 0 in $mode" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = arbitrary[Registration].sample.get
          .copy(
            entityType = Some(arbitrary[EntityType].sample.get),
            relevantApRevenue = Some(randomApRevenue())
          )
        val ukRevenue      = revenueGen.sample.get
        val additionalInfo = arbitrary[RegistrationAdditionalInfo].sample.get

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration.copy(relevantAp12Months = Some(true)))

        val updatedRegistration = registration.copy(
          relevantAp12Months = Some(true),
          relevantApRevenue = Some(ukRevenue),
          revenueMeetsThreshold = Some(true)
        )
        stubUpsertRegistration(updatedRegistration)
        stubCalculateLiability(
          CalculateLiabilityRequest(
            EclTaxYear.yearInDays,
            EclTaxYear.yearInDays,
            ukRevenue,
            EclTaxYear.fromCurrentDate().startYear
          ),
          liable = true
        )

        val result = callRoute(
          FakeRequest(routes.UkRevenueController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", ukRevenue.toString))
        )

        status(result) shouldBe SEE_OTHER

        mode match {
          case NormalMode =>
            redirectLocation(result) shouldBe Some(
              routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url
            )
          case CheckMode  =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad().url
            )
        }
      }
    }

    s"save the UK revenue then redirect to the liable for previous year page if the amount due is 0 in $mode" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = arbitrary[Registration].sample.get
        .copy(
          entityType = Some(arbitrary[EntityType].sample.get),
          internalId = testInternalId,
          relevantApRevenue = Some(randomApRevenue())
        )
      val ukRevenue    = revenueGen.sample.get
      stubGetRegistrationWithEmptyAdditionalInfo(registration.copy(relevantAp12Months = Some(true)))

      val updatedRegistration = registration.copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue),
        revenueMeetsThreshold = Some(false)
      )
      val additionalInfo      = arbitrary[RegistrationAdditionalInfo].sample.get

      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubUpsertRegistration(updatedRegistration)

      stubCalculateLiability(
        CalculateLiabilityRequest(
          EclTaxYear.yearInDays,
          EclTaxYear.yearInDays,
          ukRevenue,
          EclTaxYear.fromCurrentDate().startYear
        ),
        liable = false
      )

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit(mode))
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.LiabilityBeforeCurrentYearController.onPageLoad(mode).url
      )
    }
  }
}
