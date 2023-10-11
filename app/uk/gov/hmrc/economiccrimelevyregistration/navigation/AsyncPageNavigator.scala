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
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}

import scala.concurrent.Future

class NavigationException extends Exception("Should never get here!")

trait AsyncPageNavigator {
  private val error = new NavigationException()

  def nextPage(mode: Mode, registration: Registration)(implicit request: RequestHeader): Future[Call] = mode match {
    case NormalMode => navigateInNormalMode(registration)
    case CheckMode  => navigateInCheckMode(registration)
  }

  def nextPage(mode: Mode, registration: Registration, extraFlag: Boolean)(implicit
    request: RequestHeader
  ): Future[Call] = mode match {
    case NormalMode => navigateInNormalMode(registration, extraFlag)
    case CheckMode  => navigateInCheckMode(registration, extraFlag)
  }

  protected def navigateInNormalMode(registration: Registration)(implicit request: RequestHeader): Future[Call] =
    throw error

  protected def navigateInCheckMode(registration: Registration)(implicit request: RequestHeader): Future[Call] =
    throw error

  protected def navigateInNormalMode(registration: Registration, extraFlag: Boolean)(implicit
    request: RequestHeader
  ): Future[Call] =
    throw error

  protected def navigateInCheckMode(registration: Registration, extraFlag: Boolean)(implicit
    request: RequestHeader
  ): Future[Call] =
    throw error

}
