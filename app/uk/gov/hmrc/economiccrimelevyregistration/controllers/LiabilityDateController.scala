/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityDateFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, LiabilityYear, Mode, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.LiabilityDatePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.RegistrationAdditionalInfoService
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, LiabilityDateView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class LiabilityDateController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  storeUrl: StoreUrlAction,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  formProvider: LiabilityDateFormProvider,
  pageNavigator: LiabilityDatePageNavigator,
  view: LiabilityDateView
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {
  val form: Form[LocalDate]                      = formProvider()
  def onPageLoad(mode: Mode): Action[AnyContent] = (authorise andThen getRegistrationData andThen storeUrl) {
    implicit request =>
      request.additionalInfo match {
        case Some(value) => Ok(view(mode, form.prepare(value.liabilityStartDate)))
        case None        => Ok(view(mode, form))
      }
  }

  def onSubmit(mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(mode, formWithErrors))),
          liabilityDate => {
            val year           = EclTaxYear.taxYearFor(liabilityDate)
            val registration   = request.registration
            val additionalInfo = request.additionalInfo.get.copy(
              liabilityStartDate = Some(liabilityDate),
              liabilityYear = Some(LiabilityYear(year.startYear))
            )

            (for {
              _ <- registrationAdditionalInfoService.upsert(additionalInfo).asResponseError
            } yield EclRegistrationModel(registration, Some(additionalInfo)))
              .convertToResult(
                mode = mode,
                pageNavigator = pageNavigator,
                session = Map(SessionKeys.liabilityYear -> liabilityDate.getYear.toString)
              )
          }
        )
    }
}
