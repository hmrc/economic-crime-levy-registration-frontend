package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, SoleTrader, UkLimitedCompany}

class EntityTypeISpec extends ISpecBase {

  s"GET /$contextPath/select-entity-type"  should {
    "respond with 200 status and the select entity type HTML view" in {
      stubAuthorised()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.EntityTypeController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("What is your entity type?")
    }
  }

  s"POST /$contextPath/select-entity-type" should {
    "save the selected entity type then redirect to the GRS UK Limited Company journey when the UK Limited Company option is selected" in {
      stubAuthorised()

      val registration = random[Registration]

      stubGetRegistration(registration)
      stubCreateLimitedCompanyJourney()

      val updatedRegistration = registration.copy(entityType = Some(UkLimitedCompany))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.EntityTypeController.onSubmit()).withFormUrlEncodedBody(("value", "UkLimitedCompany"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }

    "save the selected entity type then redirect to the GRS Sole Trader journey when the Sole Trader option is selected" in {
      stubAuthorised()

      val registration = random[Registration]

      stubGetRegistration(registration)
      stubCreateSoleTraderJourney()

      val updatedRegistration = registration.copy(entityType = Some(SoleTrader))

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.EntityTypeController.onSubmit()).withFormUrlEncodedBody(("value", "SoleTrader"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }
  }

}
