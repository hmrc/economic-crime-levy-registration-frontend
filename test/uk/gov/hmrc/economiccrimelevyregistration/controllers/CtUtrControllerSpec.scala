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
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.{OK, SEE_OTHER}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.PublicBetaAction
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, OtherEntityJourneyData, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.CtUtrView
import play.api.mvc.{BodyParsers, Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.CtUtrFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.arbRegistration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.CtUtrPageNavigator

import scala.concurrent.Future

class CtUtrControllerSpec extends SpecBase {

  val view: CtUtrView                                        = app.injector.instanceOf[CtUtrView]
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val errorHandler: ErrorHandler                             = app.injector.instanceOf[ErrorHandler]
  val formProvider: CtUtrFormProvider                        = new CtUtrFormProvider()
  val form: Form[String]                                     = formProvider()
  override val appConfig: AppConfig                          = mock[AppConfig]
  val CTUTR                                                  = "0123456789"

  val pageNavigator: CtUtrPageNavigator = new CtUtrPageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call =
      onwardRoute

    override protected def navigateInCheckMode(registration: Registration): Call =
      onwardRoute
  }

  val enabled: PublicBetaAction = new PublicBetaAction(
    errorHandler = errorHandler,
    appConfig = appConfig,
    parser = app.injector.instanceOf[BodyParsers.Default]
  )

  class TestContext(registrationData: Registration) {
    when(appConfig.privateBetaEnabled).thenReturn(false)
    val controller = new CtUtrController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      enabled,
      formProvider,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has been provided" in { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
      }
    }

    "return OK and the correct view when answer is provided" in { (registration: Registration) =>
      val otherData: OtherEntityJourneyData = registration.otherEntityJourneyData.copy(ctUtr = Some(CTUTR))
      new TestContext(registration.copy(optOtherEntityJourneyData = Some(otherData))) {

        val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString()
      }
    }
  }

  "onSubmit" should {
    "redirect to the next page" in forAll(Arbitrary.arbitrary[Registration]) { (registration: Registration) =>
      new TestContext(registration) {
        val otherData: OtherEntityJourneyData = registration.otherEntityJourneyData.copy(ctUtr = Some(CTUTR))
        val updatedRegistration: Registration = registration.copy(optOtherEntityJourneyData = Some(otherData))
        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", CTUTR)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }
  }
}