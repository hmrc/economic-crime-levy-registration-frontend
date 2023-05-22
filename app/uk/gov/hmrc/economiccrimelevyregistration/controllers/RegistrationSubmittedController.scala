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
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RegistrationSubmittedView
import uk.gov.hmrc.economiccrimelevyregistration.views.html.BookmarkedRegistrationSubmittedView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class RegistrationSubmittedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  view: RegistrationSubmittedView,
  bookmarkedView: BookmarkedRegistrationSubmittedView
) extends FrontendBaseController
    with I18nSupport {

  private val SESSION: String   = "Session"
  private val ENROLMENT: String = "Enrolment"
  private val NONE: String      = "None"

  def onPageLoad: Action[AnyContent] = authorise { implicit request =>
    val sourceOfEclReference =
      checkSourceOfEclReference(request.session.get(SessionKeys.EclReference), request.eclRegistrationReference)

    val eclReference: String = sourceOfEclReference match {
      case SESSION   => request.session.get(SessionKeys.EclReference).get
      case ENROLMENT => request.eclRegistrationReference.get
      case _         => throw new IllegalStateException("ECL reference number not found in session or in enrolment")
    }

    val firstContactEmailAddress: String = sourceOfEclReference match {
      case SESSION   => request.session.get(SessionKeys.FirstContactEmailAddress).get
      case ENROLMENT => ""
      case _         => throw new IllegalStateException("First contact email address not found in session")
    }

    val secondContactEmailAddress: Option[String] = request.session.get(SessionKeys.SecondContactEmailAddress)

    sourceOfEclReference match {
      case SESSION   => Ok(view(eclReference, firstContactEmailAddress, secondContactEmailAddress))
      case ENROLMENT => Ok(bookmarkedView(eclReference))
    }
  }

  private def checkSourceOfEclReference(sessionSource: Option[String], enrolmentSource: Option[String]): String =
    sessionSource match {
      case Some(_) => SESSION
      case None    =>
        enrolmentSource match {
          case Some(_) => ENROLMENT
          case None    => NONE
        }
    }
}
