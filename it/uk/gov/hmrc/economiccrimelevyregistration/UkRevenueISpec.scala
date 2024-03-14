package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

  val minRevenue = 0L
  val maxRevenue = 99999999999L

  val revenueGen: Gen[Long] = Gen.chooseNum[Long](minRevenue, maxRevenue)

  s"GET ${routes.UkRevenueController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.UkRevenueController.onPageLoad(NormalMode))

    "respond with 200 status and the UK revenue view" in {
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

      val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Total UK revenue")
    }
  }

  s"POST ${routes.UkRevenueController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.UkRevenueController.onSubmit(NormalMode))

    "save the UK revenue then redirect to the liability before current year page if the amount due is more than 0" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val ukRevenue      = revenueGen.sample.get
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration.copy(relevantAp12Months = Some(true)))

      val updatedRegistration = registration.copy(
        relevantAp12Months = Some(true),
        relevantApRevenue = Some(ukRevenue),
        revenueMeetsThreshold = Some(true)
      )
      stubUpsertRegistration(updatedRegistration)
      stubCalculateLiability(
        CalculateLiabilityRequest(EclTaxYear.yearInDays, EclTaxYear.yearInDays, ukRevenue),
        liable = true
      )

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url
      )
    }

    "save the UK revenue then redirect to the liable for previous year page if the amount due is 0" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
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
      val additionalInfo      = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubUpsertRegistration(updatedRegistration)

      stubCalculateLiability(
        CalculateLiabilityRequest(EclTaxYear.yearInDays, EclTaxYear.yearInDays, ukRevenue),
        liable = false
      )

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url
      )
    }
  }
}
