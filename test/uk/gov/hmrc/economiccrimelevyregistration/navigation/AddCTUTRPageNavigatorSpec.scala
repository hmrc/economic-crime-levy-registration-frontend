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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.AddressLookupFrontendConnector
import uk.gov.hmrc.economiccrimelevyregistration.models.{Mode, Registration}
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class AddCTUTRPageNavigatorSpec extends SpecBase {

  val mockAddressLookupFrontendConnector: AddressLookupFrontendConnector = mock[AddressLookupFrontendConnector]

  val pageNavigator = new AddCTUTRPageNavigator()

  "nextPage" should {
    "return a call to the address lookup journey in either mode" in {
      (registration: Registration, contactAddressIsUk: Boolean, journeyUrl: String, mode: Mode) =>
        val updatedRegistration: Registration = registration.copy(contactAddressIsUk = Some(contactAddressIsUk))

        when(
          mockAddressLookupFrontendConnector
            .initJourney(ArgumentMatchers.eq(contactAddressIsUk), ArgumentMatchers.eq(mode))(any())
        )
          .thenReturn(Future.successful(journeyUrl))

        pageNavigator.nextPage(mode, updatedRegistration) shouldBe Call(GET, journeyUrl)
    }
  }

}
