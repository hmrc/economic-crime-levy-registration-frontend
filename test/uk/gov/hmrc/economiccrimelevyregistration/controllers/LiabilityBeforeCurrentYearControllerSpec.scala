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
import org.mockito.ArgumentMatchers.any
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.LiabilityBeforeCurrentYearFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, NormalMode, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{LiabilityBeforeCurrentYearPageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.{RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.LiabilityBeforeCurrentYearView
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class LiabilityBeforeCurrentYearControllerSpec extends SpecBase {

  val view: LiabilityBeforeCurrentYearView                 = app.injector.instanceOf[LiabilityBeforeCurrentYearView]
  val formProvider: LiabilityBeforeCurrentYearFormProvider = new LiabilityBeforeCurrentYearFormProvider()
  val form: Form[Boolean]                                  = formProvider()

  val mockService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]

  val mockSessionService: SessionService = mock[SessionService]

  val pageNavigator: LiabilityBeforeCurrentYearPageNavigator = new LiabilityBeforeCurrentYearPageNavigator(mock[AuditConnector]) {
    override def navigateInNormalMode(navigationData: NavigationData): Call =
      onwardRoute

    override def navigateInCheckMode(navigationData: NavigationData): Call =
      onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new LiabilityBeforeCurrentYearController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      formProvider,
      mockService,
      mockSessionService,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = None)) {
        val result: Future[Result] = controller.onPageLoad(true, NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode, true)(fakeRequest, messages).toString
      }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, liableBeforeCurrentYear: Boolean, liabilityYear: LiabilityYear) =>
        new TestContext(registration) {
          val info: RegistrationAdditionalInfo =
            RegistrationAdditionalInfo(
              registration.internalId,
              Some(liabilityYear),
              None
            )

          when(mockService.createOrUpdate(any()))
            .thenReturn(EitherT.fromEither[Future](Right()))

          val result: Future[Result] =
            controller.onSubmit(true, NormalMode)(
              fakeRequest.withFormUrlEncodedBody(("value", liableBeforeCurrentYear.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)

          verify(mockService, times(1)).createOrUpdate(any())(any(), any())

          reset(mockService)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { registration: Registration =>
      new TestContext(registration) {
        val result: Future[Result]        =
          controller.onSubmit(true, NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode, true)(fakeRequest, messages).toString
      }
    }
  }
}
