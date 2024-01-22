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
import org.scalacheck.Arbitrary
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.NonUkCrnFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.CompanyRegistrationNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models._
import uk.gov.hmrc.economiccrimelevyregistration.navigation.{NavigationData, NonUkCrnPageNavigator}
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.NonUkCrnView

import scala.concurrent.Future

class NonUkCrnControllerSpec extends SpecBase {

  val view: NonUkCrnView                 = app.injector.instanceOf[NonUkCrnView]
  val formProvider: NonUkCrnFormProvider = new NonUkCrnFormProvider()
  val form: Form[String]                 = formProvider()

  val pageNavigator: NonUkCrnPageNavigator = new NonUkCrnPageNavigator(
  ) {
    override protected def navigateInNormalMode(
      navigationData: NavigationData
    ): Call = onwardRoute

    override protected def navigateInCheckMode(
      navigationData: NavigationData
    ): Call = onwardRoute
  }

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  override val appConfig: AppConfig                      = mock[AppConfig]

  class TestContext(registrationData: Registration) {
    val controller = new NonUkCrnController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeDataRetrievalAction(registrationData),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
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
      (registration: Registration, companyNumber: String, mode: Mode) =>
        val otherEntityJourneyData =
          OtherEntityJourneyData.empty().copy(companyRegistrationNumber = Some(companyNumber))
        new TestContext(registration.copy(optOtherEntityJourneyData = Some(otherEntityJourneyData))) {

          val result: Future[Result] = controller.onPageLoad(mode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form.fill(companyNumber), mode)(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the company number then redirect to the next page" in forAll(
      Arbitrary.arbitrary[Registration],
      stringsWithMaxLength(CompanyRegistrationNumberMaxLength),
      Arbitrary.arbitrary[Mode]
    ) { (registration: Registration, companyNumber: String, mode: Mode) =>
      val otherEntityJourneyData =
        registration.otherEntityJourneyData.copy(companyRegistrationNumber = Some(companyNumber))
      new TestContext(registration) {
        val updatedRegistration: Registration = registration.copy(
          optOtherEntityJourneyData = Some(otherEntityJourneyData)
        )

        when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration)))
          .thenReturn(EitherT.fromEither[Future](Right(updatedRegistration)))

        val result: Future[Result] =
          controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", companyNumber)))

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(onwardRoute.url)
      }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (registration: Registration, mode: Mode) =>
        new TestContext(registration) {
          val result: Future[Result]       = controller.onSubmit(mode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[String] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(formWithErrors, mode)(fakeRequest, messages).toString
        }
    }
  }
}
