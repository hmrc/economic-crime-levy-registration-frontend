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
import uk.gov.hmrc.economiccrimelevyregistration.forms.DoYouHaveUtrFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, OtherEntityJourneyData, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.DoYouHaveUtrPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.DoYouHaveUtrView

import scala.concurrent.Future

class DoYouHaveUtrControllerSpec extends SpecBase {

  val view: DoYouHaveUtrView                 = app.injector.instanceOf[DoYouHaveUtrView]
  val formProvider: DoYouHaveUtrFormProvider = new DoYouHaveUtrFormProvider()
  val form: Form[Boolean]                    = formProvider()

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val pageNavigator: DoYouHaveUtrPageNavigator = new DoYouHaveUtrPageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call =
      onwardRoute

    override protected def navigateInCheckMode(registration: Registration): Call =
      onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new DoYouHaveUtrController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      formProvider,
      mockEclRegistrationService,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(optOtherEntityJourneyData = None)) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, hasUtr: Boolean) =>
        val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(hasUtr)
          )
        val updatedRegistration: Registration              =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(hasUtr), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, hasUtr: Boolean) =>
        new TestContext(registration) {
          val otherEntityJourneyData: OtherEntityJourneyData = registration.otherEntityJourneyData
            .copy(
              isCtUtrPresent = Some(hasUtr)
            )
          val updatedRegistration: Registration              =
            registration.copy(
              optOtherEntityJourneyData = Some(otherEntityJourneyData)
            )

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
              fakeRequest.withFormUrlEncodedBody(("value", hasUtr.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]        = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
