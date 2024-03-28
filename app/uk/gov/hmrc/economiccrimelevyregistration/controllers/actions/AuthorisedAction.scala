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

import cats.data.EitherT
import com.google.inject.{ImplementedBy, Inject}
import play.api.Logging
import play.api.mvc.Results._
import play.api.mvc.{request, _}
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{ErrorHandler, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType._
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{ErrorCode, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EnrolmentStoreProxyService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

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
    with ErrorHandler
    with Logging {

  val checkForEclEnrolment: Boolean
  val agentsAllowed: Boolean
  val assistantsAllowed: Boolean

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
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
                  processEnrolment(request, internalId, block, groupId, eclEnrolment, eclRegistrationReference)(
                    hc(request)
                  )
                } else {
                  processEclReference(request, internalId, block, groupId, eclRegistrationReference)(hc(request))
                }
            }
        }
      case _                                                                                          => Future.failed(new Exception("Failed to authorise due to missing data"))
    }(hc(request), executionContext) recover { case e: NoActiveSession =>
      logger.warn(s"[BaseAuthorisedAction][invokeBlock] NoActiveSession failure: ${e.reason}")
      Redirect(config.signInUrl, Map("continue" -> Seq(s"${config.host}${request.uri}")))
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
        if (request.uri.toLowerCase.contains("amend-") || request.uri.toLowerCase.contains("deregister-")) {
          block(
            AuthorisedRequest(
              request,
              internalId,
              groupId,
              eclRegistrationReference
            )
          )
        } else {
          retrieveRegistrationAndRedirect(request, internalId, block, groupId, eclRegistrationReference)
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

  private def retrieveRegistrationAndRedirect[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclRegistrationReference: Option[String]
  )(implicit hc: HeaderCarrier): Future[Result] = {
    val redirectRegType = (registration: Registration) =>
      registration.registrationType match {
        case None                             =>
          Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled().url))
        case Some(Amendment | DeRegistration) =>
          block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
        case Some(Initial)                    =>
          Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled().url))
      }

    val initialUrl = routes.CheckYourAnswersController.onPageLoad(Initial).url
    val amendUrl   = routes.CheckYourAnswersController.onPageLoad(Amendment).url
    request.uri match {
      case uri if uri == initialUrl || uri == amendUrl =>
        (for {
          registrationOption <- eclRegistrationService.get(internalId).asResponseError
          registration       <- extractRegistration(registrationOption)
        } yield registration).foldF(
          error =>
            error.code match {
              case ErrorCode.NotFound =>
                if (uri == initialUrl) {
                  Future.successful(Redirect(routes.NotableErrorController.youHaveAlreadyRegistered().url))
                } else {
                  Future.successful(Redirect(routes.NotableErrorController.youAlreadyRequestedToAmend().url))
                }
              case _                  => Future.failed(new Exception(error.message))
            },
          registration => redirectRegType(registration)
        )
      case _                                           =>
        (for {
          registration <- eclRegistrationService.getOrCreate(internalId).asResponseError
        } yield registration).foldF(
          _ => Future.successful(Redirect(routes.NotableErrorController.registrationFailed())),
          registration => redirectRegType(registration)
        )
    }
  }

  private def extractRegistration(registrationOption: Option[Registration]) = {
    println("EXTRACT: " + registrationOption)
    EitherT {
      Future.successful(
        registrationOption match {
          case Some(registration) => Right(registration)
          case None               => Left(ResponseError.notFoundError("Failed to find registration"))
        }
      )
    }
  }
  private def processAssistant[A](
    request: Request[A],
    internalId: String,
    block: AuthorisedRequest[A] => Future[Result],
    groupId: String,
    eclRegistrationReference: Option[String]
  ): Future[Result]                             =
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
