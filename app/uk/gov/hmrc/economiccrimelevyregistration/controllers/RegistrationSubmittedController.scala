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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.AuthorisedActionWithoutEnrolmentCheck
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.time.TaxYear

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RegistrationSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  view: RegistrationSubmittedView,
  outOfSessionRegistrationSubmittedView: OutOfSessionRegistrationSubmittedView,
  sessionService: SessionService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    sessionService
      .get(request.session, request.internalId, SessionKeys.LiabilityYear)
      .map(liabilityYear => liabilityYear.map(year => LiabilityYear(year.toInt)))
      .flatMap { liabilityYear =>
        sessionService
          .get(request.session, request.internalId, SessionKeys.AmlRegulatedActivity)
          .map { amlRegulatedActivity =>
            (request.session.get(SessionKeys.EclReference), request.eclRegistrationReference) match {
              case (Some(eclReference), _) =>
                val secondContactEmailAddress: Option[String] =
                  request.session.get(SessionKeys.SecondContactEmailAddress)
                val firstContactEmailAddress                  = request.session(SessionKeys.FirstContactEmailAddress)
                Ok(
                  view(
                    eclReference,
                    firstContactEmailAddress,
                    secondContactEmailAddress,
                    liabilityYear,
                    amlRegulatedActivity
                  )
                )
              case (_, Some(eclReference)) =>
                Ok(outOfSessionRegistrationSubmittedView(eclReference, liabilityYear, amlRegulatedActivity))
              case _                       => throw new IllegalStateException("ECL reference number not found in session or in enrolment")
            }
          }
      }

  }
}
