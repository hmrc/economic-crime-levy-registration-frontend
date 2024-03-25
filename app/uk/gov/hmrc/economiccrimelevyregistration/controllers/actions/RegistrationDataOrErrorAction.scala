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
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import uk.gov.hmrc.economiccrimelevyregistration.controllers.{ErrorHandler, routes}
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{ErrorCode, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{AuthorisedRequest, RegistrationDataRequest}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.http.HeaderCarrierConverter

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RegistrationDataOrErrorAction @Inject() (
  eclRegistrationService: EclRegistrationService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService
)(implicit val executionContext: ExecutionContext)
    extends DataRetrievalAction
    with FrontendHeaderCarrierProvider
    with ErrorHandler {

  override protected def refine[A](
    request: AuthorisedRequest[A]
  ): Future[Either[Result, RegistrationDataRequest[A]]] = {
    implicit val hcFromRequest: HeaderCarrier = HeaderCarrierConverter.fromRequestAndSession(request, request.session)

    (for {
      registrationOption <- eclRegistrationService.get(request.internalId).asResponseError
      registration       <- extractRegistration(registrationOption)
      info               <- registrationAdditionalInfoService
                              .getOrCreate(request.internalId, request.eclRegistrationReference)
                              .asResponseError
    } yield (registration, info)).foldF(
      error =>
        error.code match {
          case ErrorCode.NotFound =>
            Future.successful(Left(Redirect(routes.NotableErrorController.youHaveAlreadyRegistered())))
          case _                  => Future.failed(new Exception(error.message))
        },
      data =>
        Future.successful(
          Right(
            RegistrationDataRequest(
              request.request,
              request.internalId,
              data._1,
              Some(data._2),
              request.eclRegistrationReference
            )
          )
        )
    )
  }

  private def extractRegistration(registrationOption: Option[Registration]) =
    EitherT {
      Future.successful(
        registrationOption match {
          case Some(registration) => Right(registration)
          case None               => Left(ResponseError.notFoundError("Failed to find registration"))
        }
      )
    }

}
