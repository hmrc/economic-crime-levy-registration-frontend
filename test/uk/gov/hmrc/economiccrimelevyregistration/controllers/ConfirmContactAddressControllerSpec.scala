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
import play.api.mvc.{Call, RequestHeader, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.forms.ConfirmContactAddressFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.Registration
import uk.gov.hmrc.economiccrimelevyregistration.navigation.ConfirmContactAddressPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.InsetTextAddress
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ConfirmContactAddressView

import scala.concurrent.Future

class ConfirmContactAddressControllerSpec extends SpecBase {

  val view: ConfirmContactAddressView                 = app.injector.instanceOf[ConfirmContactAddressView]
  val formProvider: ConfirmContactAddressFormProvider = new ConfirmContactAddressFormProvider()
  val form: Form[Boolean]                             = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  val pageNavigator: ConfirmContactAddressPageNavigator = new ConfirmContactAddressPageNavigator(
    mockEclRegistrationConnector
  ) {
    override protected def navigateInNormalMode(
      registration: Registration
    )(implicit request: RequestHeader): Future[Call]            = Future.successful(onwardRoute)
    override def previousPage(registration: Registration): Call = backRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new ConfirmContactAddressController(
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
    "return OK and the correct view when no answer has already been provided" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile
      ) =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = None,
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        new TestContext(
          updatedRegistration
        ) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form,
            backRoute.url,
            InsetTextAddress(updatedRegistration.grsAddressToEclAddress.get)
          )(fakeRequest, messages).toString
        }
    }

    "throw an IllegalStateException when there is no registered office address in the registration data" in forAll {
      (
        registration: Registration
      ) =>
        val updatedRegistration = registration.copy(
          incorporatedEntityJourneyData = None,
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        new TestContext(
          updatedRegistration
        ) {
          val result: IllegalStateException = intercept[IllegalStateException] {
            await(controller.onPageLoad()(fakeRequest))
          }

          result.getMessage shouldBe "No registered office address found in registration data"
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile,
        useRegisteredOfficeAddressAsContactAddress: Boolean
      ) =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = Some(useRegisteredOfficeAddressAsContactAddress),
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        new TestContext(
          updatedRegistration
        ) {
          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(useRegisteredOfficeAddressAsContactAddress),
            backRoute.url,
            InsetTextAddress(updatedRegistration.grsAddressToEclAddress.get)
          )(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, useRegisteredOfficeAddressAsContactAddress: Boolean) =>
        new TestContext(registration) {
          val updatedRegistration: Registration =
            registration.copy(useRegisteredOfficeAddressAsContactAddress =
              Some(useRegisteredOfficeAddressAsContactAddress)
            )

          when(mockEclRegistrationConnector.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(Future.successful(updatedRegistration))

          val result: Future[Result] =
            controller.onSubmit()(
              fakeRequest.withFormUrlEncodedBody(("value", useRegisteredOfficeAddressAsContactAddress.toString))
            )

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll {
      (
        registration: Registration,
        incorporatedEntityJourneyDataWithValidCompanyProfile: IncorporatedEntityJourneyDataWithValidCompanyProfile
      ) =>
        val updatedRegistration = registration.copy(
          useRegisteredOfficeAddressAsContactAddress = None,
          incorporatedEntityJourneyData =
            Some(incorporatedEntityJourneyDataWithValidCompanyProfile.incorporatedEntityJourneyData),
          partnershipEntityJourneyData = None,
          soleTraderEntityJourneyData = None
        )

        new TestContext(updatedRegistration) {
          val result: Future[Result]        = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(
            formWithErrors,
            backRoute.url,
            InsetTextAddress(updatedRegistration.grsAddressToEclAddress.get)
          )(fakeRequest, messages).toString
        }
    }
  }
}
