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
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.OtherEntityTypeDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction, OtherEntityTypeAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.OtherEntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.OtherEntityTypePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.OtherEntityTypeView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OtherEntityTypeController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationConnector: EclRegistrationConnector,
  formProvider: OtherEntityTypeFormProvider,
  pageNavigator: OtherEntityTypePageNavigator,
  dataCleanup: OtherEntityTypeDataCleanup,
  checkIfOtherEntityTypeEnabled: OtherEntityTypeAction,
  view: OtherEntityTypeView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[OtherEntityType] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] =
    (checkIfOtherEntityTypeEnabled andThen authorise andThen getRegistrationData) { implicit request =>
      Ok(view(form.prepare(request.registration.otherEntityJourneyData.entityType), mode))
    }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (checkIfOtherEntityTypeEnabled andThen authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
          entityType => {
            val otherEntityJourneyData = request.registration.otherEntityJourneyData.copy(
              entityType = Some(entityType)
            )

            request.registration.otherEntityJourneyData.entityType match {
              case None                     =>
                val updatedRegistration =
                  request.registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
                eclRegistrationConnector
                  .upsertRegistration(updatedRegistration)
                  .map { updatedRegistration =>
                    Redirect(pageNavigator.nextPage(mode, updatedRegistration))
                  }
              case Some(previousEntityType) =>
                if (previousEntityType == entityType) {
                  Future.successful(Redirect(routes.OtherEntityCheckYourAnswersController.onPageLoad()))
                } else {
                  val updatedRegistration =
                    request.registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))
                  eclRegistrationConnector
                    .upsertRegistration(
                      dataCleanup.cleanup(updatedRegistration)
                    )
                    .map { updatedRegistration =>
                      Redirect(pageNavigator.nextPage(mode, updatedRegistration))
                    }
                }
            }

          }
        )
    }
}
