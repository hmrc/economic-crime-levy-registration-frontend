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

import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, Mode, NormalMode, Registration}

class NavigationException extends Exception("Method not overridden!")

trait PageNavigator {
  private val error = new NavigationException()

  def nextPage(mode: Mode, registration: Registration): Call = mode match {
    case NormalMode => navigateInNormalMode(registration)
    case CheckMode  => navigateInCheckMode(registration)
  }

  def nextPage(mode: Mode, registration: Registration, fromSpecificPage: Boolean): Call = mode match {
    case NormalMode => navigateInNormalMode(registration, fromSpecificPage)
    case CheckMode  => navigateInCheckMode(registration, fromSpecificPage)
  }

  def nextPage(mode: Mode, registration: Registration, url: String, isSame: Boolean): Call = mode match {
    case NormalMode => navigateInNormalMode(registration, url, isSame)
    case CheckMode  => navigateInCheckMode(registration, url, isSame)
  }

  def nextPage(mode: Mode, registration: Registration, url: String): Call = mode match {
    case NormalMode => navigateInNormalMode(registration, url)
    case CheckMode  => navigateInCheckMode(registration, url)
  }

  protected def navigateInNormalMode(registration: Registration): Call =
    throw error

  protected def navigateInCheckMode(registration: Registration): Call =
    throw error

  protected def navigateInNormalMode(registration: Registration, fromSpecificPage: Boolean): Call =
    throw error

  protected def navigateInCheckMode(registration: Registration, fromSpecificPage: Boolean): Call =
    throw error

  protected def navigateInNormalMode(registration: Registration, url: String, IsSame: Boolean): Call =
    throw error

  protected def navigateInCheckMode(registration: Registration, url: String, IsSame: Boolean): Call =
    throw error

  protected def navigateInNormalMode(registration: Registration, url: String): Call =
    throw error

  protected def navigateInCheckMode(registration: Registration, url: String): Call =
    throw error
}
