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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithoutEnrolmentCheck, RegistrationDataAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, LocalDateService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  getRegistrationData: RegistrationDataAction,
  view: RegistrationSubmittedView,
  outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService,
  localDateService: LocalDateService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val sessionEclReference = request.session.get(SessionKeys.eclReference)
    (request.additionalInfo.flatMap(_.eclReference), sessionEclReference) match {
      case (Some(eclReference), _) => registrationSubmittedRouting(eclReference)
      case (_, Some(eclReference)) => outOfSessionRouting(eclReference)
      case _                       =>
        Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
    }
  }

  private def registrationSubmittedRouting(
    eclReference: String
  )(implicit request: RegistrationDataRequest[_]): Future[Result] =
    (for {
      _                        <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _                        <- registrationService.deleteRegistration(request.internalId).asResponseError
      firstContactEmailAddress <-
        valueOrError(request.session.get(SessionKeys.firstContactEmail), "First contact email address")
      secondContactEmailAddress = request.session.get(SessionKeys.secondContactEmail)
      registeringForCurrentFY  <-
        valueOrError(request.session.get(SessionKeys.registeringForCurrentFY), "Registering for current FY")
      liabilityYear            <- valueOrError(request.session.get(SessionKeys.liabilityYear), "Liability Year")
    } yield (liabilityYear, firstContactEmailAddress, secondContactEmailAddress, registeringForCurrentFY))
      .fold(
        _ => Redirect(routes.NotableErrorController.answersAreInvalid()),
        data => {
          val eclTaxYear: EclTaxYear  = EclTaxYear.fromCurrentDate(localDateService.now())
          val liabilityYear           = data._1.toInt
          val firstContactEmail       = data._2
          val secondContactEmail      = data._3
          val registeringForCurrentFY = data._4.toBoolean
          Ok(
            view(
              eclReference,
              firstContactEmail,
              secondContactEmail,
              Some(LiabilityYear(liabilityYear)),
              Some(registeringForCurrentFY),
              eclTaxYear
            )
          )
        }
      )

  private def outOfSessionRouting(eclReference: String)(implicit
    request: RegistrationDataRequest[_]
  ): Future[Result] = {
    val eclTaxYear              = EclTaxYear.fromCurrentDate(localDateService.now())
    val liabilityYear           = request.session.get(SessionKeys.liabilityYear).map(year => LiabilityYear(year.toInt))
    val registeringForCurrentFY = request.session.get(SessionKeys.registeringForCurrentFY).map(_.toBoolean)

    Future.successful(
      Ok(outOfSessionRegistrationSubmittedView(eclReference, liabilityYear, registeringForCurrentFY, eclTaxYear))
    )
  }
}
