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
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmlRegulatedActivityStartDateFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmlRegulatedActivityStartDatePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlRegulatedActivityStartDateView

import java.time.LocalDate
import scala.concurrent.Future

class AmlRegulatedActivityStartDateControllerSpec extends SpecBase {

  val view: AmlRegulatedActivityStartDateView                 = app.injector.instanceOf[AmlRegulatedActivityStartDateView]
  val formProvider: AmlRegulatedActivityStartDateFormProvider = new AmlRegulatedActivityStartDateFormProvider()
  val form: Form[LocalDate]                                   = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val pageNavigator: AmlRegulatedActivityStartDatePageNavigator = new AmlRegulatedActivityStartDatePageNavigator {
    override protected def navigateInNormalMode(registration: Registration): Call = onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new AmlRegulatedActivityStartDateController(
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
      new TestContext(registration.copy(amlRegulatedActivityStartDate = None)) {
        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, amlStartDate: LocalDate) =>
        new TestContext(registration.copy(amlRegulatedActivityStartDate = Some(amlStartDate))) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(amlStartDate))(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the provided Aml start date then redirect to the next page" in forAll {
      (registration: Registration, amlStartDate: LocalDate) =>
        new TestContext(registration) {
          val updatedRegistration: Registration =
            registration.copy(amlRegulatedActivityStartDate = Some(amlStartDate))

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(
              fakeRequest.withFormUrlEncodedBody(
                ("value.day", amlStartDate.getDayOfMonth.toString),
                ("value.month", amlStartDate.getMonthValue.toString),
                ("value.year", amlStartDate.getYear.toString)
              )
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]          = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[LocalDate] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
      }
    }
  }
}
