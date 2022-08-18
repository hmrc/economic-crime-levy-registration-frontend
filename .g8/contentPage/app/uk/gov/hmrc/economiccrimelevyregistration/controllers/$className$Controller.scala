package uk.gov.hmrc.economiccrimelevyregistration.controllers

import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions._
import javax.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.economiccrimelevyregistration.views.html.$className$View

class $className$Controller @Inject()(
                                       override val messagesApi: MessagesApi,
                                       authorise: AuthorisedAction,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: $className$View
                                     ) extends FrontendBaseController with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise {
    implicit request =>
      Ok(view())
  }
}
