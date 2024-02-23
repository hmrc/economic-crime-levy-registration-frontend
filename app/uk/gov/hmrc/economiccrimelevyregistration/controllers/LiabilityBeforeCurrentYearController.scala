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
import com.google.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityBeforeCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.audit.{NotLiableReason, RegistrationNotLiableAuditEvent}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.AuditError
import uk.gov.hmrc.economiccrimelevyregistration.services.{AuditService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, LiabilityBeforeCurrentYearView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityBeforeCurrentYearController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  formProvider: LiabilityBeforeCurrentYearFormProvider,
  additionalInfoService: RegistrationAdditionalInfoService,
  view: LiabilityBeforeCurrentYearView,
  auditService: AuditService
)(implicit
  ec: ExecutionContext,
  errorTemplate: ErrorTemplate
) extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      Ok(view(form.prepare(isLiableForPreviousFY(request.additionalInfo)), mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      val registration = request.registration
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          liableBeforeCurrentYear => {
            val liabilityYear = getFirstLiabilityYear(
              registration.carriedOutAmlRegulatedActivityInCurrentFy,
              liableBeforeCurrentYear
            )

            val info = RegistrationAdditionalInfo(
              registration.internalId,
              liabilityYear,
              request.additionalInfo.flatMap(info => info.liabilityStartDate),
              request.additionalInfo.flatMap(info => info.registeringForCurrentYear),
              Some(liableBeforeCurrentYear),
              request.eclRegistrationReference
            )

            additionalInfoService
              .upsert(info)
              .asResponseError
              .fold(
                err => routeError(err),
                _ => Redirect(navigateByMode(mode, registration, liableBeforeCurrentYear))
              )
          }
        )
    }

  private def navigateByMode(mode: Mode, registration: Registration, liableBeforeCurrentYear: Boolean)(implicit
    hc: HeaderCarrier
  ): Call =
    mode match {
      case NormalMode => navigateInNormalMode(liableBeforeCurrentYear, registration, mode)
      case CheckMode  => routes.CheckYourAnswersController.onPageLoad()
    }

  private def navigateInNormalMode(liableBeforeCurrentYear: Boolean, registration: Registration, mode: Mode)(implicit
    hc: HeaderCarrier
  ): Call =
    (liableBeforeCurrentYear, registration.revenueMeetsThreshold, registration.businessSector) match {
      case (false, Some(false), None) =>
        sendNotLiableAuditEvent(registration)
        routes.NotLiableController.youDoNotNeedToRegister()
      case (false, Some(true), None)  => routes.EntityTypeController.onPageLoad(mode)
      case (false, None, None)        =>
        sendNotLiableAuditEvent(registration)
        routes.NotLiableController.youDoNotNeedToRegister()
      case (true, _, None)            =>
        routes.LiabilityDateController.onPageLoad(mode)
      case (_, _, Some(_))            => routes.CheckYourAnswersController.onPageLoad()
    }

  private def sendNotLiableAuditEvent(registration: Registration)(implicit hc: HeaderCarrier) =
    registration.revenueMeetsThreshold match {
      case Some(true)  =>
        val event = RegistrationNotLiableAuditEvent(
          registration.internalId,
          NotLiableReason.DidNotCarryOutAmlRegulatedActivity
        ).extendedDataEvent
        auditService.sendEvent(event)
      case Some(false) =>
        val event = RegistrationNotLiableAuditEvent(
          registration.internalId,
          NotLiableReason.RevenueDoesNotMeetThreshold.apply(
            registration.relevantAp12Months,
            registration.relevantApLength,
            registration.relevantApRevenue.get.toLong,
            registration.revenueMeetsThreshold.get
          )
        ).extendedDataEvent
        auditService.sendEvent(event)

      case None => EitherT[Future, AuditError, Unit](Future.successful(Right(())))
    }

  private def getFirstLiabilityYear(
    liableForCurrentFY: Option[Boolean],
    liableForPreviousFY: Boolean
  ): Option[LiabilityYear] =
    (liableForCurrentFY, liableForPreviousFY) match {
      case (Some(_), true)     => Some(LiabilityYear(TaxYear.current.previous.startYear))
      case (Some(true), false) => Some(LiabilityYear(TaxYear.current.currentYear))
      case _                   => None
    }

  private def isLiableForPreviousFY(info: Option[RegistrationAdditionalInfo]) =
    info.get.liabilityYear match {
      case Some(value) => Some(value.isNotCurrentFY)
      case _           => None
    }
}
