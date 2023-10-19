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
import uk.gov.hmrc.economiccrimelevyregistration.models.SessionKeys
import uk.gov.hmrc.economiccrimelevyregistration.utils.SessionCache
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendmentRequestedView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class AmendmentRequestedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  view: AmendmentRequestedView,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  sessionCache: SessionCache
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    sessionCache.get(SessionKeys.FirstContactEmailAddress, request).map {
      case Some(firstContactEmailAddress) => Ok(view(firstContactEmailAddress))
      case None                           => InternalServerError
    }
  }
}
