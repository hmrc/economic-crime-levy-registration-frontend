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

import com.google.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.DoYouHaveCrnFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.DoYouHaveCrnPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.DoYouHaveCrnView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DoYouHaveCrnController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  formProvider: DoYouHaveCrnFormProvider,
  eclRegistrationConnector: EclRegistrationConnector,
  pageNavigator: DoYouHaveCrnPageNavigator,
  view: DoYouHaveCrnView
)(implicit
  ec: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      Ok(view(form.prepare(request.registration.otherEntityJourneyData.isUkCrnPresent), mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          answer => {
            val companyRegistrationNumber = answer match {
              case true  => request.registration.otherEntityJourneyData.companyRegistrationNumber
              case false => None
            }
            val otherEntityJourneyData    =
              request.registration.otherEntityJourneyData.copy(
                isUkCrnPresent = Some(answer),
                companyRegistrationNumber = companyRegistrationNumber
              )
            eclRegistrationConnector
              .upsertRegistration(
                request.registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
              )
              .map(updatedRegistration => Redirect(pageNavigator.nextPage(mode, updatedRegistration)))
          }
        )
    }
}
