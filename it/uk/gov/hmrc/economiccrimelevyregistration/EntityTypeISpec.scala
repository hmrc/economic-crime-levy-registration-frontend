package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

class EntityTypeISpec extends ISpecBase {

  s"GET /$contextPath/select-entity-type"  should {
    "respond with 200 status and the select entity type HTML view" in {
      stubAuthorised()
      stubGetRegistration()

      val result = callRoute(FakeRequest(routes.EntityTypeController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("What is your entity type?")
    }
  }

  s"POST /$contextPath/select-entity-type" should {
    "save the selected entity type then redirect to the GRS UK Limited Company journey when the UK Limited Company option is selected" in {
      stubAuthorised()
      stubGetRegistration()
      stubCreateLimitedCompanyJourney()

      val registrationJson: String =
        """
          |{
          |    "internalId" : "test-id",
          |    "entityType" : "UkLimitedCompany"
          |}
        """.stripMargin

      stubUpsertRegistration(registrationJson)

      val result = callRoute(
        FakeRequest(routes.EntityTypeController.onSubmit()).withFormUrlEncodedBody(("value", "UkLimitedCompany"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        "http://localhost:9718/identify-your-incorporated-business/e9e5b979-26e8-4f33-90b0-7e5e092ed095/company-number"
      )
    }
  }

}
