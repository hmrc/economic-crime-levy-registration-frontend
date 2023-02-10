package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EclSubscriptionStatus._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.{IncorporatedEntityJourneyData, PartnershipEntityJourneyData, SoleTraderEntityJourneyData}
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclSubscriptionStatus, NormalMode, Registration}

class GrsContinueISpec extends ISpecBase with AuthorisedBehaviour {

  val journeyId: String         = "test-journey-id"
  val businessPartnerId: String = "test-business-partner-id"

  s"GET ${routes.GrsContinueController.continue(NormalMode, journeyId).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.GrsContinueController.continue(NormalMode, journeyId))

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

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
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

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.organisationAlreadyRegistered(testEclRegistrationReference).url)
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

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
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

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.organisationAlreadyRegistered(testEclRegistrationReference).url)
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

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
    }

    "retrieve the partnership entity GRS journey data, update the registration with the GRS journey data and handle the GRS/BV response where already registered" in {
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
        EclSubscriptionStatus(Subscribed(testEclRegistrationReference))
      )

      val result = callRoute(FakeRequest(routes.GrsContinueController.continue(NormalMode, journeyId)))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.NotableErrorController.organisationAlreadyRegistered(testEclRegistrationReference).url)
    }
  }

}
