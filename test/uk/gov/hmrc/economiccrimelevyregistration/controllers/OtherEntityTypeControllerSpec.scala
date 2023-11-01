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
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{BodyParsers, Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.ValidRegistrationWithDifferentEntityTypes
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.OtherEntityTypeDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.OtherEntityTypeAction
import uk.gov.hmrc.economiccrimelevyregistration.forms.OtherEntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.OtherEntityType.{Charity, NonUKEstablishment}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.OtherEntityTypePageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.views.html.OtherEntityTypeView
import uk.gov.hmrc.http.HttpVerbs.GET

import scala.concurrent.Future

class OtherEntityTypeControllerSpec extends SpecBase {

  val view: OtherEntityTypeView                 = app.injector.instanceOf[OtherEntityTypeView]
  val formProvider: OtherEntityTypeFormProvider = new OtherEntityTypeFormProvider()
  val form: Form[OtherEntityType]               = formProvider()

  val checkYourAnswersOnwardRoute: Call = Call(GET, "/register-for-economic-crime-levy/other-entity-check-your-answers")

  val pageNavigator: OtherEntityTypePageNavigator = new OtherEntityTypePageNavigator(
  ) {
    override protected def navigateInNormalMode(
      registration: Registration
    ): Call = onwardRoute

    override protected def navigateInCheckMode(
      registration: Registration
    ): Call = onwardRoute
  }

  val dataCleanup: OtherEntityTypeDataCleanup = new OtherEntityTypeDataCleanup {
    override def cleanup(registration: Registration): Registration = registration
  }

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  override val appConfig: AppConfig                          = mock[AppConfig]
  when(appConfig.otherEntityTypeEnabled).thenReturn(true)

  val otherEntityTypeAction: OtherEntityTypeAction = new OtherEntityTypeAction(
    errorHandler = errorHandler,
    appConfig = appConfig,
    parser = app.injector.instanceOf[BodyParsers.Default]
  )

  class TestContext(registrationData: Registration) {
    val controller = new OtherEntityTypeController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationConnector,
      formProvider,
      pageNavigator,
      dataCleanup,
      otherEntityTypeAction,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration.copy(optOtherEntityJourneyData = Some(OtherEntityJourneyData.empty()))) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, mode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, entityType: OtherEntityType, mode: Mode) =>
        val otherEntityJourneyData = OtherEntityJourneyData.empty().copy(entityType = Some(entityType))
        new TestContext(registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))) {
          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(entityType), mode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected entity type then redirect to the next page" in forAll {
      (registration: ValidRegistrationWithDifferentEntityTypes, mode: Mode) =>
        new TestContext(registration.registration) {

          when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
            .thenReturn(Future.successful(registration.registration))

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", Charity.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "save the selected entity type then redirect to the check your answers page" in forAll {
      (registration: ValidRegistrationWithDifferentEntityTypes, mode: Mode) =>
        new TestContext(registration.registration) {

          when(mockEclRegistrationConnector.upsertRegistration(any())(any()))
            .thenReturn(Future.successful(registration.registration))

          val result: Future[Result] =
            controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", NonUKEstablishment.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(checkYourAnswersOnwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration) {
          val result: Future[Result]                = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[OtherEntityType] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }
  }
}
