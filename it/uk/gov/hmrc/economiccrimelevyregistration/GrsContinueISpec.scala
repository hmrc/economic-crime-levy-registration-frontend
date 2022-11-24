package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.{derivedArbitrary, random}
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, NotSubscribed, Registration, Subscribed}

class GrsContinueISpec extends ISpecBase with AuthorisedBehaviour {

  val journeyId: String         = "test-journey-id"
  val businessPartnerId: String = "test-business-partner-id"

  s"GET ${routes.GrsContinueController.continue(journeyId).url}" should {
    behave like authorisedActionRoute(routes.GrsContinueController.continue(journeyId))

    "retrieve the incorporated entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response to continue the registration journey" in {
      stubAuthorisedWithNoGroupEnrolment()

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

      val updatedIncorporatedEntityJourneyData = incorporatedEntityJourneyData.copy(
        identifiersMatch = true,
        registration = successfulGrsRegistrationResult(businessPartnerId),
        businessVerification = None
      )

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData)
      )

      stubGetGrsJourneyData(
        s"/incorporated-entity-identification/api/journey/$journeyId",
        updatedIncorporatedEntityJourneyData
      )

      stubUpsertRegistration(updatedRegistration)
      stubGetSubscriptionStatus(
        updatedIncorporatedEntityJourneyData.registration.registeredBusinessPartnerId.get,
        EclSubscriptionStatus(NotSubscribed)
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe "Success - you can continue registering for ECL"
    }

    "retrieve the incorporated entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response where already registered" in {
      stubAuthorisedWithNoGroupEnrolment()

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

      val updatedIncorporatedEntityJourneyData = incorporatedEntityJourneyData.copy(
        identifiersMatch = true,
        registration = successfulGrsRegistrationResult(businessPartnerId),
        businessVerification = None
      )

      val updatedRegistration = registration.copy(
        incorporatedEntityJourneyData = Some(updatedIncorporatedEntityJourneyData)
      )

      stubGetGrsJourneyData(
        s"/incorporated-entity-identification/api/journey/$journeyId",
        updatedIncorporatedEntityJourneyData
      )

      stubUpsertRegistration(updatedRegistration)
      stubGetSubscriptionStatus(
        updatedIncorporatedEntityJourneyData.registration.registeredBusinessPartnerId.get,
        EclSubscriptionStatus(Subscribed(testEclRegistrationReference))
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe s"Business is already subscribed to ECL with registration reference $testEclRegistrationReference"
    }

    "retrieve the sole trader entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response to continue the registration journey" in {
      stubAuthorisedWithNoGroupEnrolment()

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

      val updatedSoleTraderEntityJourneyData = soleTraderEntityJourneyData.copy(
        identifiersMatch = true,
        registration = successfulGrsRegistrationResult(businessPartnerId),
        businessVerification = None
      )

      val updatedRegistration = registration.copy(
        soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData)
      )

      stubGetGrsJourneyData(s"/sole-trader-identification/api/journey/$journeyId", updatedSoleTraderEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)
      stubGetSubscriptionStatus(
        updatedSoleTraderEntityJourneyData.registration.registeredBusinessPartnerId.get,
        EclSubscriptionStatus(NotSubscribed)
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe "Success - you can continue registering for ECL"
    }

    "retrieve the sole trader entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response where already registered" in {
      stubAuthorisedWithNoGroupEnrolment()

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

      val updatedSoleTraderEntityJourneyData = soleTraderEntityJourneyData.copy(
        identifiersMatch = true,
        registration = successfulGrsRegistrationResult(businessPartnerId),
        businessVerification = None
      )

      val updatedRegistration = registration.copy(
        soleTraderEntityJourneyData = Some(updatedSoleTraderEntityJourneyData)
      )

      stubGetGrsJourneyData(s"/sole-trader-identification/api/journey/$journeyId", updatedSoleTraderEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)
      stubGetSubscriptionStatus(
        updatedSoleTraderEntityJourneyData.registration.registeredBusinessPartnerId.get,
        EclSubscriptionStatus(Subscribed(testEclRegistrationReference))
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe s"Business is already subscribed to ECL with registration reference $testEclRegistrationReference"
    }

    "retrieve the partnership entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response to continue the registration journey" in {
      stubAuthorisedWithNoGroupEnrolment()

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

      val updatedPartnershipEntityJourneyData = partnershipEntityJourneyData.copy(
        identifiersMatch = true,
        registration = successfulGrsRegistrationResult(businessPartnerId),
        businessVerification = None
      )

      val updatedRegistration = registration.copy(
        partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
      )

      stubGetGrsJourneyData(s"/partnership-identification/api/journey/$journeyId", updatedPartnershipEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)
      stubGetSubscriptionStatus(
        updatedPartnershipEntityJourneyData.registration.registeredBusinessPartnerId.get,
        EclSubscriptionStatus(NotSubscribed)
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe "Success - you can continue registering for ECL"
    }

    "retrieve the partnership entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response where already registered" in {
      stubAuthorisedWithNoGroupEnrolment()

      val entityType = random[PartnershipType].entityType
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

      val updatedPartnershipEntityJourneyData = partnershipEntityJourneyData.copy(
        identifiersMatch = true,
        registration = successfulGrsRegistrationResult(businessPartnerId),
        businessVerification = None
      )

      val updatedRegistration = registration.copy(
        partnershipEntityJourneyData = Some(updatedPartnershipEntityJourneyData)
      )

      stubGetGrsJourneyData(s"/partnership-identification/api/journey/$journeyId", updatedPartnershipEntityJourneyData)

      stubUpsertRegistration(updatedRegistration)
      stubGetSubscriptionStatus(
        updatedPartnershipEntityJourneyData.registration.registeredBusinessPartnerId.get,
        EclSubscriptionStatus(Subscribed(testEclRegistrationReference))
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(journeyId)))

      status(result) shouldBe OK

      contentAsString(result) shouldBe s"Business is already subscribed to ECL with registration reference $testEclRegistrationReference"
    }
  }

}
