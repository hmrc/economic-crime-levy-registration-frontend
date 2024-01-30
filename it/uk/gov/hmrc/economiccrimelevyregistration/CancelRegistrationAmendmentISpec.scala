package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

class CancelRegistrationAmendmentISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CancelRegistrationAmendmentController.onPageLoad().url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(
      routes.CancelRegistrationAmendmentController.onPageLoad()
    )

    "respond with 200 status and the cancel registration view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val registration = random[Registration]

      stubGetRegistration(registration)

      val result = callRoute(FakeRequest(routes.CancelRegistrationAmendmentController.onPageLoad()))

      status(result) shouldBe OK

      html(result) should include("Are you sure you want to cancel your request to amend your registration?")
    }
  }

//  s"POST ${routes.CancelRegistrationAmendmentController.onSubmit(NormalMode).url}"  should {
//    behave like authorisedActionWithEnrolmentCheckRoute(routes.CancelRegistrationAmendmentController.onSubmit(NormalMode))
//
//    "save the selected option then redirect to the UK revenue page when the Yes option is selected" in {
//      stubAuthorisedWithNoGroupEnrolment()
//
//      val registration = random[Registration]
//
//      stubGetRegistration(registration)
//
//      val updatedRegistration =
//        registration.copy(relevantAp12Months = Some(true), relevantApLength = None, revenueMeetsThreshold = None)
//
//      stubUpsertRegistration(updatedRegistration)
//
//      val result = callRoute(
//        FakeRequest(routes.CancelRegistrationAmendmentController.onSubmit(NormalMode)).withFormUrlEncodedBody(("value", "true"))
//      )
//
//      status(result) shouldBe SEE_OTHER
//
//      redirectLocation(result) shouldBe Some(routes.UkRevenueController.onPageLoad(NormalMode).url)
//    }
//  }
}
