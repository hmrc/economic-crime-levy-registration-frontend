package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.mvc.Call
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.addresslookup._

class LiabilityBeforeCurrentYearISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.LiabilityBeforeCurrentYearController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.LiabilityBeforeCurrentYearController.onPageLoad())

    "respond with 200 status and the liability before current year HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.LiabilityBeforeCurrentYearController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Were you liable to pay the ECL from 1 April 2022 to 31 March 2023?")
    }
  }

  s"POST ${routes.LiabilityBeforeCurrentYearController.onSubmit().url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.LiabilityBeforeCurrentYearController.onSubmit())

    "save the selected address option then redirect to the address lookup frontend journey" in {
      stubAuthorisedWithNoGroupEnrolment()

      val liableBeforeCurrentYear = random[Boolean]
      val registration       = random[Registration]

      stubGetRegistration(registration)

      stubUpsertRegistration(registration)

      val result = callRoute(
        FakeRequest(routes.LiabilityBeforeCurrentYearController.onSubmit())
          .withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
      )

      status(result) shouldBe SEE_OTHER

      val call: Call = if (liableBeforeCurrentYear) {
        routes.EntityTypeController.onPageLoad(NormalMode)
      } else {
        registration.relevantApRevenue match {
          case Some(_) => registration.revenueMeetsThreshold match {
            case Some(true) => routes.EntityTypeController.onPageLoad(NormalMode)
            case _          => routes.NotLiableController.youDoNotNeedToRegister()
          }
          case None    => routes.NotLiableController.youDoNotNeedToRegister()
        }
      }

      redirectLocation(result) shouldBe Some(call.url)
    }
  }
}
