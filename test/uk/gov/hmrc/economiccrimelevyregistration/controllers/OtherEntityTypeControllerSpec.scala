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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.OtherEntityTypeDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.OtherEntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.OtherEntityTypePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.OtherEntityTypeView

import scala.concurrent.Future

class OtherEntityTypeControllerSpec extends SpecBase {

  val view: OtherEntityTypeView                 = app.injector.instanceOf[OtherEntityTypeView]
  val formProvider: OtherEntityTypeFormProvider = new OtherEntityTypeFormProvider()
  val form: Form[OtherEntityType]               = formProvider()

  val pageNavigator: OtherEntityTypePageNavigator = new OtherEntityTypePageNavigator(
  ) {
    override protected def navigateInNormalMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call] = Future.successful(onwardRoute)

    override protected def navigateInCheckMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call] = Future.successful(onwardRoute)
  }

  val dataCleanup: OtherEntityTypeDataCleanup = new OtherEntityTypeDataCleanup()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val errorHandler: ErrorHandler                             = app.injector.instanceOf[ErrorHandler]
  override val appConfig: AppConfig                          = mock[AppConfig]

  class TestContext(registrationData: Registration) {

    val controller = new OtherEntityTypeController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      dataCleanup,
      appConfig,
      errorHandler,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration.copy(otherEntityJourneyData = OtherEntityJourneyData.empty())) {
          when(appConfig.privateBetaEnabled).thenReturn(false)

          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "return not found if private beta enabled" in forAll { (registration: Registration, mode: Mode) =>
      new TestContext(registration.copy(otherEntityJourneyData = OtherEntityJourneyData.empty())) {
        when(appConfig.privateBetaEnabled).thenReturn(true)

        val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

        status(result) shouldBe NOT_FOUND
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, entityType: OtherEntityType, mode: Mode) =>
        val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(entityType = Some(entityType))
        new TestContext(registration.copy(otherEntityJourneyData = otherEntityJourneyData)) {
          when(appConfig.privateBetaEnabled).thenReturn(false)

          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(entityType), mode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected entity type then redirect to the next page" in forAll {
      (registration: Registration, entityType: OtherEntityType, mode: Mode) =>
        val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(entityType = Some(entityType))
        new TestContext(registration) {
          val updatedRegistration: Registration = registration.copy(
            otherEntityJourneyData = otherEntityJourneyData
          )

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", entityType.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration) {
          val result: Future[Result]                = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[OtherEntityType] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }
  }
}