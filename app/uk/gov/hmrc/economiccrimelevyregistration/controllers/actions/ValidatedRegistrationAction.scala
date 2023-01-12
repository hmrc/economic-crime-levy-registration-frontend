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

import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionFilter, Result}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ValidatedRegistrationActionImpl @Inject() (eclRegistrationConnector: EclRegistrationConnector)(implicit
  val executionContext: ExecutionContext
) extends ValidatedRegistrationAction
    with FrontendHeaderCarrierProvider {

  override def filter[A](request: RegistrationDataRequest[A]): Future[Option[Result]] =
    eclRegistrationConnector.getRegistrationValidationErrors(request.internalId)(hc(request)).map {
      case Some(_) => Some(Redirect(routes.StartController.onPageLoad()))
      case None    => None
    }
}

trait ValidatedRegistrationAction extends ActionFilter[RegistrationDataRequest]
