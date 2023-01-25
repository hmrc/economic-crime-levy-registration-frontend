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

import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{EntityType, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.EntityTypePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.EntityTypeView

import scala.concurrent.Future

class EntityTypeControllerSpec extends SpecBase {

  val view: EntityTypeView                 = app.injector.instanceOf[EntityTypeView]
  val formProvider: EntityTypeFormProvider = new EntityTypeFormProvider()
  val form: Form[EntityType]               = formProvider()

  val pageNavigator: EntityTypePageNavigator = new EntityTypePageNavigator(
    mock[IncorporatedEntityIdentificationFrontendConnector],
    mock[SoleTraderIdentificationFrontendConnector],
    mock[PartnershipIdentificationFrontendConnector]
  ) {
    override protected def navigateInNormalMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call] = Future.successful(onwardRoute)
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new EntityTypeController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck,
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(entityType = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, entityType: EntityType) =>
        new TestContext(registration.copy(entityType = Some(entityType))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(entityType))(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected entity type then redirect to the next page" in forAll {
      (registration: Registration, entityType: EntityType) =>
        new TestContext(registration) {
          val updatedRegistration: Registration = registration.copy(entityType = Some(entityType))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", entityType.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
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
