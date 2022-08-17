/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.ExampleRadioButtonFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{ExampleRadioButton, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.Navigator
import uk.gov.hmrc.economiccrimelevyregistration.pages.ExampleRadioButtonPage
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ExampleRadioButtonView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ExampleRadioButtonController @Inject() (
  override val messagesApi: MessagesApi,
  navigator: Navigator,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  formProvider: ExampleRadioButtonFormProvider,
  eclRegistrationConnector: EclRegistrationConnector,
  val controllerComponents: MessagesControllerComponents,
  view: ExampleRadioButtonView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[ExampleRadioButton] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    val preparedForm = request.registration.exampleRadioButton match {
      case None        => form
      case Some(value) => form.fill(value)
    }

    Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        value => {
          val updatedRegistration = request.registration.copy(exampleRadioButton = Some(value))

          eclRegistrationConnector.updateRegistration(updatedRegistration).map { _ =>
            Redirect(navigator.nextPage(ExampleRadioButtonPage, mode, updatedRegistration))
          }
        }
      )
  }
}
