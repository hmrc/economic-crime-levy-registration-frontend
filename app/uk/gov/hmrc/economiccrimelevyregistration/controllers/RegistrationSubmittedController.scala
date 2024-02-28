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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithoutEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RegistrationSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  view: RegistrationSubmittedView,
  outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val sessionEclReference = request.session.get(SessionKeys.EclReference)
    (request.additionalInfo.flatMap(_.eclReference), sessionEclReference) match {
      case (Some(eclReference), _) => registrationSubmittedRouting(eclReference)
      case (_, Some(eclReference)) => outOfSessionRouting(eclReference)
      case _                       =>
        Future.successful(Redirect(routes.NotableErrorController.answersAreInvalid()))
    }
  }

  private def registrationSubmittedRouting(eclReference: String)(implicit request: RegistrationDataRequest[_]) =
    (for {
      _                        <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _                        <- registrationService.deleteRegistration(request.internalId).asResponseError
      firstContactEmailAddress <-
        valueOrError(request.registration.contacts.firstContactDetails.emailAddress, "First contact email address")
      secondContactEmailAddress = request.registration.contacts.secondContactDetails.emailAddress
      liabilityYear             =
        request.additionalInfo.flatMap(_.liabilityStartDate.map(_.getYear)).map(year => LiabilityYear(year))
    } yield (liabilityYear, firstContactEmailAddress, secondContactEmailAddress))
      .fold(
        _ => Redirect(routes.NotableErrorController.answersAreInvalid()),
        data => {
          val liabilityYear      = data._1
          val firstContactEmail  = data._2
          val secondContactEmail = data._3
          Ok(view(eclReference, firstContactEmail, secondContactEmail, liabilityYear))
        }
      )

  private def outOfSessionRouting(eclReference: String)(implicit
    request: RegistrationDataRequest[_]
  ) = {
    val liabilityYear = request.session.get(SessionKeys.LiabilityYear).map(year => LiabilityYear(year.toInt))

    Future.successful(Ok(outOfSessionRegistrationSubmittedView(eclReference, liabilityYear)))
  }
}
