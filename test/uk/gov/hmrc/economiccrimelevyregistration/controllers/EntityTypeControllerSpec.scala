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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.PartnershipType
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.{EclRegistrationConnector, IncorporatedEntityIdentificationFrontendConnector, PartnershipIdentificationFrontendConnector, SoleTraderIdentificationFrontendConnector}
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType._
import uk.gov.hmrc.economiccrimelevyregistration.models.grs.GrsCreateJourneyResponse
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.EntityTypeView

import scala.concurrent.Future

class EntityTypeControllerSpec extends SpecBase {

  val view: EntityTypeView                 = app.injector.instanceOf[EntityTypeView]
  val formProvider: EntityTypeFormProvider = new EntityTypeFormProvider()
  val form: Form[EntityType]               = formProvider()

  val mockIncorporatedEntityIdentificationFrontendConnector: IncorporatedEntityIdentificationFrontendConnector =
    mock[IncorporatedEntityIdentificationFrontendConnector]

  val mockSoleTraderIdentificationFrontendConnector: SoleTraderIdentificationFrontendConnector =
    mock[SoleTraderIdentificationFrontendConnector]

  val mockPartnershipIdentificationFrontendConnector: PartnershipIdentificationFrontendConnector =
    mock[PartnershipIdentificationFrontendConnector]

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new EntityTypeController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockIncorporatedEntityIdentificationFrontendConnector,
      mockSoleTraderIdentificationFrontendConnector,
      mockPartnershipIdentificationFrontendConnector,
      mockEclRegistrationConnector,
      formProvider,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "save the selected entity type then redirect to the GRS UK Limited Company journey when the UK Limited Company option is selected" in forAll {
      registration: Registration =>
        new TestContext(registration) {
          when(mockIncorporatedEntityIdentificationFrontendConnector.createLimitedCompanyJourney()(any()))
            .thenReturn(Future.successful(GrsCreateJourneyResponse("test-url")))

          val updatedRegistration: Registration = registration.copy(entityType = Some(UkLimitedCompany))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "UkLimitedCompany")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some("test-url")
        }
    }

    "save the selected entity type then redirect to the GRS Sole Trader journey when the Sole Trader option is selected" in forAll {
      registration: Registration =>
        new TestContext(registration) {
          when(mockSoleTraderIdentificationFrontendConnector.createSoleTraderJourney()(any()))
            .thenReturn(Future.successful(GrsCreateJourneyResponse("test-url")))

          val updatedRegistration: Registration = registration.copy(entityType = Some(SoleTrader))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "SoleTrader")))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some("test-url")
        }
    }

    "save the selected entity type then redirect to the GRS Partnership journey when a Partnership entity type is selected" in forAll {
      (registration: Registration, partnershipType: PartnershipType) =>
        new TestContext(registration) {
          val entityType: EntityType = partnershipType.entityType

          when(
            mockPartnershipIdentificationFrontendConnector.createPartnershipJourney(
              ArgumentMatchers.eq(entityType)
            )(
              any()
            )
          )
            .thenReturn(Future.successful(GrsCreateJourneyResponse("test-url")))

          val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", entityType.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some("test-url")
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]           = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[EntityType] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
