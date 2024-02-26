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
import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.SessionError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{SessionData, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.SessionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StoreUrlAction @Inject() (
  sessionService: SessionService
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[RegistrationDataRequest, RegistrationDataRequest]
    with FrontendHeaderCarrierProvider
    with ErrorHandler {

  override protected def refine[A](
    request: RegistrationDataRequest[A]
  ): Future[Either[Result, RegistrationDataRequest[A]]] = {
    implicit val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    val sessionData = SessionData(request.internalId, Map(SessionKeys.UrlToReturnTo -> request.uri))

    (for {
      _ <- request.registration.registrationType match {
             case Some(Initial) => sessionService.upsert(sessionData).asResponseError
             case _             => EitherT[Future, SessionError, Unit](Future.successful(Right(()))).asResponseError
           }
    } yield ()).foldF(
      error => Future.failed(new Exception(error.message)),
      _ => Future.successful(Right(request))
    )
  }
}
