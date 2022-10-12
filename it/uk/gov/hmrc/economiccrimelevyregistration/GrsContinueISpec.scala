package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.{Partnership, Registration, SoleTrader, UkLimitedCompany}

class GrsContinueISpec extends ISpecBase {

  s"GET /$contextPath/grs-continue" should {
    "retrieve the incorporated entity GRS journey data, update the registration with the GRS journey data and (display the GRS journey data)" in {
      stubAuthorised()

      val registration =
        random[Registration]
          .copy(
            entityType = Some(UkLimitedCompany),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

      stubGetRegistration(registration)

      val journeyId: String = "test-journey-id"

      val incorporatedEntityJourneyData = random[IncorporatedEntityJourneyData]

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData = Some(incorporatedEntityJourneyData),
        soleTraderEntityJourneyData = None,
        partnershipEntityJourneyData = None
      )

      stubGetIncorporatedEntityJourneyData(journeyId, incorporatedEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.toJson(incorporatedEntityJourneyData)
    }

    "retrieve the sole trader entity GRS journey data, update the registration with the GRS journey data and (display the GRS journey data)" in {
      stubAuthorised()

      val registration =
        random[Registration]
          .copy(
            entityType = Some(SoleTrader),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

      stubGetRegistration(registration)

      val journeyId: String = "test-journey-id"

      val soleTraderEntityJourneyData = random[SoleTraderEntityJourneyData]

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData = None,
        soleTraderEntityJourneyData = Some(soleTraderEntityJourneyData),
        partnershipEntityJourneyData = None
      )

      stubGetSoleTraderEntityJourneyData(journeyId, soleTraderEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.toJson(soleTraderEntityJourneyData)
    }

    "retrieve the partnership entity GRS journey data, update the registration with the GRS journey data and (display the GRS journey data)" in {
      stubAuthorised()

      val registration =
        random[Registration]
          .copy(
            entityType = Some(Partnership),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

      stubGetRegistration(registration)

      val journeyId: String = "test-journey-id"

      val partnershipEntityJourneyData = random[PartnershipEntityJourneyData]

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData = None,
        soleTraderEntityJourneyData = None,
        partnershipEntityJourneyData = Some(partnershipEntityJourneyData)
      )

      stubGetPartnershipEntityJourneyData(journeyId, partnershipEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.toJson(partnershipEntityJourneyData)
    }
  }

}
