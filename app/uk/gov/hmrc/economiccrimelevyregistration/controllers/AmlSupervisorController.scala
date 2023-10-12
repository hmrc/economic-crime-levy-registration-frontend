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
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.{AmendAmlSupervisorFormProvider, AmlSupervisorFormProvider}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmlSupervisorPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlSupervisorView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlSupervisorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  eclRegistrationConnector: EclRegistrationConnector,
  formProvider: AmlSupervisorFormProvider,
  amendFormProvider: AmendAmlSupervisorFormProvider,
  appConfig: AppConfig,
  pageNavigator: AmlSupervisorPageNavigator,
  view: AmlSupervisorView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  val form: Form[AmlSupervisor] = formProvider(appConfig)

  val amendForm: Form[AmlSupervisor] = amendFormProvider(appConfig)

  def onPageLoad(
    mode: Mode,
    registrationType: RegistrationType = Initial,
    fromLiableBeforeCurrentYearPage: Boolean = false
  ): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      registrationType match {
        case Initial   =>
          Ok(
            view(
              form.prepare(request.registration.amlSupervisor),
              mode,
              registrationType,
              fromLiableBeforeCurrentYearPage
            )
          )
        case Amendment =>
          Ok(
            view(
              amendForm.prepare(request.registration.amlSupervisor),
              mode,
              registrationType,
              fromLiableBeforeCurrentYearPage
            )
          )
      }
    }

  def onSubmit(
    mode: Mode,
    registrationType: RegistrationType = Initial,
    fromLiableBeforeCurrentYearPage: Boolean
  ): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors =>
            Future
              .successful(BadRequest(view(formWithErrors, mode, registrationType, fromLiableBeforeCurrentYearPage))),
          amlSupervisor =>
            eclRegistrationConnector
              .upsertRegistration(
                request.registration
                  .copy(amlSupervisor = Some(amlSupervisor), registrationType = Some(registrationType))
              )
              .flatMap { updatedRegistration =>
                pageNavigator.nextPage(mode, updatedRegistration, fromLiableBeforeCurrentYearPage).map(Redirect)
              }
        )
    }
}
