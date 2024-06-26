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

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.UtrTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.navigation.UtrTypePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.UtrTypeView

import scala.concurrent.Future

class UtrTypeControllerSpec extends SpecBase {

  val view: UtrTypeView                 = app.injector.instanceOf[UtrTypeView]
  val formProvider: UtrTypeFormProvider = new UtrTypeFormProvider()
  val form: Form[UtrType]               = formProvider()

  val pageNavigator: UtrTypePageNavigator = new UtrTypePageNavigator() {
    override protected def navigateInNormalMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = onwardRoute

    override protected def navigateInCheckMode(
      eclRegistrationModel: EclRegistrationModel
    ): Call = onwardRoute
  }

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  override val appConfig: AppConfig                      = mock[AppConfig]

  class TestContext(registrationData: Registration) {
    val controller = new UtrTypeController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration.copy(optOtherEntityJourneyData = Some(OtherEntityJourneyData.empty()))) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, utrType: UtrType, mode: Mode) =>
        val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(utrType = Some(utrType))
        new TestContext(registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(utrType), mode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected UTR type then redirect to the next page" in forAll {
      (registration: Registration, utrType: UtrType, mode: Mode) =>
        val otherEntityJourneyData = registration.otherEntityJourneyData.copy(utrType = Some(utrType))
        new TestContext(registration) {
          val updatedRegistration: Registration = registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", utrType.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration) {
          val result: Future[Result]        = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[UtrType] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }
  }
}
