/*
 * Copyright 2023 HM Revenue & Customs
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
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.forms.SavedResponsesFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, SavedResponsesView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SavedResponsesController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  eclRegistrationService: EclRegistrationService,
  additionalInfoService: RegistrationAdditionalInfoService,
  sessionService: SessionService,
  formProvider: SavedResponsesFormProvider,
  view: SavedResponsesView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with ErrorHandler
    with BaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    Ok(view(form.prepare(None)))
  }

  def onSubmit: Action[AnyContent] = authorise.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors))),
        {
          case true  =>
            (for {
              urlToReturnTo <-
                sessionService.get(request.session, request.internalId, SessionKeys.UrlToReturnTo).asResponseError
            } yield urlToReturnTo).fold(
              err => routeError(err),
              urlToReturnTo => Redirect(urlToReturnTo)
            )
          case false =>
            (for {
              _ <- eclRegistrationService.deleteRegistration(request.internalId).asResponseError
              _ <- additionalInfoService.delete(request.internalId).asResponseError
            } yield ()).fold(
              err => routeError(err),
              _ => Redirect(routes.AmlRegulatedActivityController.onPageLoad(NormalMode))
            )
        }
      )
  }
}
