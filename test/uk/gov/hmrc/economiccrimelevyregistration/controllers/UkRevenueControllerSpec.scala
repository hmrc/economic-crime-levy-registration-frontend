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
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.UkRevenueFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.UkRevenuePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.UkRevenueView

import scala.concurrent.Future

class UkRevenueControllerSpec extends SpecBase {

  val view: UkRevenueView                 = app.injector.instanceOf[UkRevenueView]
  val formProvider: UkRevenueFormProvider = new UkRevenueFormProvider()
  val form: Form[Long]                    = formProvider()

  val pageNavigator: UkRevenuePageNavigator = new UkRevenuePageNavigator(mock[EclReturnsConnector]) {
    override protected def navigateInNormalMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call] = Future.successful(onwardRoute)
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val minRevenue = 0L
  val maxRevenue = 99999999999L

  class TestContext(registrationData: Registration) {
    val controller = new UkRevenueController(
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
      new TestContext(registration.copy(relevantApRevenue = None)) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, ukRevenue: Long) =>
        new TestContext(
          registration.copy(relevantApRevenue = Some(ukRevenue))
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(ukRevenue), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided UK revenue then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      Gen.chooseNum[Long](minRevenue, maxRevenue)
    ) { (registration: Registration, ukRevenue: Long) =>
      new TestContext(registration) {
        val updatedRegistration: Registration =
          registration.copy(relevantApRevenue = Some(ukRevenue))

        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", ukRevenue.toString)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll(
      Arbitrary.arbitrary[Registration],
      Gen.alphaStr
    ) { (registration: Registration, invalidRevenue: String) =>
      new TestContext(registration) {
        val result: Future[Result]     =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", invalidRevenue)))
        val formWithErrors: Form[Long] = form.bind(Map("value" -> invalidRevenue))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
