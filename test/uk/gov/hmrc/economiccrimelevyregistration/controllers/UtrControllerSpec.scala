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
import play.api.data.Form
import play.api.http.Status.SEE_OTHER
import play.api.mvc.{Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.RegistrationWithUnincorporatedAssociation
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.UtrFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{NavigationData, UtrPageNavigator}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.UtrView

import scala.concurrent.Future

class UtrControllerSpec extends SpecBase {

  val view: UtrView                 = app.injector.instanceOf[UtrView]
  val formProvider: UtrFormProvider = new UtrFormProvider()
  val form: Form[String]            = formProvider()
  override val appConfig: AppConfig = mock[AppConfig]

  val pageNavigator: UtrPageNavigator = new UtrPageNavigator() {
    override protected def navigateInNormalMode(navigationData: NavigationData): Call = onwardRoute

    override protected def navigateInCheckMode(navigationData: NavigationData): Call = onwardRoute
  }

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  class TestContext(registration: Registration) {
    val controller = new UtrController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registration.internalId),
      fakeDataRetrievalAction(registration),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in {
      (registration: RegistrationWithUnincorporatedAssociation) =>
        val otherEntityData = registration.registration.otherEntityJourneyData.copy(ctUtr = None)
        new TestContext(
          registration = registration.registration.copy(optOtherEntityJourneyData = Some(otherEntityData))
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe Ok

          contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString()
        }
    }

    "return OK and the correct view when answer has already been provided" in {
      (registration: RegistrationWithUnincorporatedAssociation) =>
        new TestContext(
          registration = registration.registration
        ) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe Ok

          contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString()
        }
    }
  }

  "onSubmit" should {
    "redirect to the next page" in { (registration: RegistrationWithUnincorporatedAssociation) =>
      new TestContext(registration.registration) {
        val updatedRegistration: Registration = registration.registration

        when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration)))
          .thenReturn(EitherT.fromEither[Future](Right(updatedRegistration)))

        val utr: String                       = registration.registration.otherEntityJourneyData.ctUtr.getOrElse("")

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", utr)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }
  }
}
