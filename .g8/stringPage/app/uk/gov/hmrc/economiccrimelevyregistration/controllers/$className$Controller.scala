package uk.gov.hmrc.economiccrimelevyregistration.controllers

import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions._
import uk.gov.hmrc.economiccrimelevyregistration.forms.$className$FormProvider
import javax.inject.Inject
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.navigation.Navigator
import uk.gov.hmrc.economiccrimelevyregistration.pages.$className$Page
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.economiccrimelevyregistration.views.html.$className$View
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector

import scala.concurrent.{ExecutionContext, Future}

class $className$Controller @Inject()(
                                       override val messagesApi: MessagesApi,
                                       eclRegistrationConnector: EclRegistrationConnector,
                                       navigator: Navigator,
                                       authorise: AuthorisedAction,
                                       getRegistrationData: DataRetrievalAction,
                                       formProvider: $className$FormProvider,
                                       val controllerComponents: MessagesControllerComponents,
                                       view: $className$View
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) {
    implicit request =>

      val preparedForm = request.registration.??? match { //TODO Choose the data you want to fill the form with
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value => {
          val updatedRegistration = request.registration.copy(??? = Some(value)) //TODO Choose the data you want to update

          eclRegistrationConnector.upsertRegistration(updatedRegistration).map { registration =>
            Redirect(navigator.nextPage($className$Page, mode, registration))
          }
        }
      )
  }
}
