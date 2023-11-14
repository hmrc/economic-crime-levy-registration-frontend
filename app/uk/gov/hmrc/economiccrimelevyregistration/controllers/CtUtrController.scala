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
import play.api.data.Form
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.CtUtrFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models.Mode
import uk.gov.hmrc.economiccrimelevyregistration.navigation.CtUtrPageNavigator
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CtUtrView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CtUtrController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationConnector: EclRegistrationConnector,
  formProvider: CtUtrFormProvider,
  pageNavigator: CtUtrPageNavigator,
  view: CtUtrView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      Ok(view(form.prepare(request.registration.otherEntityJourneyData.ctUtr), mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          ctutr => {
            val otherEntity = request.registration.otherEntityJourneyData.copy(
              ctUtr = Some(ctutr),
              saUtr = None
            );
            eclRegistrationConnector
              .upsertRegistration(
                request.registration.copy(optOtherEntityJourneyData = Some(otherEntity))
              )
              .map(updatedRegistration => Redirect(pageNavigator.nextPage(mode, updatedRegistration)))
          }
        )
    }
}
