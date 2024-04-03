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

import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.deregister.DeregistrationDataRequest

import scala.concurrent.{ExecutionContext, Future}

class FakeDeregistrationDataAction(
  deregistration: Deregistration
) extends DeregistrationDataAction {

  override protected implicit val executionContext: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  override protected def refine[A](
    request: AuthorisedRequest[A]
  ): Future[Either[Result, DeregistrationDataRequest[A]]] =
    Future(
      Right(
        DeregistrationDataRequest(
          request.request,
          request.internalId,
          deregistration,
          request.eclRegistrationReference
        )
      )
    )
}
