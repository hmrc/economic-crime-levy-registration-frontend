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
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedAction, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.contacts.SecondContactRoleFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{Contacts, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.contacts.SecondContactRolePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.SecondContactRoleView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SecondContactRoleController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationConnector: EclRegistrationConnector,
  formProvider: SecondContactRoleFormProvider,
  pageNavigator: SecondContactRolePageNavigator,
  view: SecondContactRoleView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(view(form.prepare(request.registration.contacts.secondContactDetails.role), secondContactName(request)))
  }

  def onSubmit: Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, secondContactName(request)))),
        role => {
          val updatedContacts: Contacts = request.registration.contacts
            .copy(secondContactDetails = request.registration.contacts.secondContactDetails.copy(role = Some(role)))

          eclRegistrationConnector
            .upsertRegistration(request.registration.copy(contacts = updatedContacts))
            .map { updatedRegistration =>
              Redirect(pageNavigator.nextPage(NormalMode, updatedRegistration))
            }
        }
      )
  }

}
