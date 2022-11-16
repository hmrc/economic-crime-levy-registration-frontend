package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

class UkRevenueISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET /$contextPath/what-was-your-uk-revenue" should {
    behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad())

    "respond with 200 status and the uk revenue HTML view" in {
      stubAuthorised()
      stubNoGroupEnrolment()

      val expectedTaxYear = EclTaxYear.currentFinancialYear

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.UkRevenueController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include(s"What was your UK revenue in $expectedTaxYear?")
    }
  }

  s"POST /$contextPath/what-was-your-uk-revenue" should {
    behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad())

    "save the selected Uk revenue option then display 'Ineligible' when the LessThan option is selected" in {
      stubAuthorised()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(false))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.UkRevenueController.onSubmit()).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe OK

      contentAsString(result) shouldBe "Ineligible"
    }

    s"POST /$contextPath/what-was-your-uk-revenue" should {
      behave like authorisedActionRoute(routes.UkRevenueController.onPageLoad())

      "save the selected Uk revenue option then display 'Proceed' when the EqualToOrGreaterThan option is selected" in {
        stubAuthorised()

        val registration = random[Registration]

        stubGetRegistration(registration)

        val updatedRegistration = registration.copy(meetsRevenueThreshold = Some(true))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.UkRevenueController.onSubmit()).withFormUrlEncodedBody(("value", "true"))
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe "Proceed"
      }
    }
  }
}
