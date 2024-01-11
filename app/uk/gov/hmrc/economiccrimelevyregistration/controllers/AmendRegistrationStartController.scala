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
import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendRegistrationStartView, AnswersAreInvalidView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AmendRegistrationStartController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  authorise: AuthorisedActionWithEnrolmentCheck,
  view: AmendRegistrationStartView,
  registrationService: EclRegistrationService,
  answersAreInvalidView: AnswersAreInvalidView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  def onPageLoad(eclReference: String): Action[AnyContent] = authorise.async { implicit request =>
    (for {
      registration               <- registrationService.getOrCreateRegistration(request.internalId).asResponseError
      _                          <-
        registrationService.upsertRegistration(registration.copy(registrationType = Some(Amendment))).asResponseError
      createOrUpdateRegistration <-
        registrationAdditionalInfoService.createOrUpdate(request.internalId, Some(eclReference)).asResponseError
    } yield createOrUpdateRegistration).fold(
      _ => Ok(answersAreInvalidView()),
      _ => Ok(view(eclReference))
    )
  }
}
