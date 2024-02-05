package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class EntityTypeISpec extends ISpecBase with AuthorisedBehaviour {

  private def randomRegistration() =
    random[Registration].copy(
      optOtherEntityJourneyData = None
    )

  s"GET ${routes.EntityTypeController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.EntityTypeController.onPageLoad(NormalMode))

    "respond with 200 status and the select entity type HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = randomRegistration()
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.EntityTypeController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("What is your entity type?")
    }
  }

  s"POST ${routes.EntityTypeController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.EntityTypeController.onSubmit(NormalMode))

    "save the selected entity type then redirect to the GRS Incorporated Entity journey when one of the Incorporated Entity type options is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = randomRegistration()

      val entityType = random[IncorporatedEntityType].entityType

      val urlIncorporatedEntityType: String = entityType match {
        case UkLimitedCompany | UnlimitedCompany => "limited-company-journey"
        case RegisteredSociety                   => "registered-society-journey"
        case e                                   => fail(s"$e is not a valid incorporated entity type")
      }
      val additionalInfo                    = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)

      stubGetRegistration(registration)

      stubCreateGrsJourney(s"/incorporated-entity-identification/api/$urlIncorporatedEntityType")

      val updatedRegistration = registration.copy(
        entityType = Some(entityType),
        incorporatedEntityJourneyData = None,
        soleTraderEntityJourneyData = None,
        partnershipEntityJourneyData = None,
        partnershipName = None
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.EntityTypeController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", entityType.toString))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }

    "save the selected entity type then redirect to the GRS Sole Trader journey when the Sole Trader option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = randomRegistration()
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)
      stubCreateGrsJourney("/sole-trader-identification/api/sole-trader-journey")

      val updatedRegistration = registration.copy(
        entityType = Some(SoleTrader),
        incorporatedEntityJourneyData = None,
        soleTraderEntityJourneyData = None,
        partnershipEntityJourneyData = None,
        partnershipName = None
      )

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.EntityTypeController.onSubmit(NormalMode)).withFormUrlEncodedBody(("value", "SoleTrader"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }
  }

  "save the selected entity type then redirect to the GRS Partnership journey when the Partnership option is selected" in {
    stubAuthorisedWithNoGroupEnrolment()

    val registration = randomRegistration()
    val entityType   = random[PartnershipType].entityType

    val urlPartnershipType: String = entityType match {
      case GeneralPartnership          => "general-partnership-journey"
      case ScottishPartnership         => "scottish-partnership-journey"
      case LimitedPartnership          => "limited-partnership-journey"
      case ScottishLimitedPartnership  => "scottish-limited-partnership-journey"
      case LimitedLiabilityPartnership => "limited-liability-partnership-journey"
      case e                           => fail(s"$e is not a valid partnership type")
    }
    val additionalInfo             = random[RegistrationAdditionalInfo]

    stubGetRegistrationAdditionalInfo(additionalInfo)
    stubGetRegistration(registration)
    stubCreateGrsJourney(s"/partnership-identification/api/$urlPartnershipType")

    val updatedRegistration = registration.copy(
      entityType = Some(entityType),
      incorporatedEntityJourneyData = None,
      soleTraderEntityJourneyData = None,
      partnershipEntityJourneyData = None,
      partnershipName = None
    )

    stubUpsertRegistration(updatedRegistration)

    val result = callRoute(
      FakeRequest(routes.EntityTypeController.onSubmit(NormalMode))
        .withFormUrlEncodedBody(("value", entityType.toString))
    )

    status(result) shouldBe SEE_OTHER

    redirectLocation(result) shouldBe Some("test-url")
  }

}
