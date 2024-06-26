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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.{LiabilityDateAdditionalInfoCleanup, LiabilityDateRegistrationCleanup}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.RegisterForCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SessionError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, LiabilityYear, Mode, NormalMode, Registration, RegistrationAdditionalInfo, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.RegisterForCurrentYearPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, LocalDateService, RegistrationAdditionalInfoService, SessionService}
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
  view: RegisterForCurrentYearView,
  localDateService: LocalDateService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[Boolean] = formProvider(localDateService)

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    (for {
      urlToReturnTo <- mode match {
                         case NormalMode =>
                           sessionService
                             .getOptional(request.session, request.internalId, SessionKeys.urlToReturnTo)
                             .asResponseError
                         case CheckMode  =>
                           EitherT[Future, SessionError, Option[String]] {
                             Future.successful(Right(None))
                           }.asResponseError
                       }
    } yield urlToReturnTo).fold(
      err => routeError(err),
      {
        case Some(_) =>
          Redirect(routes.SavedResponsesController.onPageLoad)
            .withSession(
              request.session ++ Seq(
                SessionKeys.registrationType -> Json.stringify(Json.toJson(request.registration.registrationType))
              )
            )
        case None    =>
          request.additionalInfo match {
            case Some(value) =>
              val eclTaxYear = EclTaxYear.fromCurrentDate(localDateService.now())
              Ok(
                view(
                  form.prepare(value.registeringForCurrentYear),
                  mode,
                  eclTaxYear.startYear.toString,
                  eclTaxYear.finishYear.toString,
                  eclTaxYear.startDate,
                  eclTaxYear.finishDate
                )
              ).withSession(
                request.session ++ Seq(
                  SessionKeys.registrationType -> Json.stringify(Json.toJson(request.registration.registrationType))
                )
              )
            case None        =>
              val eclTaxYear = EclTaxYear.fromCurrentDate(localDateService.now())
              Ok(
                view(
                  form,
                  mode,
                  eclTaxYear.startYear.toString,
                  eclTaxYear.finishYear.toString,
                  eclTaxYear.startDate,
                  eclTaxYear.finishDate
                )
              ).withSession(
                request.session ++ Seq(
                  SessionKeys.registrationType -> Json.stringify(Json.toJson(request.registration.registrationType))
                )
              )
          }
      }
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val eclTaxYear = EclTaxYear.fromCurrentDate(localDateService.now())
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(
                formWithErrors,
                mode,
                eclTaxYear.startYear.toString,
                eclTaxYear.finishYear.toString,
                eclTaxYear.startDate,
                eclTaxYear.finishDate
              )
            )
          ),
        answer =>
          (for {
            additionalInfo         <- registrationAdditionalInfoService.get(request.internalId).asResponseError
            liabilityYear           = if (answer) Some(eclTaxYear.startYear) else None
            currentAnswer           = additionalInfo.get.liabilityYear.isDefined
            updatedAdditionalInfo   = additionalInfo.get.copy(
                                        registeringForCurrentYear = Some(answer),
                                        liabilityYear = liabilityYear.map(value => LiabilityYear(value))
                                      )
            cleanedUpAdditionalInfo = additionalInfoCleanup(updatedAdditionalInfo)
            _                      <- registrationAdditionalInfoService.upsert(cleanedUpAdditionalInfo).asResponseError
            updatedRegistration     = request.registration.copy(registrationType = Some(Initial))
            cleanedUpRegistration   = registrationCleanup(updatedRegistration, cleanedUpAdditionalInfo)
            _                      <- registrationService.upsertRegistration(cleanedUpRegistration).asResponseError
          } yield EclRegistrationModel(
            registration = cleanedUpRegistration,
            registrationAdditionalInfo = Some(cleanedUpAdditionalInfo),
            hasAdditionalInfoChanged = (answer != currentAnswer)
          )).convertToResult(mode = mode, pageNavigator = pageNavigator)
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
