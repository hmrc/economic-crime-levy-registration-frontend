package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.{Registration, SoleTrader, UkLimitedCompany}

class GrsContinueISpec extends ISpecBase with AuthorisedBehaviour {

  val journeyId: String = "test-journey-id"

  s"GET /$contextPath/grs-continue" should {
    behave like authorisedActionRoute(routes.GrsContinueController.continue(journeyId))

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

      val incorporatedEntityJourneyData = random[IncorporatedEntityJourneyData]

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData = Some(incorporatedEntityJourneyData)
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

      val soleTraderEntityJourneyData = random[SoleTraderEntityJourneyData]

      val updatedRegistration = registration.copy(
        soleTraderEntityJourneyData = Some(soleTraderEntityJourneyData)
      )

      stubGetSoleTraderEntityJourneyData(journeyId, soleTraderEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsJson(result) shouldBe Json.toJson(soleTraderEntityJourneyData)
    }

    "retrieve the partnership entity GRS journey data, update the registration with the GRS journey data and (display the GRS journey data)" in {
      stubAuthorised()

      val entityType   = random[PartnershipType].entityType
      val registration =
        random[Registration]
          .copy(
            entityType = Some(entityType),
            incorporatedEntityJourneyData = None,
            soleTraderEntityJourneyData = None,
            partnershipEntityJourneyData = None
          )

      stubGetRegistration(registration)

      val partnershipEntityJourneyData = random[PartnershipEntityJourneyData]

      val updatedRegistration = registration.copy(
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
