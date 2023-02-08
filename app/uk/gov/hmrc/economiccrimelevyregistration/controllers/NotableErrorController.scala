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
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, AuthorisedActionWithoutEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AnswersAreInvalidView, GroupAlreadyEnrolledView, UserAlreadyEnrolledView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class NotableErrorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authoriseWithoutEnrolmentCheck: AuthorisedActionWithoutEnrolmentCheck,
  authoriseWithEnrolmentCheck: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  appConfig: AppConfig,
  userAlreadyEnrolledView: UserAlreadyEnrolledView,
  groupAlreadyEnrolledView: GroupAlreadyEnrolledView,
  answersAreInvalidView: AnswersAreInvalidView
) extends FrontendBaseController
    with I18nSupport {

  def answersAreInvalid: Action[AnyContent] = (authoriseWithEnrolmentCheck andThen getRegistrationData) {
    implicit request =>
      Ok(answersAreInvalidView())
  }

  def userAlreadyEnrolled: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    Ok(
      userAlreadyEnrolledView(
        request.eclRegistrationReference.getOrElse(
          throw new IllegalStateException("ECL registration reference not found in request")
        )
      )
    )
  }

  def groupAlreadyEnrolled: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    val eclRegistrationReference = request.eclRegistrationReference.getOrElse(
      throw new IllegalStateException("ECL registration reference not found in request")
    )
    val addLevyUrl               =
      s"${appConfig.addLevyUrl}/services/${EclEnrolment.ServiceName}/${EclEnrolment.IdentifierKey}~$eclRegistrationReference/users"

    Ok(groupAlreadyEnrolledView(eclRegistrationReference, addLevyUrl))
  }
}
