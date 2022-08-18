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

import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions._
import uk.gov.hmrc.economiccrimelevyregistration.forms.ExampleMultipleQuestionsFormProvider
import javax.inject.Inject
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.navigation.Navigator
import uk.gov.hmrc.economiccrimelevyregistration.pages.ExampleMultipleQuestionsPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ExampleMultipleQuestionsView
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector

import scala.concurrent.{ExecutionContext, Future}

class ExampleMultipleQuestionsController @Inject() (
  override val messagesApi: MessagesApi,
  eclRegistrationConnector: EclRegistrationConnector,
  navigator: Navigator,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  formProvider: ExampleMultipleQuestionsFormProvider,
  val controllerComponents: MessagesControllerComponents,
  view: ExampleMultipleQuestionsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    val preparedForm = request.registration.exampleMultipleQuestions match {
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
          val updatedRegistration = request.registration.copy(exampleMultipleQuestions = Some(value))

          eclRegistrationConnector.updateRegistration(updatedRegistration).map { registration =>
            Redirect(navigator.nextPage(ExampleMultipleQuestionsPage, mode, registration))
          }
        }
      )
  }
}
