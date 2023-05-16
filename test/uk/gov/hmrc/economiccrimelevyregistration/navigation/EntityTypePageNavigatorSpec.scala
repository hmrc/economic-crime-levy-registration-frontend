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

import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Call
import uk.gov.hmrc.economiccrimelevyregistration.{PartnershipType, UkCompanyType}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType.SoleTrader
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.GrsCreateJourneyResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Mode, Registration}
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class EntityTypePageNavigatorSpec extends SpecBase {

  val mockIncorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector =
    mock[IncorporatedEntityIdentificationFrontendConnector]
  val mockSoleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector                 =
    mock[SoleTraderIdentificationFrontendConnector]
  val mockPartnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector               =
    mock[PartnershipIdentificationFrontendConnector]

  val pageNavigator = new EntityTypePageNavigator(
    mockIncorporatedEntityIdentificationFrontendConnector,
    mockSoleTraderIdentificationFrontendConnector,
    mockPartnershipIdentificationFrontendConnector
  )

  "nextPage" should {
    "return a Call to the UK company GRS journey irrespective of mode when the limited company or unlimited company option is selected" in forAll {
      (registration: Registration, journeyUrl: String, companyType: UkCompanyType, mode: Mode) =>
        val entityType                        = companyType.entityType
        val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

        when(
          mockIncorporatedEntityIdentificationFrontendConnector
            .createUkCompanyJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(
              any()
            )
        )
          .thenReturn(Future.successful(GrsCreateJourneyResponse(journeyUrl)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, journeyUrl)
    }

    "return a Call to the sole trader GRS journey irrespective of mode when the sole trader option is selected" in forAll {
      (registration: Registration, journeyUrl: String, mode: Mode) =>
        val updatedRegistration: Registration = registration.copy(entityType = Some(SoleTrader))

        when(mockSoleTraderIdentificationFrontendConnector.createSoleTraderJourney(ArgumentMatchers.eq(mode))(any()))
          .thenReturn(Future.successful(GrsCreateJourneyResponse(journeyUrl)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, journeyUrl)
    }

    "return a Call to the partnership GRS journey irrespective of mode when a partnership option is selected" in forAll {
      (registration: Registration, partnershipType: PartnershipType, journeyUrl: String, mode: Mode) =>
        val entityType: EntityType = partnershipType.entityType

        val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

        when(
          mockPartnershipIdentificationFrontendConnector
            .createPartnershipJourney(ArgumentMatchers.eq(entityType), ArgumentMatchers.eq(mode))(
              any()
            )
        )
          .thenReturn(Future.successful(GrsCreateJourneyResponse(journeyUrl)))

        await(pageNavigator.nextPage(mode, updatedRegistration)(fakeRequest)) shouldBe Call(GET, journeyUrl)
    }
  }

}
