package uk.gov.hmrc.economiccrimelevyregistration

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

class GrsContinueISpec extends ISpecBase {

  s"GET /$contextPath/grs-continue" should {
    "retrieve the GRS journey data, update the registration with the GRS journey data and (display the GRS journey data)" in {
      stubAuthorised()
      stubGetRegistration()

      val journeyId: String = "journeyId"

      val registrationJson: String =
        """
          |{
          |    "internalId" : "test-id",
          |    "entityType" : "UkLimitedCompany",
          |    "incorporatedEntityJourneyData" : {
          |        "companyProfile" : {
          |            "companyName" : "Test Company Ltd",
          |            "companyNumber" : "01234567",
          |            "unsanitisedCHROAddress" : {
          |                "address_line_1" : "testLine1",
          |                "address_line_2" : "test town",
          |                "care_of" : "test name",
          |                "country" : "United Kingdom",
          |                "locality" : "test city",
          |                "po_box" : "123",
          |                "postal_code" : "AA11AA",
          |                "premises" : "1",
          |                "region" : "test region"
          |            }
          |        },
          |        "ctutr" : "1234567890",
          |        "identifiersMatch" : true,
          |        "businessVerification" : {
          |            "verificationStatus" : "PASS"
          |        },
          |        "registration" : {
          |            "registrationStatus" : "REGISTERED",
          |            "registeredBusinessPartnerId" : "X00000123456789"
          |        }
          |    }
          |}
        """.stripMargin

      stubGetJourneyData(journeyId)
      stubUpsertRegistration(registrationJson)

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      html(result) should include("UkLimitedCompany")
    }
  }

}
