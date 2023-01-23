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
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}

import scala.concurrent.Future

class ConfirmContactAddressPageNavigatorSpec extends SpecBase {

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val pageNavigator = new ConfirmContactAddressPageNavigator(mockEclRegistrationConnector)

  "nextPage" should {
    "return a Call to the UK address question page in NormalMode when the answer is no" in forAll {
      (registration: Registration) =>
        val updatedRegistration: Registration =
          registration.copy(useRegisteredOfficeAddressAsContactAddress = Some(false))

        await(
          pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.IsUkAddressController
          .onPageLoad()
    }

    "return a Call to the check your answers page in NormalMode when the answer is yes" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile
      ) =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = Some(true),
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        when(
          mockEclRegistrationConnector.upsertRegistration(
            ArgumentMatchers.eq(updatedRegistration.copy(contactAddress = updatedRegistration.grsAddressToEclAddress))
          )(any())
        )
          .thenReturn(Future.successful(updatedRegistration))

        await(
          pageNavigator.nextPage(NormalMode, updatedRegistration)(fakeRequest)
        ) shouldBe routes.CheckYourAnswersController
          .onPageLoad()
    }
  }

}
