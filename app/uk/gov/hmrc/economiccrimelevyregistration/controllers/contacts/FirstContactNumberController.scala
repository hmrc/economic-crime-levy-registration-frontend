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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{BaseController, ErrorHandler}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.FirstContactNumberFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, Mode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.FirstContactNumberPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate
import uk.gov.hmrc.economiccrimelevyregistration.views.html.contacts.FirstContactNumberView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FirstContactNumberController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationService: EclRegistrationService,
  formProvider: FirstContactNumberFormProvider,
  pageNavigator: FirstContactNumberPageNavigator,
  view: FirstContactNumberView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    (for {
      firstContactName <- request.firstContactNameOrError
    } yield firstContactName).fold(
      _ =>
        Redirect(
          uk.gov.hmrc.economiccrimelevyregistration.controllers.routes.NotableErrorController.answersAreInvalid()
        ),
      name =>
        Ok(
          view(
            form.prepare(request.registration.contacts.firstContactDetails.telephoneNumber),
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
            firstContactName <- request.firstContactNameOrError
          } yield firstContactName)
            .fold(
              _ =>
                Future.successful(
                  Redirect(
                    uk.gov.hmrc.economiccrimelevyregistration.controllers.routes.NotableErrorController
                      .answersAreInvalid()
                  )
                ),
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
        telephoneNumber => {
          val updatedContacts: Contacts = request.registration.contacts
            .copy(firstContactDetails =
              request.registration.contacts.firstContactDetails.copy(telephoneNumber = Some(telephoneNumber))
            )

          val updatedRegistration = request.registration.copy(contacts = updatedContacts)

          (for {
            _ <-
              eclRegistrationService
                .upsertRegistration(updatedRegistration)
                .asResponseError
          } yield updatedRegistration).convertToResult(mode, pageNavigator)

        }
      )
  }

}
