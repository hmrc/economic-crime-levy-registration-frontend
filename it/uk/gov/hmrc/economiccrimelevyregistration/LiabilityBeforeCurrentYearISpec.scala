package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import RegistrationType.Initial
import uk.gov.hmrc.time.TaxYear

class LiabilityBeforeCurrentYearISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode)
    )

    "respond with 200 status and the liability before current year HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration   = random[Registration]
      val additionalInfo = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.LiabilityBeforeCurrentYearController.onPageLoad(NormalMode)))

      status(result) shouldBe OK

      html(result) should include("Were you liable to pay the ECL from 1 April 2022 to 31 March 2023?")
    }
  }

  s"POST ${routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode).url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode)
    )

    "save the selected address option then redirect to the address lookup frontend journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = random[Boolean]
      val registration            = random[Registration].copy(
        registrationType = Some(Initial)
      )
      val additionalInfo          = random[RegistrationAdditionalInfo]

      stubGetRegistrationAdditionalInfo(additionalInfo)
      stubGetRegistration(registration)

      val liabilityYear = (registration.carriedOutAmlRegulatedActivityInCurrentFy, liableBeforeCurrentYear) match {
        case (Some(_), true)     => Some(LiabilityYear(TaxYear.current.previous.startYear))
        case (Some(true), false) => Some(LiabilityYear(TaxYear.current.currentYear))
        case _                   => None
      }

      val info = RegistrationAdditionalInfo(
        registration.internalId,
        liabilityYear,
        None
      )

      stubUpsertRegistrationAdditionalInfo(info)
      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit(NormalMode))
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      val call: Call = if (liableBeforeCurrentYear) {
        routes.AmlSupervisorController.onPageLoad(NormalMode, Initial)
      } else {
        routes.NotLiableController.youDoNotNeedToRegister()
      }

      redirectLocation(result) shouldBe Some(call.url)
    }
  }
}
