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
import org.scalacheck.{Arbitrary, Gen}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.RelevantApLengthFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.RelevantApLengthPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.RelevantApLengthView

import scala.concurrent.Future

class RelevantApLengthControllerSpec extends SpecBase {

  val view: RelevantApLengthView                 = app.injector.instanceOf[RelevantApLengthView]
  val formProvider: RelevantApLengthFormProvider = new RelevantApLengthFormProvider()
  val form: Form[Int]                            = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val pageNavigator: RelevantApLengthPageNavigator = new RelevantApLengthPageNavigator() {
    override protected def navigateInNormalMode(
      registration: Registration
    ): Call = onwardRoute
  }

  val minLength = 1
  val maxLength = 999

  class TestContext(registrationData: Registration) {
    val controller = new RelevantApLengthController(
      mcc,
      fakeAuthorisedAction,
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(relevantApLength = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, relevantApLength: Int) =>
        new TestContext(
          registration.copy(relevantApLength = Some(relevantApLength))
        ) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(relevantApLength))(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided UK revenue then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      Gen.chooseNum[Int](minLength, maxLength)
    ) { (registration: Registration, relevantApLength: Int) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(relevantApLength = Some(relevantApLength))

        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", relevantApLength.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll(
      Arbitrary.arbitrary[Registration],
      Gen.alphaStr
    ) { (registration: Registration, invalidLength: String) =>
      new TestContext(registration) {
        val result: Future[Result]    =
          controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", invalidLength)))
        val formWithErrors: Form[Int] = form.bind(Map("value" -> invalidLength))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
