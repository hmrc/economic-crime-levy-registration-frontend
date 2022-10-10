package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Arbitrary
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.UkLimitedCompany
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.IncorporatedEntityJourneyData

class GrsContinueISpec extends ISpecBase {

  s"GET /$contextPath/grs-continue" should {
    "retrieve the GRS journey data, update the registration with the GRS journey data and (display the GRS journey data)" in {
      stubAuthorised()

      val registration =
        Arbitrary
          .arbitrary[models.Registration]
          .sample
          .get
          .copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = None
          )

      stubGetRegistration(Json.toJson(registration))

      val journeyId: String = "test-journey-id"

      val incorporatedEntityJourneyData = Arbitrary.arbitrary[IncorporatedEntityJourneyData].sample.get

      val updatedRegistration = registration.copy(incorporatedEntityJourneyData = Some(incorporatedEntityJourneyData))

      stubGetJourneyData(journeyId, Json.toJson(incorporatedEntityJourneyData))

      stubUpsertRegistration(Json.toJson(updatedRegistration))

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.toJson(incorporatedEntityJourneyData)
    }
  }

}
