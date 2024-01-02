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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, ErrorHandler}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.FirstContactEmailFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.FirstContactEmailPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AnswersAreInvalidView
import uk.gov.hmrc.economiccrimelevyregistration.views.html.contacts.FirstContactEmailView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FirstContactEmailController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: FirstContactEmailFormProvider,
  pageNavigator: FirstContactEmailPageNavigator,
  view: FirstContactEmailView,
  answersAreInvalidView: AnswersAreInvalidView,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ContactsUtils
    with ErrorHandler {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    (for {
      firstContactName <- request.firstContactName.asTestResponseError
    } yield firstContactName).fold(
      _ => Ok(answersAreInvalidView()),
      name =>
        Ok(
          view(
            form.prepare(request.registration.contacts.firstContactDetails.emailAddress),
            name,
            mode,
            request.registration.registrationType,
            request.eclRegistrationReference
          )
        )
    )
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors =>
          (for {
            firstContactName <- request.firstContactName.asTestResponseError
          } yield firstContactName)
            .fold(
              _ => Future.successful(Ok(answersAreInvalidView())),
              name =>
                Future.successful(
                  BadRequest(
                    view(
                      formWithErrors,
                      name,
                      mode,
                      request.registration.registrationType,
                      request.eclRegistrationReference
                    )
                  )
                )
            ),
        email => {
          val updatedContacts: Contacts = request.registration.contacts
            .copy(firstContactDetails =
              request.registration.contacts.firstContactDetails.copy(emailAddress = Some(email))
            )

          (for {
            _                    <-
              sessionService
                .upsert(SessionData(request.internalId, Map(SessionKeys.FirstContactEmailAddress -> email)))
                .asResponseError
            updatedRegistration   = request.registration.copy(contacts = updatedContacts)
            upsertedRegistration <-
              eclRegistrationService
                .upsertRegistration(updatedRegistration)
                .asResponseError
          } yield upsertedRegistration).convertToResult(mode, pageNavigator)
        }
      )
  }
}
