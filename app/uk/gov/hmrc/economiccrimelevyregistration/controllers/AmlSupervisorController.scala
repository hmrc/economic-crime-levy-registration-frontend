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
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.forms.{AmendAmlSupervisorFormProvider, AmlSupervisorFormProvider}
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{NotLiableReason, RegistrationNotLiableAuditEvent}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{AmlSupervisorPageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlSupervisorView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlSupervisorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: AmlSupervisorFormProvider,
  amendFormProvider: AmendAmlSupervisorFormProvider,
  appConfig: AppConfig,
  pageNavigator: AmlSupervisorPageNavigator,
  auditConnector: AuditConnector,
  view: AmlSupervisorView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  private val form: Form[AmlSupervisor] = formProvider(appConfig)

  private val amendForm: Form[AmlSupervisor] = amendFormProvider(appConfig)

  def onPageLoad(
    mode: Mode,
    registrationType: RegistrationType = Initial,
    fromLiableBeforeCurrentYearPage: Boolean = false
  ): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      registrationType match {
        case Initial   =>
          Ok(
            view(
              form.prepare(request.registration.amlSupervisor),
              mode,
              Some(registrationType),
              fromLiableBeforeCurrentYearPage,
              request.eclRegistrationReference
            )
          )
        case Amendment =>
          Ok(
            view(
              amendForm.prepare(request.registration.amlSupervisor),
              mode,
              Some(registrationType),
              fromLiableBeforeCurrentYearPage,
              request.eclRegistrationReference
            )
          )
      }
    }

  def onSubmit(
    mode: Mode,
    registrationType: RegistrationType = Initial,
    fromLiableBeforeCurrentYearPage: Boolean
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
                  fromLiableBeforeCurrentYearPage,
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

            updatedRegistration.amlSupervisor match {
              case Some(amlSupervisor) =>
                amlSupervisor.supervisorType match {
                  case t @ (GamblingCommission | FinancialConductAuthority) =>
                    registerWithGcOrFca(t, updatedRegistration)
                  case _                                                    =>
                }
            }

            (for {
              upsertedRegistration <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
            } yield NavigationData(upsertedRegistration, "", false)).convertToResult(mode, pageNavigator)
          }
        )
    }

  private def registerWithGcOrFca(amlSupervisorType: AmlSupervisorType, registration: Registration)(implicit
    hc: HeaderCarrier
  ): Future[Unit] =
    amlSupervisorType match {
      case GamblingCommission        =>
        sendNotLiableAuditEvent(registration.internalId, NotLiableReason.SupervisedByGamblingCommission)
      case FinancialConductAuthority =>
        sendNotLiableAuditEvent(registration.internalId, NotLiableReason.SupervisedByFinancialConductAuthority)
    }

  private def sendNotLiableAuditEvent(internalId: String, notLiableReason: NotLiableReason)(implicit
    hc: HeaderCarrier
  ): Future[Unit] = {
    auditConnector
      .sendExtendedEvent(RegistrationNotLiableAuditEvent(internalId, notLiableReason).extendedDataEvent)

    Future.unit
  }
}
