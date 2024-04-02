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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.CtUtrPostcodeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.CtUtrPostcodePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{CtUtrPostcodeView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CtUtrPostcodeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  storeUrl: StoreUrlAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: CtUtrPostcodeFormProvider,
  pageNavigator: CtUtrPostcodePageNavigator,
  view: CtUtrPostcodeView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  val form: Form[String]                         = formProvider()
  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData andThen storeUrl) {
    implicit request =>
      Ok(view(form.prepare(request.registration.otherEntityJourneyData.postcode), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        postcode => {
          val otherEntity         = request.registration.otherEntityJourneyData.copy(postcode = Some(postcode))
          val updatedRegistration = request.registration.copy(optOtherEntityJourneyData = Some(otherEntity))
          (for {
            _ <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
          } yield EclRegistrationModel(updatedRegistration)).convertToResult(mode, pageNavigator)
        }
      )
  }
}
