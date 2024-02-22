/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister

import play.api.mvc.{ActionRefiner, Result}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.deregister.DeregistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DeregistrationDataRetrievalAction @Inject() (
  deregistrationService: DeregistrationService
)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[AuthorisedRequest, DeregistrationDataRequest]
    with FrontendHeaderCarrierProvider
    with ErrorHandler {

  override protected def refine[A](
    request: AuthorisedRequest[A]
  ): Future[Either[Result, DeregistrationDataRequest[A]]] = {
    implicit val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)
    (for {
      deregistration <- deregistrationService.getOrCreate(request.internalId).asResponseError
    } yield deregistration).foldF(
      error => Future.failed(new Exception(error.message)),
      deregistration =>
        Future.successful(
          Right(
            DeregistrationDataRequest(
              request.request,
              request.internalId,
              deregistration,
              request.eclRegistrationReference
            )
          )
        )
    )
  }
}
