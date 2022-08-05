/*
 * Copyright 2022 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{DataRequest, IdentifierRequest}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DataRetrievalActionImpl @Inject() (
  val eclRegistrationService: EclRegistrationService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction {

  override protected def transform[A](request: IdentifierRequest[A]): Future[DataRequest[A]] =
    eclRegistrationService.getOrCreateRegistration(request.internalId).map {
      DataRequest(request.request, request.internalId, _)
    }
}

trait DataRetrievalAction extends ActionTransformer[IdentifierRequest, DataRequest]
