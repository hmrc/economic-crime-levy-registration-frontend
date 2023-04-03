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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.forms.PrivateBetaAccessFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.PrivateBetaAccessView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PrivateBetaAccessController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  eclRegistrationService: EclRegistrationService,
  eclRegistrationConnector: EclRegistrationConnector,
  formProvider: PrivateBetaAccessFormProvider,
  view: PrivateBetaAccessView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(continueUrl: String): Action[AnyContent] = authorise { implicit request =>
    Ok(view(form, continueUrl))
  }

  def onSubmit(continueUrl: String): Action[AnyContent] = authorise.async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, continueUrl))),
        accessCode =>
          for {
            registration <- eclRegistrationService.getOrCreateRegistration(request.internalId)
            _            <- eclRegistrationConnector.upsertRegistration(
                              registration.copy(privateBetaAccessCode = Some(accessCode))
                            )
          } yield Redirect(continueUrl)
      )
  }

}