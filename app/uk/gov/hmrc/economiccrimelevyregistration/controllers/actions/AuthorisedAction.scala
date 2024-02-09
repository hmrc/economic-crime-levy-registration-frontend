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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions

import com.google.inject.{ImplementedBy, Inject}
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{ErrorHandler, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EnrolmentStoreProxyService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with ActionFunction[Request, AuthorisedRequest]

@ImplementedBy(classOf[AuthorisedActionWithoutEnrolmentCheckImpl])
trait AuthorisedActionWithoutEnrolmentCheck extends AuthorisedAction

@ImplementedBy(classOf[AuthorisedActionWithEnrolmentCheckImpl])
trait AuthorisedActionWithEnrolmentCheck extends AuthorisedAction

@ImplementedBy(classOf[AuthorisedActionAgentsAllowedImpl])
trait AuthorisedActionAgentsAllowed extends AuthorisedAction

@ImplementedBy(classOf[AuthorisedActionAssistantsAllowedImpl])
trait AuthorisedActionAssistantsAllowed extends AuthorisedAction

class AuthorisedActionWithoutEnrolmentCheckImpl @Inject() (
  override val authConnector: AuthConnector,
  eclRegistrationService: EclRegistrationService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, eclRegistrationService, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionWithoutEnrolmentCheck {
  override val checkForEclEnrolment: Boolean = false
  override val agentsAllowed: Boolean        = false
  override val assistantsAllowed: Boolean    = false
}

class AuthorisedActionWithEnrolmentCheckImpl @Inject() (
  override val authConnector: AuthConnector,
  eclRegistrationService: EclRegistrationService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, eclRegistrationService, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionWithEnrolmentCheck {
  override val checkForEclEnrolment: Boolean = true
  override val agentsAllowed: Boolean        = false
  override val assistantsAllowed: Boolean    = false
}

class AuthorisedActionAgentsAllowedImpl @Inject() (
  override val authConnector: AuthConnector,
  eclRegistrationService: EclRegistrationService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, eclRegistrationService, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionAgentsAllowed {
  override val checkForEclEnrolment: Boolean = false
  override val agentsAllowed: Boolean        = true
  override val assistantsAllowed: Boolean    = false
}

class AuthorisedActionAssistantsAllowedImpl @Inject() (
  override val authConnector: AuthConnector,
  eclRegistrationService: EclRegistrationService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, eclRegistrationService, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionAssistantsAllowed {
  override val checkForEclEnrolment: Boolean = false
  override val agentsAllowed: Boolean        = false
  override val assistantsAllowed: Boolean    = true
}

abstract class BaseAuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  eclRegistrationService: EclRegistrationService,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedAction
    with FrontendHeaderCarrierProvider
    with AuthorisedFunctions
    with ErrorHandler {

  val checkForEclEnrolment: Boolean
  val agentsAllowed: Boolean
  val assistantsAllowed: Boolean

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] = {
    implicit val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    authorised().retrieve(internalId and allEnrolments and groupIdentifier and affinityGroup and credentialRole) {
      case Some(internalId) ~ enrolments ~ Some(groupId) ~ Some(affinityGroup) ~ Some(credentialRole) =>
        val eclEnrolment: Option[Enrolment]          = enrolments.enrolments.find(_.key == EclEnrolment.ServiceName)
        val eclRegistrationReference: Option[String] =
          eclEnrolment.flatMap(_.getIdentifier(EclEnrolment.IdentifierKey).map(_.value))

        affinityGroup match {
          case Agent =>
            processAgent(request, internalId, block, groupId, eclRegistrationReference)
          case _     =>
            credentialRole match {
              case Assistant =>
                processAssistant(request, internalId, block, groupId, eclRegistrationReference)
              case _         =>
                if (checkForEclEnrolment) {
                  processEnrolment(request, internalId, block, groupId, eclEnrolment, eclRegistrationReference)
                } else {
                  processEclReference(request, internalId, block, groupId, eclRegistrationReference)
                }
            }
        }
      case _                                                                                          => Future.failed(new Exception("Failed to authorise due to missing data"))
    } recover { case _: NoActiveSession =>
      Redirect(config.signInUrl, Map("continue" -> Seq(s"${config.host}${request.uri}")))
    }
  }

  private def processEclReference[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclRegistrationReference: Option[String]
  )(implicit hc: HeaderCarrier): Future[Result] =
    eclRegistrationReference match {
      case Some(_) =>
        block(
          AuthorisedRequest(
            request,
            internalId,
            groupId,
            eclRegistrationReference
          )
        )
      case None    =>
        (for {
          eclReferenceFromGroupEnrolment <-
            enrolmentStoreProxyService.getEclReferenceFromGroupEnrolment(groupId)
        } yield eclReferenceFromGroupEnrolment).foldF(
          _ => block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference)),
          _ => Future.successful(Redirect(routes.NotableErrorController.groupAlreadyEnrolled()))
        )
    }

  private def processEnrolment[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclEnrolment: Option[Enrolment],
    eclRegistrationReference: Option[String]
  )(implicit hc: HeaderCarrier): Future[Result] =
    eclEnrolment match {
      case Some(_) =>
        if (request.uri.toLowerCase.contains("amend-")) {
          block(
            AuthorisedRequest(
              request,
              internalId,
              groupId,
              eclRegistrationReference
            )
          )
        } else {
          (for {
            registration <- eclRegistrationService.getOrCreate(internalId).asResponseError
          } yield registration).foldF(
            _ => Future.successful(Redirect(routes.NotableErrorController.registrationFailed())),
            registration =>
              registration.registrationType match {
                case None            =>
                  Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled().url))
                case Some(Amendment) =>
                  block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
                case Some(Initial)   =>
                  Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled().url))
              }
          )
        }
      case None    =>
        (for {
          eclReference <-
            enrolmentStoreProxyService.getEclReferenceFromGroupEnrolment(groupId)
        } yield eclReference).foldF(
          _ => block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference)),
          _ => Future.successful(Redirect(routes.NotableErrorController.groupAlreadyEnrolled()))
        )
    }

  private def processRegistrationType[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclRegistrationReference: Option[String],
    registration: Registration
  ): Future[Result] =
    registration.registrationType match {
      case None            =>
        Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled()))
      case Some(Amendment) =>
        block(
          AuthorisedRequest(
            request,
            internalId,
            groupId,
            eclRegistrationReference
          )
        )
      case Some(Initial)   =>
        Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled()))
    }

  private def processAssistant[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclRegistrationReference: Option[String]
  ): Future[Result] =
    if (assistantsAllowed) {
      block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
    } else {
      Future.successful(Redirect(routes.NotableErrorController.assistantCannotRegister()))
    }

  private def processAgent[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclRegistrationReference: Option[String]
  ): Future[Result] =
    if (agentsAllowed) {
      block(
        AuthorisedRequest(
          request,
          internalId,
          groupId,
          eclRegistrationReference
        )
      )
    } else {
      Future.successful(Redirect(routes.NotableErrorController.agentCannotRegister()))
    }
}
