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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.RegisterForCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.RegisterForCurrentYearPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, RegisterForCurrentYearView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisterForCurrentYearController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: RegisterForCurrentYearView,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  formProvider: RegisterForCurrentYearFormProvider,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService,
  pageNavigator: RegisterForCurrentYearPageNavigator
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[Boolean]                        = formProvider()
  def onPageLoad(mode: Mode): Action[AnyContent] = authorise { implicit request =>
    Ok(view(form, mode, s"${EclTaxYear.currentFinancialYear} to ${EclTaxYear.yearDue}"))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(view(formWithErrors, mode, s"${EclTaxYear.currentFinancialYear} to ${EclTaxYear.yearDue}"))
          ),
        answer =>
          (for {
            additionalInfo       <- registrationAdditionalInfoService.get(request.internalId).asResponseError
            updatedAdditionalInfo = additionalInfo.get.copy(registeringForCurrentYear = Some(answer))
            _                    <- registrationAdditionalInfoService.upsert(updatedAdditionalInfo).asResponseError
            updatedRegistration   = request.registration.copy(registrationType = Some(Initial))
            _                    <- registrationService.upsertRegistration(updatedRegistration).asResponseError
          } yield EclRegistrationModel(updatedRegistration, Some(updatedAdditionalInfo)))
            .convertToResult(mode = mode, pageNavigator = pageNavigator)
      )
  }
}
