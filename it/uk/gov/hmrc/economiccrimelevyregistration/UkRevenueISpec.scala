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

  s"GET ${routes.UkRevenueController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad())

    "respond with 200 status and the UK revenue view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("What was your UK revenue for the relevant accounting period?")
    }
  }

  s"POST ${routes.UkRevenueController.onSubmit().url}"  should {
    behave like authorisedActionRoute(routes.UkRevenueController.onSubmit())

    "save the UK revenue then redirect to the entity type page if the amount due is more than 0" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val ukRevenue    = revenueGen.sample.get

      stubGetRegistration(registration.copy(relevantAp12Months = Some(true)))

      val updatedRegistration = registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

      stubUpsertRegistration(updatedRegistration)
      stubCalculateLiability(
        CalculateLiabilityRequest(EclTaxYear.yearInDays, EclTaxYear.yearInDays, ukRevenue),
        liable = true
      )

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit())
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.EntityTypeController.onPageLoad().url)
    }

    "save the UK revenue then redirect to the not liable page if the amount due is 0" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]
      val ukRevenue    = revenueGen.sample.get

      stubGetRegistration(registration.copy(relevantAp12Months = Some(true)))

      val updatedRegistration = registration.copy(relevantAp12Months = Some(true), relevantApRevenue = Some(ukRevenue))

      stubUpsertRegistration(updatedRegistration)
      stubCalculateLiability(
        CalculateLiabilityRequest(EclTaxYear.yearInDays, EclTaxYear.yearInDays, ukRevenue),
        liable = false
      )

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit())
          .withFormUrlEncodedBody(("value", ukRevenue.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotLiableController.onPageLoad().url)
    }
  }
}
