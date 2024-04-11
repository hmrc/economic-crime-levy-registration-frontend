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

import cats.data.EitherT
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.forms.{AmendAmlSupervisorFormProvider, AmlSupervisorFormProvider}
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{NotLiableReason, RegistrationNotLiableAuditEvent}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{AuditError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmlSupervisorPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmlSupervisorView, ErrorTemplate}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlSupervisorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  storeUrl: StoreUrlAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: AmlSupervisorFormProvider,
  amendFormProvider: AmendAmlSupervisorFormProvider,
  appConfig: AppConfig,
  pageNavigator: AmlSupervisorPageNavigator,
  view: AmlSupervisorView,
  auditService: AuditService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  private val form: Form[AmlSupervisor] = formProvider(appConfig)

  private val amendForm: Form[AmlSupervisor] = amendFormProvider(appConfig)

  def onPageLoad(
    mode: Mode,
    registrationType: RegistrationType = Initial
  ): Action[AnyContent] =
    (authorise andThen getRegistrationData andThen storeUrl) { implicit request =>
      registrationType match {
        case Initial   =>
          Ok(
            view(
              form.prepare(request.registration.amlSupervisor),
              mode,
              Some(registrationType),
              request.registration.carriedOutAmlRegulatedActivityInCurrentFy,
              request.eclRegistrationReference
            )
          )
        case Amendment =>
          Ok(
            view(
              amendForm.prepare(request.registration.amlSupervisor),
              mode,
              Some(registrationType),
              request.registration.carriedOutAmlRegulatedActivityInCurrentFy,
              request.eclRegistrationReference
            )
          )
        case _         => routeError(error = ResponseError.badRequestError("Invalid registrationType"))
      }
    }

  def onSubmit(
    mode: Mode,
    registrationType: RegistrationType = Initial
  ): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future.successful(
              BadRequest(
                view(
                  formWithErrors,
                  mode,
                  Some(registrationType),
                  request.registration.carriedOutAmlRegulatedActivityInCurrentFy,
                  request.eclRegistrationReference
                )
              )
            ),
          amlSupervisor => {
            val updatedRegistration =
              request.registration.copy(
                amlSupervisor = Some(amlSupervisor),
                registrationType = Some(registrationType)
              )

            (for {
              _ <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
              _  = registerWithGcOrFca(updatedRegistration).asResponseError
            } yield EclRegistrationModel(updatedRegistration)).convertToResult(mode, pageNavigator)
          }
        )
    }

  private def registerWithGcOrFca(registration: Registration)(implicit
    hc: HeaderCarrier
  ): EitherT[Future, AuditError, Unit] =
    registration.amlSupervisor.map(_.supervisorType) match {
      case Some(GamblingCommission)        =>
        val event = RegistrationNotLiableAuditEvent(
          registration.internalId,
          NotLiableReason.SupervisedByGamblingCommission
        ).extendedDataEvent
        auditService.sendEvent(event)
      case Some(FinancialConductAuthority) =>
        val event = RegistrationNotLiableAuditEvent(
          registration.internalId,
          NotLiableReason.SupervisedByFinancialConductAuthority
        ).extendedDataEvent
        auditService.sendEvent(event)
      case _                               =>
        EitherT[Future, AuditError, Unit](Future.successful(Right(())))
    }
}
