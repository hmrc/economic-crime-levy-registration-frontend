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
import uk.gov.hmrc.economiccrimelevyregistration.forms.BusinessSectorFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{BusinessSector, EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.BusinessSectorPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.BusinessSectorView
import org.mockito.Mockito.when
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

import scala.concurrent.Future

class BusinessSectorControllerSpec extends SpecBase {

  val view: BusinessSectorView                           = app.injector.instanceOf[BusinessSectorView]
  val formProvider: BusinessSectorFormProvider           = new BusinessSectorFormProvider()
  val form: Form[BusinessSector]                         = formProvider()
  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val pageNavigator: BusinessSectorPageNavigator = new BusinessSectorPageNavigator() {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
      onwardRoute

    override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
      onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new BusinessSectorController(
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
      (registration: Registration) =>
        new TestContext(registration.copy(businessSector = None, registrationType = Some(Initial))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, None, NormalMode, None)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, businessSector: BusinessSector) =>
        new TestContext(registration.copy(businessSector = Some(businessSector), registrationType = Some(Initial))) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(businessSector), None, NormalMode, None)(
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

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", businessSector.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { (registration: Registration) =>
      val updatedRegistration = registration.copy(registrationType = Some(Initial))
      new TestContext(updatedRegistration) {
        val result: Future[Result]               = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[BusinessSector] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, None, NormalMode, None)(fakeRequest, messages).toString
      }
    }
  }
}
