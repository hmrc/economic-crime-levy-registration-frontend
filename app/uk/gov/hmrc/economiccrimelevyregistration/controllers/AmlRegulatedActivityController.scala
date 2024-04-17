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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, RegistrationDataAction, StoreUrlAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmlRegulatedActivityFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits._
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, Mode, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmlRegulatedActivityPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, LocalDateService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmlRegulatedActivityView, ErrorTemplate}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmlRegulatedActivityController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  storeUrl: StoreUrlAction,
  eclRegistrationService: EclRegistrationService,
  additionalInfoService: RegistrationAdditionalInfoService,
  formProvider: AmlRegulatedActivityFormProvider,
  pageNavigator: AmlRegulatedActivityPageNavigator,
  view: AmlRegulatedActivityView,
  localDateService: LocalDateService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with ErrorHandler
    with BaseController
    with I18nSupport {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData andThen storeUrl) {
    implicit request =>
      val eclTaxYear: EclTaxYear = EclTaxYear.fromCurrentDate(localDateService.now())
      Ok(view(form.prepare(request.registration.carriedOutAmlRegulatedActivityInCurrentFy), mode, eclTaxYear))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val eclTaxYear: EclTaxYear = EclTaxYear.fromCurrentDate(localDateService.now())
    form
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, eclTaxYear))),
        amlRegulatedActivity => {
          val answerChanged =
            !request.registration.carriedOutAmlRegulatedActivityInCurrentFy.contains(amlRegulatedActivity)

          val updatedRegistration = request.registration.copy(
            carriedOutAmlRegulatedActivityInCurrentFy = Some(amlRegulatedActivity),
            amlSupervisor = clearValueIfChange(answerChanged, request.registration.amlSupervisor)
          )

          val info        = request.additionalInfo.getOrElse(RegistrationAdditionalInfo(request.registration.internalId))
          val updatedInfo = info.copy(
            liableForPreviousYears = clearValueIfChange(answerChanged, info.liableForPreviousYears)
          )

          (for {
            _ <- eclRegistrationService.upsertRegistration(updatedRegistration).asResponseError
            _ <- additionalInfoService.upsert(updatedInfo).asResponseError
          } yield EclRegistrationModel(registration = updatedRegistration, hasRegistrationChanged = answerChanged))
            .convertToResult(mode, pageNavigator)
        }
      )
  }

  private def clearValueIfChange[T](answerChanged: Boolean, existingValue: Option[T]): Option[T] =
    answerChanged match {
      case true  => None
      case false => existingValue
    }
}
