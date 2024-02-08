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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.contacts

import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.AddAnotherContactDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, _}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.AddAnotherContactFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.AddAnotherContactPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate
import uk.gov.hmrc.economiccrimelevyregistration.views.html.contacts.AddAnotherContactView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AddAnotherContactController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: AddAnotherContactFormProvider,
  pageNavigator: AddAnotherContactPageNavigator,
  dataCleanup: AddAnotherContactDataCleanup,
  view: AddAnotherContactView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(
      view(
        form.prepare(request.registration.contacts.secondContact),
        mode,
        request.registration.registrationType,
        request.eclRegistrationReference
      )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          Future.successful(
            BadRequest(
              view(formWithErrors, mode, request.registration.registrationType, request.eclRegistrationReference)
            )
          ),
        secondContactBoolean => {
          val secondContact: Contacts = request.registration.contacts.copy(secondContact = Some(secondContactBoolean))
          val updatedRegistration     = dataCleanup.cleanup(request.registration).copy(contacts = secondContact)

          (for {
            _ <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
          } yield updatedRegistration).convertToResult(mode, pageNavigator)
        }
      )
  }
}
