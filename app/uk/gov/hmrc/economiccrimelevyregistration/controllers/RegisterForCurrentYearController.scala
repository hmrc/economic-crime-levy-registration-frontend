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

import cats.data.EitherT
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.{LiabilityDateAdditionalInfoCleanup, LiabilityDateRegistrationCleanup}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.RegisterForCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SessionError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, LiabilityYear, Mode, NormalMode, Registration, RegistrationAdditionalInfo, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.RegisterForCurrentYearPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, RegisterForCurrentYearView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegisterForCurrentYearController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  sessionService: SessionService,
  formProvider: RegisterForCurrentYearFormProvider,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService,
  pageNavigator: RegisterForCurrentYearPageNavigator,
  registrationDataCleanup: LiabilityDateRegistrationCleanup,
  additionalInfoDataCleanup: LiabilityDateAdditionalInfoCleanup,
  view: RegisterForCurrentYearView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    (for {
      urlToReturnTo <- mode match {
                         case NormalMode =>
                           sessionService
                             .getOptional(request.session, request.internalId, SessionKeys.UrlToReturnTo)
                             .asResponseError
                         case CheckMode  =>
                           EitherT[Future, SessionError, Option[String]] {
                             Future.successful(Right(None))
                           }.asResponseError
                       }
    } yield urlToReturnTo).fold(
      err => routeError(err),
      {
        case Some(_) => Redirect(routes.SavedResponsesController.onPageLoad)
        case None    =>
          request.additionalInfo match {
            case Some(value) =>
              Ok(
                view(
                  form.prepare(value.registeringForCurrentYear),
                  mode,
                  s"${EclTaxYear.currentStartYear().toString} to ${EclTaxYear.currentFinishYear().toString}",
                  EclTaxYear.currentFinancialYearStartDate,
                  EclTaxYear.currentFinancialYearFinishDate
                )
              )
            case None        =>
              Ok(
                view(
                  form,
                  mode,
                  s"${EclTaxYear.currentStartYear().toString} to ${EclTaxYear.currentFinishYear().toString}",
                  EclTaxYear.currentFinancialYearStartDate,
                  EclTaxYear.currentFinancialYearFinishDate
                )
              )
          }
      }
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                mode,
                s"${EclTaxYear.currentStartYear().toString} to ${EclTaxYear.currentFinishYear().toString}",
                EclTaxYear.currentFinancialYearStartDate,
                EclTaxYear.currentFinancialYearFinishDate
              )
            )
          ),
        answer =>
          (for {
            additionalInfo         <- registrationAdditionalInfoService.get(request.internalId).asResponseError
            liabilityYear           = if (answer) Some(EclTaxYear.currentStartYear()) else None
            updatedAdditionalInfo   = additionalInfo.get.copy(
                                        registeringForCurrentYear = Some(answer),
                                        liabilityYear = liabilityYear.map(value => LiabilityYear(value))
                                      )
            cleanedUpAdditionalInfo = additionalInfoCleanup(updatedAdditionalInfo)
            _                      <- registrationAdditionalInfoService.upsert(cleanedUpAdditionalInfo).asResponseError
            updatedRegistration     = request.registration.copy(registrationType = Some(Initial))
            cleanedUpRegistration   = registrationCleanup(updatedRegistration, cleanedUpAdditionalInfo)
            _                      <- registrationService.upsertRegistration(cleanedUpRegistration).asResponseError
          } yield EclRegistrationModel(cleanedUpRegistration, Some(cleanedUpAdditionalInfo)))
            .convertToResult(mode = mode, pageNavigator = pageNavigator)
      )
  }

  private def registrationCleanup(
    registration: Registration,
    additionalInfo: RegistrationAdditionalInfo
  ): Registration =
    if (additionalInfo.registeringForCurrentYear.contains(false)) {
      registrationDataCleanup.cleanup(registration)
    } else {
      registration
    }

  private def additionalInfoCleanup(additionalInfo: RegistrationAdditionalInfo): RegistrationAdditionalInfo =
    if (additionalInfo.registeringForCurrentYear.contains(false)) {
      additionalInfoDataCleanup.cleanup(additionalInfo)
    } else {
      additionalInfo
    }

}
