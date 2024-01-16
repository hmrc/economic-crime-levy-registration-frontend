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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.forms.FormImplicits.FormOps
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityBeforeCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{LiabilityBeforeCurrentYearPageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.{RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.LiabilityBeforeCurrentYearView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear

import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class LiabilityBeforeCurrentYearController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  formProvider: LiabilityBeforeCurrentYearFormProvider,
  service: RegistrationAdditionalInfoService,
  sessionService: SessionService,
  pageNavigator: LiabilityBeforeCurrentYearPageNavigator,
  view: LiabilityBeforeCurrentYearView
)(implicit
  ec: ExecutionContext
) extends FrontendBaseController
    with I18nSupport
    with ErrorHandler
    with BaseController {

  val form: Form[Boolean] = formProvider()

  def onPageLoad(fromRevenuePage: Boolean, mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData) { implicit request =>
      Ok(view(form.prepare(isLiableForPreviousFY(request.additionalInfo)), mode, fromRevenuePage))
    }

  def onSubmit(fromRevenuePage: Boolean, mode: Mode): Action[AnyContent] =
    (authorise andThen getRegistrationData).async { implicit request =>
      form
        .bindFromRequest()
        .fold(
          formWithErrors => Future.successful(BadRequest(view(formWithErrors, mode, fromRevenuePage))),
          liableBeforeCurrentYear => {
            val liabilityYear = getFirstLiabilityYear(
              request.registration.carriedOutAmlRegulatedActivityInCurrentFy,
              liableBeforeCurrentYear
            )

            val info = RegistrationAdditionalInfo(
              request.registration.internalId,
              liabilityYear,
              request.eclRegistrationReference
            )

            liabilityYear
              .map { year =>
                val liabilityYearSessionData = Map(SessionKeys.LiabilityYear -> year.asString)

                sessionService.upsert(
                  SessionData(
                    request.internalId,
                    liabilityYearSessionData
                  )
                )

                (for (_ <- service.createOrUpdate(info).asResponseError)
                  yield NavigationData(
                    request.registration,
                    "",
                    fromRevenuePage,
                    false,
                    liableBeforeCurrentYear.toString
                  )).convertToResult(mode, pageNavigator, liabilityYearSessionData)
              }
              .getOrElse(
                Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
              )
          }
        )
    }

  private def getFirstLiabilityYear(
    liableForCurrentFY: Option[Boolean],
    liableForPreviousFY: Boolean
  ): Option[LiabilityYear] =
    (liableForCurrentFY, liableForPreviousFY) match {
      case (Some(true), true) | (Some(false), true) => Some(LiabilityYear(TaxYear.current.previous.startYear))
      case (Some(true), false)                      => Some(LiabilityYear(TaxYear.current.currentYear))
      case _                                        => None
    }

  private def isLiableForPreviousFY(info: Option[RegistrationAdditionalInfo]) =
    info.map(_.liabilityYear.exists(_.isNotCurrentFY))
}
