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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithoutEnrolmentCheck, DataRetrievalAction}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{ErrorTemplate, RegistrationReceivedView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RegistrationReceivedController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedActionWithoutEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  view: RegistrationReceivedView,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  registrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with BaseController
    with ErrorHandler
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    (for {
      _                        <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
      _                        <- registrationService.deleteRegistration(request.internalId).asResponseError
      firstContactEmailAddress <-
        valueOrError(request.registration.contacts.firstContactDetails.emailAddress, "First contact email address")
      secondContactEmailAddress = request.registration.contacts.secondContactDetails.emailAddress
      amlRegulatedActivity     <-
        valueOrError(request.registration.carriedOutAmlRegulatedActivityInCurrentFy, "AML Regulated activity")
      liabilityYear             = request.additionalInfo.flatMap(_.liabilityYear)
      registrationReceivedView  = view(
                                    firstContactEmailAddress,
                                    secondContactEmailAddress,
                                    liabilityYear,
                                    amlRegulatedActivity
                                  )
    } yield registrationReceivedView).fold(
      error => routeError(error),
      registrationReceivedView => Ok(registrationReceivedView)
    )
  }
}
