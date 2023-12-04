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
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmlRegulatedActivityFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, SessionData, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmlRegulatedActivityPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlRegulatedActivityView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlRegulatedActivityController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationConnector: EclRegistrationConnector,
  sessionService: SessionService,
  formProvider: AmlRegulatedActivityFormProvider,
  pageNavigator: AmlRegulatedActivityPageNavigator,
  view: AmlRegulatedActivityView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData) { implicit request =>
    Ok(view(form.prepare(request.registration.carriedOutAmlRegulatedActivityInCurrentFy), mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode))),
        amlRegulatedActivity =>
          eclRegistrationConnector
            .upsertRegistration(
              request.registration.copy(
                carriedOutAmlRegulatedActivityInCurrentFy = Some(amlRegulatedActivity),
                registrationType = Some(Initial)
              )
            )
            .flatMap { updatedRegistration =>
              if (amlRegulatedActivity) {
                val amlActivitySessionData = Map(
                  SessionKeys.AmlRegulatedActivity -> amlRegulatedActivity.toString
                )

                sessionService
                  .upsert(
                    SessionData(
                      request.internalId,
                      amlActivitySessionData
                    )
                  )

                pageNavigator
                  .nextPage(mode, updatedRegistration)
                  .map(
                    Redirect(_).withSession(
                      request.session ++
                        amlActivitySessionData
                    )
                  )
              } else {
                pageNavigator
                  .nextPage(mode, updatedRegistration)
                  .map(Redirect)
              }
            }
      )
  }
}
