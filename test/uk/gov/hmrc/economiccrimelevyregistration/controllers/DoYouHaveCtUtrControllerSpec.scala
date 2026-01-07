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
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.{Call, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.DoYouHaveCtUtrFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EclRegistrationModel, NormalMode, OtherEntityJourneyData, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.navigation.DoYouHaveCtUtrPageNavigator
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.DoYouHaveCtUtrView
import org.mockito.Mockito.when

import scala.concurrent.Future

class DoYouHaveCtUtrControllerSpec extends SpecBase {

  val view: DoYouHaveCtUtrView                 = app.injector.instanceOf[DoYouHaveCtUtrView]
  val formProvider: DoYouHaveCtUtrFormProvider = new DoYouHaveCtUtrFormProvider()
  val form: Form[Boolean]                      = formProvider()

  val mockEclRegistrationService: EclRegistrationService           = mock[EclRegistrationService]
  val mockAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]

  val pageNavigator: DoYouHaveCtUtrPageNavigator = new DoYouHaveCtUtrPageNavigator() {
    override protected def navigateInNormalMode(eclRegistrationModel: EclRegistrationModel): Call =
      onwardRoute

    override protected def navigateInCheckMode(eclRegistrationModel: EclRegistrationModel): Call =
      onwardRoute
  }

  class TestContext(registrationData: Registration) {
    val controller = new DoYouHaveCtUtrController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(registrationData.internalId),
      fakeRegistrationDataAction(registrationData),
      fakeStoreUrlAction(),
      formProvider,
      mockEclRegistrationService,
      pageNavigator,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view when no answer has already been provided" in forAll {
      (registration: Registration) =>
        new TestContext(registration.copy(optOtherEntityJourneyData = None)) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
        }
    }

    "populate the view correctly when the question has previously been answered" in forAll {
      (registration: Registration, hasUtr: Boolean) =>
        val otherEntityJourneyData: OtherEntityJourneyData = OtherEntityJourneyData
          .empty()
          .copy(
            isCtUtrPresent = Some(hasUtr)
          )
        val updatedRegistration: Registration              =
          registration.copy(
            optOtherEntityJourneyData = Some(otherEntityJourneyData)
          )

        new TestContext(updatedRegistration) {
          val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

          status(result)          shouldBe OK
          contentAsString(result) shouldBe view(form.fill(hasUtr), NormalMode)(
            fakeRequest,
            messages
          ).toString
        }
    }
  }

  "onSubmit" should {
    "save the selected answer then redirect to the next page in Normal Mode" in forAll {
      (registration: Registration, hasUtr: Boolean, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration) {

          when(mockEclRegistrationService.getOrCreate(ArgumentMatchers.eq(testInternalId))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Registration](Future.successful(Right(registration))))
          when(
            mockAdditionalInfoService.getOrCreate(
              ArgumentMatchers.eq(testInternalId),
              ArgumentMatchers.eq(Some(testEclRegistrationReference))
            )(any())
          )
            .thenReturn(
              EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo](Future.successful(Right(additionalInfo)))
            )

          val answerChanged: (Option[String], Option[String]) = if (hasUtr) {
            (registration.otherEntityJourneyData.postcode, registration.otherEntityJourneyData.ctUtr)
          } else {
            (None, None)
          }
          val updatedRegistration: Registration               = registration.copy(optOtherEntityJourneyData =
            Some(
              registration.otherEntityJourneyData
                .copy(isCtUtrPresent = Some(hasUtr), postcode = answerChanged._1, ctUtr = answerChanged._2)
            )
          )

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", hasUtr.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "save the selected answer then redirect to the next page in CheckMode" in forAll {
      (registration: Registration, hasUtr: Boolean, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration) {

          when(mockEclRegistrationService.getOrCreate(ArgumentMatchers.eq(testInternalId))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Registration](Future.successful(Right(registration))))
          when(
            mockAdditionalInfoService.getOrCreate(
              ArgumentMatchers.eq(testInternalId),
              ArgumentMatchers.eq(Some(testEclRegistrationReference))
            )(any())
          )
            .thenReturn(
              EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo](Future.successful(Right(additionalInfo)))
            )

          val answerChanged: (Option[String], Option[String]) = if (hasUtr) {
            (registration.otherEntityJourneyData.postcode, registration.otherEntityJourneyData.ctUtr)
          } else {
            (None, None)
          }
          val updatedRegistration: Registration               = registration.copy(optOtherEntityJourneyData =
            Some(
              registration.otherEntityJourneyData
                .copy(isCtUtrPresent = Some(hasUtr), postcode = answerChanged._1, ctUtr = answerChanged._2)
            )
          )

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", hasUtr.toString)))

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(onwardRoute.url)
        }
    }

    "return an error if the call to upsert the registration fails in CheckMode" in forAll {
      (registration: Registration, hasUtr: Boolean, additionalInfo: RegistrationAdditionalInfo) =>
        new TestContext(registration) {

          when(mockEclRegistrationService.getOrCreate(anyString())(any()))
            .thenReturn(EitherT[Future, DataRetrievalError, Registration](Future.successful(Right(registration))))
          when(
            mockAdditionalInfoService.getOrCreate(anyString(), ArgumentMatchers.eq(Some(testEclRegistrationReference)))(
              any()
            )
          )
            .thenReturn(
              EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo](Future.successful(Right(additionalInfo)))
            )

          val answerChanged: (Option[String], Option[String]) = if (hasUtr) {
            (registration.otherEntityJourneyData.postcode, registration.otherEntityJourneyData.ctUtr)
          } else {
            (None, None)
          }
          val updatedRegistration: Registration               = registration.copy(optOtherEntityJourneyData =
            Some(
              registration.otherEntityJourneyData
                .copy(isCtUtrPresent = Some(hasUtr), postcode = answerChanged._1, ctUtr = answerChanged._2)
            )
          )

          when(mockEclRegistrationService.upsertRegistration(ArgumentMatchers.eq(updatedRegistration))(any()))
            .thenReturn(
              EitherT[Future, DataRetrievalError, Unit](
                Future.successful(
                  Left(DataRetrievalError.InternalUnexpectedError("Unable to upsert registration.", None))
                )
              )
            )

          val result: Future[Result] =
            controller.onSubmit(CheckMode)(fakeRequest.withFormUrlEncodedBody(("value", hasUtr.toString)))

          status(result) shouldBe INTERNAL_SERVER_ERROR

        }
    }

    "return a Bad Request with form errors when invalid data is submitted" in forAll { (registration: Registration) =>
      new TestContext(registration) {
        val result: Future[Result]        = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody(("value", "")))
        val formWithErrors: Form[Boolean] = form.bind(Map("value" -> ""))

        status(result) shouldBe BAD_REQUEST

        contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
      }
    }
  }
}
