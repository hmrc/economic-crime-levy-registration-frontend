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

package uk.gov.hmrc.economiccrimelevyregistration.navigation

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}

import scala.concurrent.Future

trait PageNavigator {
  def navigate(mode: Mode, registration: Registration): Call = mode match {
    case NormalMode => navigateInNormalMode(registration)
    case CheckMode  => navigateInCheckMode(registration)
  }

  def navigateAsync(mode: Mode, registration: Registration): Future[Call] = mode match {
    case NormalMode => navigateInNormalModeAsync(registration)
    case CheckMode  => navigateInCheckModeAsync(registration)
  }

  protected def navigateInNormalMode(registration: Registration): Call =
    throw new NotImplementedError("navigateInNormalMode is not implemented on this page")

  protected def navigateInCheckMode(registration: Registration): Call =
    throw new NotImplementedError("navigateInCheckMode is not implemented on this page")

  protected def navigateInNormalModeAsync(registration: Registration): Future[Call] =
    throw new NotImplementedError("navigateInNormalModeAsync is not implemented on this page")

  protected def navigateInCheckModeAsync(registration: Registration): Future[Call] =
    throw new NotImplementedError("navigateInCheckModeAsync is not implemented on this page")
}
