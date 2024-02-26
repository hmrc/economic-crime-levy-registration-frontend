package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EntityType, NormalMode, Registration, RegistrationAdditionalInfo, SessionData}

class RegisterForCurrentYearControllerISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.RegisterForCurrentYearController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.RegisterForCurrentYearController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the register for current year HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()
      stubGetSession(SessionData(random[String], Map()))

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.RegisterForCurrentYearController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("enter the date you became liable for ECL")
    }

    s"POST ${routes.RegisterForCurrentYearController.onSubmit(CheckMode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.RegisterForCurrentYearController.onSubmit(CheckMode)
      )

      "save the entered option then redirect to the aml regulated activity" in {
        stubAuthorisedWithNoGroupEnrolment()
        stubGetSession(SessionData(random[String], Map()))

        val registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
          .copy(
            registrationType = Some(Initial)
          )

        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistration(registration)
        val info = RegistrationAdditionalInfo(
          testInternalId,
          None,
          None,
          Some(true),
          None,
          None
        )

        stubUpsertRegistrationAdditionalInfo(info)
        stubUpsertRegistration(registration)

        val result = callRoute(
          FakeRequest(routes.RegisterForCurrentYearController.onSubmit(CheckMode))
            .withFormUrlEncodedBody(
              ("value", "true")
            )
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.AmlRegulatedActivityController.onPageLoad(NormalMode).url)

      }
    }
  }
}
