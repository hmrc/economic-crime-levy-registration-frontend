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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataRetrievalAction
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, ErrorHandler}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.deregister.DeregisterContactEmailFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.deregister.Navigator
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterContactEmailView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DeregisterContactEmailController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getDeregistrationData: DeregistrationDataRetrievalAction,
  deregistrationService: DeregistrationService,
  formProvider: DeregisterContactEmailFormProvider,
  view: DeregisterContactEmailView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler
    with Navigator {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getDeregistrationData) { implicit request =>
    Ok(
      view(
        form.prepare(request.deregistration.contactDetails.emailAddress),
        request.deregistration.contactDetails.name.getOrElse(""),
        mode,
        request.deregistration.registrationType
      )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getDeregistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                request.deregistration.contactDetails.name.getOrElse(""),
                mode,
                request.deregistration.registrationType
              )
            )
          ),
        email => {

          val updatedDeregistration = request.deregistration.copy(
            contactDetails = request.deregistration.contactDetails.copy(
              emailAddress = Some(email)
            )
          )

          (for {
            _ <- deregistrationService.upsert(updatedDeregistration).asResponseError
          } yield ()).fold(
            err => routeError(err),
            _ => toNextPage(mode, routes.DeregisterDateController.onPageLoad(mode))
          )
        }
      )
  }
}
