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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction, OtherEntityTypeAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityBeforeCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, NormalMode, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.LiabilityBeforeCurrentYearPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationAdditionalInfoService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{DoYouHaveCtUtrView, LiabilityBeforeCurrentYearView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityBeforeCurrentYearController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  formProvider: LiabilityBeforeCurrentYearFormProvider,
  service: RegistrationAdditionalInfoService,
  pageNavigator: LiabilityBeforeCurrentYearPageNavigator,
  view: LiabilityBeforeCurrentYearView
)(implicit
  ec: ExecutionContext
) extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      Ok(view(form.prepare(None), NormalMode))
    }

  def onSubmit(): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      val mode = NormalMode
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          liableBeforeCurrentYear => {
            val info = RegistrationAdditionalInfo(
              request.registration.internalId,
              getLiabilityYear(liableBeforeCurrentYear),
              request.eclRegistrationReference
            )
            service
              .createOrUpdate(info)
              .map(_ => Redirect(pageNavigator.nextPage(liableBeforeCurrentYear, request.registration)))
          }
        )
    }

  def getLiabilityYear(liableBeforeCurrentYear: Boolean) =
    Some(if (liableBeforeCurrentYear) 2022 else 2023)
}
