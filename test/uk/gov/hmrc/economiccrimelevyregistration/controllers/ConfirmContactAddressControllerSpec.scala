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
import uk.gov.hmrc.economiccrimelevyregistration.IncorporatedEntityJourneyDataWithValidCompanyProfile
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.cleanup.ConfirmContactAddressDataCleanup
import uk.gov.hmrc.economiccrimelevyregistration.forms.ConfirmContactAddressFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{EclRegistrationModel, NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.ConfirmContactAddressPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.EclRegistrationService
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.AddressViewModel
import uk.gov.hmrc.economiccrimelevyregistration.views.html.ConfirmContactAddressView

import scala.concurrent.Future

class ConfirmContactAddressControllerSpec extends SpecBase {

  val view: ConfirmContactAddressView                 = app.injector.instanceOf[ConfirmContactAddressView]
  val formProvider: ConfirmContactAddressFormProvider = new ConfirmContactAddressFormProvider()
  val form: Form[Boolean]                             = formProvider()

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val pageNavigator: ConfirmContactAddressPageNavigator = new ConfirmContactAddressPageNavigator() {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call = onwardRoute
  }

  val dataCleanup: ConfirmContactAddressDataCleanup = new ConfirmContactAddressDataCleanup {
    override def cleanup(registration: Registration): Registration = registration
  }

  class TestContext(registrationData: Registration) {
    val controller = new ConfirmContactAddressController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      mockEclRegistrationService,
      formProvider,
      pageNavigator,
      dataCleanup,
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
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form,
            AddressViewModel.insetText(updatedRegistration.grsAddressToEclAddress.get),
            NormalMode
          )(fakeRequest, messages).toString
        }
    }

    "return a DataRetrievalError when there is no registered office address in the registration data" in forAll {
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
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe INTERNAL_SERVER_ERROR

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
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            form.fill(useRegisteredOfficeAddressAsContactAddress),
            AddressViewModel.insetText(updatedRegistration.grsAddressToEclAddress.get),
            NormalMode
          )(fakeRequest, messages).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page" in forAll {
      (registration: Registration, useRegisteredOfficeAddressAsContactAddress: Boolean) =>
        new TestContext(registration) {
          val updatedRegistration: Registration   =
            registration.copy(useRegisteredOfficeAddressAsContactAddress =
              Some(useRegisteredOfficeAddressAsContactAddress)
            )
          val cleanedUpRegistration: Registration = dataCleanup.cleanup(updatedRegistration)
          val modifiedRegistration: Registration  =
            if (useRegisteredOfficeAddressAsContactAddress) {
              cleanedUpRegistration.copy(
                contactAddress = cleanedUpRegistration.grsAddressToEclAddress
              )
            } else {
              cleanedUpRegistration
            }

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(modifiedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(
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
          val result: Future[Result]        =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
          val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

          status(result) shouldBe BAD_REQUEST

          contentAsString(result) shouldBe view(
            formWithErrors,
            AddressViewModel.insetText(updatedRegistration.grsAddressToEclAddress.get),
            NormalMode
          )(fakeRequest, messages).toString
        }
    }
  }
}
