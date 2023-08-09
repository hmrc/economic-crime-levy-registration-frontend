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
import play.api.mvc.{BodyParsers, Call, Result}
import play.api.test.Helpers.{contentAsString, redirectLocation, status}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.OtherEntityTypeAction
import uk.gov.hmrc.economiccrimelevyregistration.forms.SaUtrFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.arbRegistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, OtherEntityJourneyData, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.SaUtrPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.SaUtrView

import scala.concurrent.Future

class SaUtrControllerSpec extends SpecBase {

  val view: SaUtrView                                        = app.injector.instanceOf[SaUtrView]
  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  val formProvider: SaUtrFormProvider                        = new SaUtrFormProvider()
  val form: Form[String]                                     = formProvider()
  override val appConfig: AppConfig                          = mock[AppConfig]
  val SAUTR                                                  = "0123456789"
  val otherEntityTypeAction: OtherEntityTypeAction           = new OtherEntityTypeAction(
    errorHandler = errorHandler,
    appConfig = appConfig,
    parser = app.injector.instanceOf[BodyParsers.Default]
  )

  val pageNavigator: SaUtrPageNavigator = new SaUtrPageNavigator() {
    override protected def navigateInNormalMode(registration: Registration): Call =
      onwardRoute

    override protected def navigateInCheckMode(registration: Registration): Call =
      onwardRoute
  }

  class TestContext(registrationData: Registration) {
    when(appConfig.otherEntityTypeEnabled).thenReturn(true)
    val controller = new SaUtrController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      otherEntityTypeAction,
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
      val otherData: OtherEntityJourneyData = registration.otherEntityJourneyData.copy(saUtr = Some(SAUTR))
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
        val otherData: OtherEntityJourneyData = registration.otherEntityJourneyData.copy(
          saUtr = Some(SAUTR),
          ctUtr = None
        )
        val updatedRegistration: Registration = registration.copy(optOtherEntityJourneyData = Some(otherData))
        when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
          .thenReturn(Future.successful(updatedRegistration))

        val result: Future[Result] =
          controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", SAUTR)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }
  }
}
