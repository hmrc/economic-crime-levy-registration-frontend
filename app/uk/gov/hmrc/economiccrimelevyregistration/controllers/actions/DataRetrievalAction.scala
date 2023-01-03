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

import play.api.mvc.ActionTransformer
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{AuthorisedRequest, RegistrationDataRequest}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationDataRetrievalAction @Inject() (
  val eclRegistrationService: EclRegistrationService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with FrontendHeaderCarrierProvider {

  override protected def transform[A](request: AuthorisedRequest[A]): Future[RegistrationDataRequest[A]] =
    eclRegistrationService.getOrCreateRegistration(request.internalId)(hc(request)).map {
      RegistrationDataRequest(request.request, request.internalId, _)
    }
}

trait DataRetrievalAction extends ActionTransformer[AuthorisedRequest, RegistrationDataRequest]
