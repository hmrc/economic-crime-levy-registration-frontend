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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{contacts, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ConfirmContactAddressPageNavigator @Inject() (eclRegistrationConnector: EclRegistrationConnector)(implicit
  ec: ExecutionContext
) extends AsyncPageNavigator
    with FrontendHeaderCarrierProvider {

  override protected def navigateInNormalMode(
    registration: Registration
  )(implicit request: RequestHeader): Future[Call] =
    registration.useRegisteredOfficeAddressAsContactAddress match {
      case Some(true)  =>
        eclRegistrationConnector
          .upsertRegistration(registration = registration.copy(contactAddress = registration.grsAddressToEclAddress))
          .map(_ => routes.CheckYourAnswersController.onPageLoad())
      case Some(false) => Future.successful(routes.IsUkAddressController.onPageLoad())
      case _           => Future.successful(routes.StartController.onPageLoad())
    }

  override protected def navigateInCheckMode(registration: Registration): Future[Call] = ???

  override def previousPage(registration: Registration): Call = registration.contacts.secondContact match {
    case Some(true)  => contacts.routes.SecondContactNumberController.onPageLoad()
    case Some(false) => contacts.routes.AddAnotherContactController.onPageLoad()
    case _           => routes.StartController.onPageLoad()
  }
}
