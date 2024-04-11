package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models._

class RelevantAp12MonthsISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.RelevantAp12MonthsController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantAp12MonthsController.onPageLoad(mode))

      "respond with 200 status and the relevant AP 12 months view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.RelevantAp12MonthsController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Is your relevant accounting period 12 months?")
      }
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantAp12MonthsController.onSubmit(NormalMode))

    "save the selected option then redirect to the UK revenue page when the Yes option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration =
        registration.copy(relevantAp12Months = Some(true), relevantApLength = None, revenueMeetsThreshold = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(NormalMode)).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration =
        registration.copy(relevantAp12Months = Some(false), revenueMeetsThreshold = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(NormalMode)).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantApLengthController.onPageLoad(NormalMode).url)
    }
  }

  s"POST ${routes.RelevantAp12MonthsController.onSubmit(CheckMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.RelevantAp12MonthsController.onSubmit(CheckMode))

    "save the selected option then redirect to the CheckYourAnswers page when the Yes option is selected and the answer has not changed" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantAp12Months = Some(true),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration =
        registration.copy(relevantAp12Months = Some(true), relevantApLength = None, revenueMeetsThreshold = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(CheckMode)).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
      )
    }

    "save the selected option then redirect to the Uk Revenue page when the Yes option is selected and the answer has changed" in {
      stubAuthorisedWithNoGroupEnrolment()
      val validLength = Gen.chooseNum(apLengthMin, apLengthMax).sample.get

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantAp12Months = Some(false),
          relevantApRevenue = Some(randomApRevenue()),
          relevantApLength = Some(validLength)
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration =
        registration.copy(relevantAp12Months = Some(true), relevantApLength = None, revenueMeetsThreshold = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(CheckMode)).withFormUrlEncodedBody(("value", "true"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(CheckMode).url)
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected and the answer has not changed" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantAp12Months = Some(false),
          relevantApRevenue = Some(randomApRevenue())
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration =
        registration.copy(relevantAp12Months = Some(false), revenueMeetsThreshold = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(CheckMode)).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
      )
    }

    "save the selected option then redirect to the relevant AP length page when the No option is selected and the answer has changed" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
        .copy(
          entityType = Some(random[EntityType]),
          relevantAp12Months = Some(true),
          relevantApRevenue = Some(randomApRevenue()),
          relevantApLength = None
        )
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistrationWithEmptyAdditionalInfo(registration)

      val updatedRegistration =
        registration.copy(relevantAp12Months = Some(false), revenueMeetsThreshold = None, relevantApLength = None)

      stubUpsertRegistration(updatedRegistration)

      val result = callRoute(
        FakeRequest(routes.RelevantAp12MonthsController.onSubmit(CheckMode)).withFormUrlEncodedBody(("value", "false"))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(routes.RelevantApLengthController.onPageLoad(CheckMode).url)
    }

  }

}
