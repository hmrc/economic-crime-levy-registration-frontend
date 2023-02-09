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
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.EnrolmentStoreProxyService
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
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionWithoutEnrolmentCheck {
  override val checkForEclEnrolment: Boolean = false
  override val agentsAllowed: Boolean        = false
  override val assistantsAllowed: Boolean    = false
}

class AuthorisedActionWithEnrolmentCheckImpl @Inject() (
  override val authConnector: AuthConnector,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionWithEnrolmentCheck {
  override val checkForEclEnrolment: Boolean = true
  override val agentsAllowed: Boolean        = false
  override val assistantsAllowed: Boolean    = false
}

class AuthorisedActionAgentsAllowedImpl @Inject() (
  override val authConnector: AuthConnector,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionAgentsAllowed {
  override val checkForEclEnrolment: Boolean = false
  override val agentsAllowed: Boolean        = true
  override val assistantsAllowed: Boolean    = false
}

class AuthorisedActionAssistantsAllowedImpl @Inject() (
  override val authConnector: AuthConnector,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  override val parser: BodyParsers.Default
)(override implicit val executionContext: ExecutionContext)
    extends BaseAuthorisedAction(authConnector, enrolmentStoreProxyService, config, parser)
    with AuthorisedActionAssistantsAllowed {
  override val checkForEclEnrolment: Boolean = false
  override val agentsAllowed: Boolean        = false
  override val assistantsAllowed: Boolean    = true
}

abstract class BaseAuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedAction
    with FrontendHeaderCarrierProvider
    with AuthorisedFunctions {

  val checkForEclEnrolment: Boolean
  val agentsAllowed: Boolean
  val assistantsAllowed: Boolean

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    authorised().retrieve(internalId and allEnrolments and groupIdentifier and affinityGroup and credentialRole) {
      case optInternalId ~ enrolments ~ optGroupId ~ optAffinityGroup ~ optCredentialRole =>
        val internalId: String             = optInternalId.getOrElseFail("Unable to retrieve internalId")
        val groupId: String                = optGroupId.getOrElseFail("Unable to retrieve groupIdentifier")
        val affinityGroup: AffinityGroup   = optAffinityGroup.getOrElseFail("Unable to retrieve affinityGroup")
        val credentialRole: CredentialRole = optCredentialRole.getOrElseFail("Unable to retrieve credentialRole")

        val eclEnrolment: Option[Enrolment]          = enrolments.enrolments.find(_.key == EclEnrolment.ServiceName)
        val eclRegistrationReference: Option[String] =
          eclEnrolment.flatMap(_.getIdentifier(EclEnrolment.IdentifierKey).map(_.value))

        affinityGroup match {
          case Agent =>
            if (agentsAllowed) {
              block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
            } else {
              Future.successful(Redirect(routes.NotableErrorController.agentCannotRegister()))
            }
          case _     =>
            credentialRole match {
              case Assistant =>
                if (assistantsAllowed) {
                  block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
                } else {
                  Future.successful(Redirect(routes.NotableErrorController.assistantCannotRegister()))
                }
              case _         =>
                if (checkForEclEnrolment) {
                  eclEnrolment match {
                    case Some(_) => Future.successful(Redirect(routes.NotableErrorController.userAlreadyEnrolled().url))
                    case None    =>
                      enrolmentStoreProxyService
                        .getEclReferenceFromGroupEnrolment(groupId)(hc(request))
                        .flatMap {
                          case Some(_) =>
                            Future.successful(Redirect(routes.NotableErrorController.groupAlreadyEnrolled().url))
                          case None    => block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
                        }
                  }
                } else {
                  eclRegistrationReference match {
                    case Some(_) => block(AuthorisedRequest(request, internalId, groupId, eclRegistrationReference))
                    case None    =>
                      enrolmentStoreProxyService
                        .getEclReferenceFromGroupEnrolment(groupId)(hc(request))
                        .flatMap { eclReferenceFromGroupEnrolment =>
                          block(AuthorisedRequest(request, internalId, groupId, eclReferenceFromGroupEnrolment))
                        }
                  }
                }
            }
        }

    }(hc(request), executionContext) recover { case _: NoActiveSession =>
      Redirect(config.signInUrl, Map("continue" -> Seq(s"${config.host}${request.uri}")))
    }

  implicit class OptionOps[T](o: Option[T]) {
    def getOrElseFail(failureMessage: String): T = o.getOrElse(throw new IllegalStateException(failureMessage))
  }
}
