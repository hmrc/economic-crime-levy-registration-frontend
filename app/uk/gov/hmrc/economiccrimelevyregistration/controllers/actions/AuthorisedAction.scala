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

import com.google.inject.Inject
import play.api.mvc.Results._
import play.api.mvc._
import uk.gov.hmrc.auth.core.AffinityGroup.Agent
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals._
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.EnrolmentStoreProxyService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import scala.concurrent.{ExecutionContext, Future}

trait AuthorisedAction
    extends ActionBuilder[AuthorisedRequest, AnyContent]
    with FrontendHeaderCarrierProvider
    with ActionFunction[Request, AuthorisedRequest]

class BaseAuthorisedAction @Inject() (
  override val authConnector: AuthConnector,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  config: AppConfig,
  val parser: BodyParsers.Default
)(implicit val executionContext: ExecutionContext)
    extends AuthorisedAction
    with FrontendHeaderCarrierProvider
    with AuthorisedFunctions {

  override def invokeBlock[A](request: Request[A], block: AuthorisedRequest[A] => Future[Result]): Future[Result] =
    authorised().retrieve(internalId and allEnrolments and groupIdentifier and affinityGroup and credentialRole) {
      case optInternalId ~ enrolments ~ optGroupId ~ optAffinityGroup ~ optCredentialRole =>
        val internalId: String             = optInternalId.getOrElseFail("Unable to retrieve internalId")
        val groupId: String                = optGroupId.getOrElseFail("Unable to retrieve groupIdentifier")
        val affinityGroup: AffinityGroup   = optAffinityGroup.getOrElseFail("Unable to retrieve affinityGroup")
        val credentialRole: CredentialRole = optCredentialRole.getOrElseFail("Unable to retrieve credentialRole")

        val eclEnrolment: Option[Enrolment] = enrolments.enrolments.find(_.key == EclEnrolment.ServiceName)

        affinityGroup match {
          case Agent => Future.successful(Ok("Agent account not supported - must be an organisation or individual"))
          case _     =>
            credentialRole match {
              case Assistant => Future.successful(Ok("User is not an Admin - request an admin to perform registration"))
              case _         =>
                eclEnrolment match {
                  case Some(_) => Future.successful(Ok("Already registered - user already has enrolment"))
                  case None    =>
                    enrolmentStoreProxyService
                      .groupHasEnrolment(groupId)(hc(request))
                      .flatMap {
                        case true  =>
                          Future.successful(Ok("Group already has the enrolment - assign the enrolment to the user"))
                        case false => block(AuthorisedRequest(request, internalId))
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
