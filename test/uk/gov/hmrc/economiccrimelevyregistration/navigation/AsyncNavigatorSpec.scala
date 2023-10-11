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

import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._

import scala.util.{Failure, Success, Try}

class AsyncNavigatorSpec extends SpecBase {

  val pageNavigator = new AsyncPageNavigator {}

  "nextPage" should {
    "throw an exception" in forAll { (registration: Registration, mode: Mode, extraFlag: Boolean) =>
      Try {
        pageNavigator.nextPage(mode, registration)(fakeRequest)
        pageNavigator.nextPage(mode, registration, extraFlag)(fakeRequest)
      } match {
        case Success(_) => fail
        case Failure(_) =>
      }
    }
  }

}
