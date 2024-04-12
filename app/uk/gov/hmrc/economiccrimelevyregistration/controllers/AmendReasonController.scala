/*
 * Copyright 2024 HM Revenue & Customs
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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Mode, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmendReasonPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendReasonView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendReasonController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  view: AmendReasonView,
  formProvider: AmendReasonFormProvider,
  registrationService: EclRegistrationService,
  pageNavigator: AmendReasonPageNavigator
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {
  val form: Form[String]                         = formProvider()
  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(
      view(
        form.prepare(request.registration.amendReason),
        mode,
        request.registration.registrationType,
        request.eclRegistrationReference
      )
    ).withSession(
      request.session ++ Seq(
        SessionKeys.registrationType -> Json.stringify(Json.toJson(request.registration.registrationType))
      )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        errors =>
          Future.successful(
            BadRequest(view(errors, mode, request.registration.registrationType, request.eclRegistrationReference))
          ),
        input => {
          val updatedRegistration = request.registration.copy(amendReason = Some(input))
          registrationService
            .upsertRegistration(updatedRegistration)
            .asResponseError
            .fold(
              err => routeError(err),
              _ => Redirect(pageNavigator.nextPage(mode, EclRegistrationModel(updatedRegistration)))
            )
        }
      )
  }
}
