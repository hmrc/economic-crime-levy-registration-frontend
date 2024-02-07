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

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithoutEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RegistrationSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  view: RegistrationSubmittedView,
  outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView,
  sessionService: SessionService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      _                    <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _                    <- registrationService.deleteRegistration(request.internalId).asResponseError
      liabilityYear        <-
        sessionService.get(request.session, request.internalId, SessionKeys.LiabilityYear).asResponseError
      amlRegulatedActivity <-
        sessionService.get(request.session, request.internalId, SessionKeys.AmlRegulatedActivity).asResponseError
      firstContactEmail    <-
        sessionService.get(request.session, request.internalId, SessionKeys.FirstContactEmailAddress).asResponseError
      secondContactEmail   <- sessionService
                                .getOptional(request.session, request.internalId, SessionKeys.SecondContactEmailAddress)
                                .asResponseError
      eclReference         <-
        sessionService.getOptional(request.session, request.internalId, SessionKeys.EclReference).asResponseError
    } yield (liabilityYear, amlRegulatedActivity, firstContactEmail, secondContactEmail, eclReference)).fold(
      _ => Redirect(routes.NotableErrorController.answersAreInvalid()),
      data => {
        val liabilityYear        = Some(LiabilityYear(data._1.toInt))
        val amlRegulatedActivity = data._2
        val firstContactEmail    = data._3
        val secondContactEmail   = data._4
        val eclReference         = data._5
        (eclReference, request.eclRegistrationReference) match {
          case (Some(eclReference), _) =>
            Ok(view(eclReference, firstContactEmail, secondContactEmail, liabilityYear, Some(amlRegulatedActivity)))
          case (_, Some(eclReference)) =>
            Ok(outOfSessionRegistrationSubmittedView(eclReference, liabilityYear, Some(amlRegulatedActivity)))
          case _                       =>
            Redirect(routes.NotableErrorController.answersAreInvalid())
        }
      }
    )
  }
}
