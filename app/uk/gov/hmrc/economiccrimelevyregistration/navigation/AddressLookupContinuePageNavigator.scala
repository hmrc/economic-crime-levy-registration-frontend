package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes

import uk.gov.hmrc.economiccrimelevyregistration.models.Registration

class AddressLookupContinuePageNavigator extends PageNavigator {

  override protected def navigateInNormalMode(registration: Registration): Call = navigateInBothModes

  override protected def navigateInCheckMode(registration: Registration): Call =
    navigateInBothModes

  private def navigateInBothModes: Call = routes.CheckYourAnswersController.onPageLoad()
}
