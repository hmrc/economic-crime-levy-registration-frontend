/*
 * Copyright 2024 HM Revenue & Customs
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
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.AmendReasonFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.AmendReasonPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.AmendReasonView

import scala.concurrent.Future

class AmendReasonControllerSpec extends SpecBase {

  val view: AmendReasonView                           = app.injector.instanceOf[AmendReasonView]
  val formProvider: AmendReasonFormProvider           = new AmendReasonFormProvider()
  val form: Form[String]                              = formProvider()
  val mockRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val retrievedEclRefFromAction                       = "ECLRefNumber12345"
  val pageNavigator: AmendReasonPageNavigator         = new AmendReasonPageNavigator() {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute
  }

  class TestContext(registration: Registration) {
    val controller = new AmendReasonController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registration.internalId),
      fakeDataRetrievalAction(registration),
      view,
      formProvider,
      mockRegistrationService,
      pageNavigator
    )
  }

  "onPageLoad" should {
    "return OK and correct view when no answer has already been provided" in forAll { registration: Registration =>
      new TestContext(registration.copy(amendReason = None, registrationType = Some(Amendment))) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode, Some(Amendment), Some(retrievedEclRefFromAction))(
          fakeRequest,
          messages
        ).toString
      }
    }

    "return OK and correct view when answer has already been provided" in forAll {
      (registration: Registration, reason: String) =>
        new TestContext(registration.copy(amendReason = Some(reason), registrationType = Some(Amendment))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(reason),
            NormalMode,
            Some(Amendment),
            Some(retrievedEclRefFromAction)
          )(fakeRequest, messages).toString()
        }
    }
  }

  "onSubmit" should {
    "save the provided amendment reason then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      nonEmptyString
    ) { (registration: Registration, reason: String) =>
      val updatedRegistration = registration.copy(amendReason = Some(reason), registrationType = Some(Amendment))
      new TestContext(updatedRegistration) {

        when(mockRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(updatedRegistration))))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> reason))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)

        verify(mockRegistrationService, times(1)).upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any())

        reset(mockRegistrationService)
      }
    }
    "return a Bad Request with form errors when user has provided wrong input" in forAll { registration: Registration =>
      new TestContext(registration.copy(registrationType = Some(Amendment))) {

        val result: Future[Result]       = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> ""))
        val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(
          formWithErrors,
          NormalMode,
          Some(Amendment),
          Some(retrievedEclRefFromAction)
        )(fakeRequest, messages).toString()
      }

    }
  }
}
