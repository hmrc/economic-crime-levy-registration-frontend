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
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmlRegulatedActivityFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{AmlRegulatedActivityPageNavigator, NavigationData}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmlRegulatedActivityView

import scala.concurrent.Future

class AmlRegulatedActivityControllerSpec extends SpecBase {

  val view: AmlRegulatedActivityView                 = app.injector.instanceOf[AmlRegulatedActivityView]
  val formProvider: AmlRegulatedActivityFormProvider = new AmlRegulatedActivityFormProvider()
  val form: Form[Boolean]                            = formProvider()

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockSessionService: SessionService                 = mock[SessionService]

  val pageNavigator: AmlRegulatedActivityPageNavigator = new AmlRegulatedActivityPageNavigator() {
    override protected def navigateInNormalMode(navigationData: Registration): Call =
      onwardRoute

    override protected def navigateInCheckMode(navigationData: Registration): Call =
      onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new AmlRegulatedActivityController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationService,
      mockSessionService,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = None)) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, carriedOutAmlRegulatedActivity: Boolean) =>
        new TestContext(
          registration.copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(carriedOutAmlRegulatedActivity))
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(carriedOutAmlRegulatedActivity), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" should {
      "and store the result in the session service when user has carried out aml activity" in forAll {
        (registration: Registration) =>
          val carriedOutAmlRegulatedActivity = true
          new TestContext(registration) {
            val updatedRegistration: Registration =
              registration.copy(
                carriedOutAmlRegulatedActivityInCurrentFy = Some(carriedOutAmlRegulatedActivity),
                registrationType = Some(Initial)
              )

            when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration)))
              .thenReturn(EitherT.fromEither[Future](Right(updatedRegistration)))

            when(mockSessionService.upsert(any())(any()))
              .thenReturn(EitherT.fromEither[Future](Right()))

            val result: Future[Result] =
              controller.onSubmit(NormalMode)(
                fakeRequest.withFormUrlEncodedBody(("value", carriedOutAmlRegulatedActivity.toString))
              )

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(onwardRoute.url)
          }
      }

      "without storing the result in the session service when user has carried out aml activity" in forAll {
        (registration: Registration) =>
          val carriedOutAmlRegulatedActivity = false
          new TestContext(registration) {
            val updatedRegistration: Registration =
              registration.copy(
                carriedOutAmlRegulatedActivityInCurrentFy = Some(carriedOutAmlRegulatedActivity),
                registrationType = Some(Initial)
              )

            when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration)))
              .thenReturn(EitherT.fromEither[Future](Right(updatedRegistration)))

            val result: Future[Result] =
              controller.onSubmit(NormalMode)(
                fakeRequest.withFormUrlEncodedBody(("value", carriedOutAmlRegulatedActivity.toString))
              )

            status(result) shouldBe SEE_OTHER

            redirectLocation(result) shouldBe Some(onwardRoute.url)
          }
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
