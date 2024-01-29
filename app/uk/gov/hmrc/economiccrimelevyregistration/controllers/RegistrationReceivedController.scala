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
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, RegistrationReceivedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RegistrationReceivedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  view: RegistrationReceivedView,
  sessionService: SessionService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      _                                        <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _                                        <- registrationService.deleteRegistration(request.internalId).asResponseError
      firstContactEmailAddress: Option[String]  = request.session.get(SessionKeys.FirstContactEmailAddress)
      secondContactEmailAddress: Option[String] = request.session.get(SessionKeys.SecondContactEmailAddress)
      amlRegulatedActivity: Option[String]      = request.session.get(SessionKeys.AmlRegulatedActivity)
      liabilityYear: Option[String]             = request.session.get(SessionKeys.LiabilityYear)
      transformedLiabilityYear                  = liabilityYear.map(value => LiabilityYear(value.toInt))

      registrationReceivedView = view(
                                   firstContactEmailAddress.getOrElse(""),
                                   secondContactEmailAddress,
                                   transformedLiabilityYear,
                                   amlRegulatedActivity
                                 )
    } yield registrationReceivedView).fold(
      error => routeError(error),
      registrationReceivedView => Ok(registrationReceivedView)
    )
  }
}
