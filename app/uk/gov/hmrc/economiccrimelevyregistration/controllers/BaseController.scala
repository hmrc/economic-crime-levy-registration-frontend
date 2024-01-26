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
import play.api.Logger
import play.api.http.HeaderNames.CACHE_CONTROL
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.Results.{InternalServerError, Redirect}
import play.api.mvc.{Request, RequestHeader, Result, Results}
import play.twirl.api.Html
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, ErrorCode, ResponseError}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.PageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ErrorTemplate

import scala.concurrent.{ExecutionContext, Future}

trait BaseController extends I18nSupport {

  private def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit
    request: Request[_],
    errorTemplate: ErrorTemplate
  ): Html =
    errorTemplate(pageTitle, heading, message)

  private def internalServerErrorTemplate(implicit request: Request[_], errorTemplate: ErrorTemplate): Html =
    standardErrorTemplate(
      Messages("error.problemWithService.title"),
      Messages("error.problemWithService.heading"),
      Messages("error.problemWithService.message")
    )

  private def fallbackClientErrorTemplate(implicit request: Request[_], errorTemplate: ErrorTemplate): Html =
    standardErrorTemplate(
      Messages("global.error.fallbackClientError4xx.title"),
      Messages("global.error.fallbackClientError4xx.heading"),
      Messages("global.error.fallbackClientError4xx.message")
    )

  def routeError(error: ResponseError)(implicit request: Request[_], errorTemplate: ErrorTemplate): Result =
    error.code match {
      case ErrorCode.BadRequest                                 => Redirect(routes.NotableErrorController.answersAreInvalid())
      case ErrorCode.InternalServerError | ErrorCode.BadGateway =>
        InternalServerError(internalServerErrorTemplate(request, errorTemplate)).withHeaders(
          CACHE_CONTROL -> "no-cache"
        )
      case errorCode                                            => Results.Status(errorCode.statusCode)(fallbackClientErrorTemplate(request, errorTemplate))
    }

  implicit class ResponseHandler(data: EitherT[Future, ResponseError, Registration]) {

    def convertToResult(
      mode: Mode,
      pageNavigator: PageNavigator,
      session: Map[String, String] = Map()
    )(implicit
      ec: ExecutionContext,
      rh: RequestHeader,
      request: Request[_],
      errorTemplate: ErrorTemplate
    ): Future[Result] =
      data.fold(
        error => routeError(error),
        data =>
          Redirect(pageNavigator.nextPage(mode, data))
            .withSession(rh.session ++ session)
      )
  }
}
