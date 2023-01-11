package uk.gov.hmrc.economiccrimelevyregistration

import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

class CheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.CheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AmlSupervisorController.onPageLoad())

    "respond with 200 status and the Check your answers HTML view" in {
      pending
    }

    "redirect to the start page when the registration data is invalid" in {
      pending
    }
  }

}
