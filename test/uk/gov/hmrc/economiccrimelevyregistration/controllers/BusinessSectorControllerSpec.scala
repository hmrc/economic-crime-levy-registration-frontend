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
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.forms.BusinessSectorFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{BusinessSector, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.BusinessSectorPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.BusinessSectorView

import scala.concurrent.Future

class BusinessSectorControllerSpec extends SpecBase {

  val view: BusinessSectorView                 = app.injector.instanceOf[BusinessSectorView]
  val formProvider: BusinessSectorFormProvider = new BusinessSectorFormProvider()
  val form: Form[BusinessSector]               = formProvider()

  val pageNavigator: BusinessSectorPageNavigator = new BusinessSectorPageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call = onwardRoute
    override def previousPage(registration: Registration): Call                   = backRoute
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestContext(registrationData: Registration) {
    val controller = new BusinessSectorController(
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
      new TestContext(registration.copy(businessSector = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, backRoute.url)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, businessSector: BusinessSector) =>
        new TestContext(registration.copy(businessSector = Some(businessSector))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(businessSector), backRoute.url)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected business sector then redirect to the next page" in forAll {
      (registration: Registration, businessSector: BusinessSector) =>
        new TestContext(registration) {
          val updatedRegistration: Registration = registration.copy(businessSector = Some(businessSector))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", businessSector.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]               = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[BusinessSector] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, backRoute.url)(fakeRequest, messages).toString
      }
    }
  }
}
