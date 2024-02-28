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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.{any, anyString}
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.deregister.DeregistrationDataRetrievalAction
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.{arbDeregistration, arbGetSubscriptionResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, GetSubscriptionResponse}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EmailService}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregisterCheckYourAnswersView

import scala.concurrent.Future

class DeregisterCheckYourAnswersControllerSpec extends SpecBase {

  val view: DeregisterCheckYourAnswersView = app.injector.instanceOf[DeregisterCheckYourAnswersView]

  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockDeregistrationService: DeregistrationService   = mock[DeregistrationService]
  val mockEmailService: EmailService                     = mock[EmailService]

  class TestContext(internalId: String, eclReference: String) {
    val controller = new DeregisterCheckYourAnswersController(
      mcc,
      fakeAuthorisedActionWithEnrolmentCheck(internalId, Some(eclReference)),
      new DeregistrationDataRetrievalAction(mockDeregistrationService),
      mockEclRegistrationService,
      mockDeregistrationService,
      mockEmailService,
      view
    )
  }

  "onPageLoad" should {
    "return OK and the correct view" in forAll {
      (subscription: GetSubscriptionResponse, deregistration: Deregistration, eclReference: String) =>
        new TestContext(deregistration.internalId, eclReference) {
          when(mockDeregistrationService.getOrCreate(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(deregistration)))

          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))

          when(mockDeregistrationService.upsert(any())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(deregistration.copy(eclReference = Some(eclReference)))))

          val result: Future[Result] = controller.onPageLoad()(fakeRequest)

          status(result) shouldBe OK

          contentAsString(result) shouldBe view(
            controller.organisation(Some(eclReference), subscription.legalEntityDetails.organisationName)(messages),
            controller.additionalInfo(deregistration)(messages),
            controller.contact(deregistration.contactDetails)(messages),
            deregistration.registrationType
          )(fakeRequest, messages).toString

          verify(mockDeregistrationService, times(1)).upsert(any())(any())
          reset(mockDeregistrationService)
        }
    }
  }

  "onSubmit" should {
    "go to the deregistration requested page" in forAll {
      (
        deregistration: Deregistration,
        subscription: GetSubscriptionResponse,
        email: String,
        name: String,
        eclReference: String
      ) =>
        new TestContext(deregistration.internalId, "") {
          val validDeregistration =
            deregistration.copy(contactDetails = ContactDetails(Some(name), None, Some(email), None))

          when(mockDeregistrationService.getOrCreate(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(validDeregistration)))
          when(mockEclRegistrationService.getSubscription(anyString())(any()))
            .thenReturn(EitherT.fromEither[Future](Right(subscription)))
          when(
            mockEmailService.sendDeregistrationEmail(
              ArgumentMatchers.eq(email),
              ArgumentMatchers.eq(name),
              ArgumentMatchers.eq(eclReference),
              ArgumentMatchers.eq(subscription.correspondenceAddressDetails)
            )(any(), any())
          )
            .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

          val result: Future[Result] =
            controller.onSubmit()(fakeRequest)

          status(result) shouldBe SEE_OTHER

          redirectLocation(result) shouldBe Some(routes.DeregistrationRequestedController.onPageLoad().url)
        }
    }
  }
}
