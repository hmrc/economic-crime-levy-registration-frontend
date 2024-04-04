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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.AlreadyDeregisteredView
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{YouHaveAlreadyRegisteredView, _}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}

@Singleton
class NotableErrorController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authoriseWithoutEnrolmentCheck: AuthorisedActionWithoutEnrolmentCheck,
  authoriseWithEnrolmentCheck: AuthorisedActionWithEnrolmentCheck,
  authoriseAgentsAllowed: AuthorisedActionAgentsAllowed,
  authoriseAssistantsAllowed: AuthorisedActionAssistantsAllowed,
  getRegistrationData: RegistrationDataAction,
  appConfig: AppConfig,
  userAlreadyEnrolledView: UserAlreadyEnrolledView,
  groupAlreadyEnrolledView: GroupAlreadyEnrolledView,
  answersAreInvalidView: AnswersAreInvalidView,
  agentCannotRegisterView: AgentCannotRegisterView,
  assistantCannotRegisterView: AssistantCannotRegisterView,
  organisationAlreadyRegisteredView: OrganisationAlreadyRegisteredView,
  registrationFailedView: RegistrationFailedView,
  partyTypeMismatchView: PartyTypeMismatchView,
  verificationFailedView: VerfificationFailedView,
  youHaveAlreadyRegisteredView: YouHaveAlreadyRegisteredView,
  youAlreadyRequestedToAmendView: YouAlreadyRequestedToAmendView,
  alreadyDeregisteredView: AlreadyDeregisteredView
) extends FrontendBaseController
    with I18nSupport
    with ErrorHandler {

  def answersAreInvalid: Action[AnyContent] = (authoriseWithEnrolmentCheck andThen getRegistrationData) {
    implicit request =>
      Ok(answersAreInvalidView())
  }

  def userAlreadyEnrolled: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    (for {
      eclReference <-
        request.eclReferenceOrError
    } yield eclReference)
      .fold(
        _ => Ok(answersAreInvalidView()),
        success => Ok(userAlreadyEnrolledView(success))
      )
  }

  def groupAlreadyEnrolled: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    def taxAndSchemeManagementUrl(eclReference: String) =
      s"${appConfig.taxAndSchemeManagementUrl}/services/${EclEnrolment.ServiceName}/${EclEnrolment.IdentifierKey}~$eclReference/users"

    (for {
      eclReference <- request.eclReferenceOrError
    } yield eclReference).fold(
      _ => Ok(answersAreInvalidView()),
      success => Ok(groupAlreadyEnrolledView(success, taxAndSchemeManagementUrl(success)))
    )
  }

  def agentCannotRegister: Action[AnyContent] = authoriseAgentsAllowed { implicit request =>
    Ok(agentCannotRegisterView())
  }

  def assistantCannotRegister: Action[AnyContent] = authoriseAssistantsAllowed { implicit request =>
    Ok(assistantCannotRegisterView())
  }

  def organisationAlreadyRegistered(eclReferenceNumber: String): Action[AnyContent] =
    authoriseWithoutEnrolmentCheck { implicit request =>
      Ok(organisationAlreadyRegisteredView(eclReferenceNumber))
    }

  def registrationFailed: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    Ok(registrationFailedView())
  }

  def partyTypeMismatch: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    Ok(partyTypeMismatchView())
  }

  def verificationFailed: Action[AnyContent] = (authoriseWithoutEnrolmentCheck andThen getRegistrationData) {
    implicit request =>
      (for {
        entityType <- request.entityTypeOrError
      } yield entityType).fold(
        _ => Ok(answersAreInvalidView()),
        _ => Ok(verificationFailedView())
      )
  }

  def youHaveAlreadyRegistered: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    Ok(youHaveAlreadyRegisteredView())
  }

  def youAlreadyRequestedToAmend: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    Ok(youAlreadyRequestedToAmendView())
  }

  def alreadyDeregistered: Action[AnyContent] = authoriseWithoutEnrolmentCheck { implicit request =>
    Ok(alreadyDeregisteredView())
  }
}
