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

import cats.data.EitherT
import play.api.mvc.Results.Redirect
import play.api.mvc.{RequestHeader, Result}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{NavigationData, PageNavigator}

import scala.concurrent.{ExecutionContext, Future}

trait BaseController {

  protected def getField[T](
    message: String,
    data: Option[T]
  ): Either[DataRetrievalError, T] =
    data match {
      case Some(value) => Right(value)
      case None        => Left(DataRetrievalError.FieldNotFound(message))
    }

  implicit class ResponseHandler(data: EitherT[Future, ResponseError, NavigationData]) {

    def convertToResult(
      mode: Mode,
      pageNavigator: PageNavigator,
      session: Map[String, String] = Map()
    )(implicit ec: ExecutionContext, rh: RequestHeader): Future[Result] =
      data.fold(
        _ => Redirect(routes.NotableErrorController.answersAreInvalid()),
        data =>
          Redirect(pageNavigator.nextPage(mode, data))
            .withSession(rh.session ++ session)
      )
  }
}
