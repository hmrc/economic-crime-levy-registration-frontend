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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Session}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendmentRequestedView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendmentRequestedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: AmendmentRequestedView,
  authorise: AuthorisedActionWithEnrolmentCheck,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def getSessionData(key: String)(implicit
    request: AuthorisedRequest[AnyContent]
  ): String =
    request.session.get(key) match {
      case Some(value) => value
      case None        => ""
    }

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    sessionService
      .get(request.session, request.internalId, SessionKeys.FirstContactEmailAddress)
      .map {
        case Some(firstContactEmailAddress) =>
          Ok(
            view(
              firstContactEmailAddress,
              request.eclRegistrationReference,
              getSessionData(SessionKeys.addressLine1),
              getSessionData(SessionKeys.addressLine2),
              getSessionData(SessionKeys.addressLine3),
              getSessionData(SessionKeys.addressLine4)
            )
          )
        case None                           =>
          logger.error("Failed to retrieve first contact email address from session and database")
          InternalServerError
      }
      .recover { case e =>
        logger.error(
          s"Unable to retrieve first contact email address from session: ${e.getMessage}"
        )
        InternalServerError
      }
  }
}
