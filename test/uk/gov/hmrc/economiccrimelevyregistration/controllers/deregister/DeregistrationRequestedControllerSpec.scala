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
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.{DataRetrievalError, SessionError}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.deregister.DeregistrationService
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService, SessionService}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.deregister.DeregistrationRequestedView
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{OutOfSessionRegistrationSubmittedView, RegistrationSubmittedView}

import scala.concurrent.Future

class DeregistrationRequestedControllerSpec extends SpecBase {

  val view: DeregistrationRequestedView                  = app.injector.instanceOf[DeregistrationRequestedView]
  val mockSessionService: SessionService                 = mock[SessionService]
  val mockDeregistrationService: DeregistrationService   = mock[DeregistrationService]
  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val controller = new DeregistrationRequestedController(
    mcc,
    fakeAuthorisedActionWithoutEnrolmentCheck("test-internal-id"),
    new DeregistrationDataRetrievalAction(mockDeregistrationService),
    mockDeregistrationService,
    mockEclRegistrationService,
    view
  )

  "onPageLoad" should {
    "return OK and the correct view when there is one contact email address and aml activity in the session" in forAll {
      (
        liabilityYear: LiabilityYear,
        firstContactEmailAddress: String,
        secondContactEmailAddress: Option[String],
        amlRegulatedActivity: Boolean
      ) =>
        when(mockRegistrationAdditionalInfoService.delete(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        when(mockEclRegistrationService.deleteRegistration(anyString())(any(), any()))
          .thenReturn(EitherT[Future, DataRetrievalError, Unit](Future.successful(Right(()))))

        val request = fakeRequest.withSession(
          (SessionKeys.EclReference, eclReference),
          (SessionKeys.FirstContactEmailAddress, firstContactEmailAddress),
          (SessionKeys.AmlRegulatedActivity, amlRegulatedActivity.toString)
        )

        when(
          mockSessionService.get(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.LiabilityYear)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(liabilityYear.value.toString))))
        when(
          mockSessionService.get(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.AmlRegulatedActivity)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(amlRegulatedActivity.toString))))
        when(
          mockSessionService.get(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.FirstContactEmailAddress)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(firstContactEmailAddress))))
        when(
          mockSessionService.getOptional(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.SecondContactEmailAddress)
          )(any())
        )
          .thenReturn(
            EitherT[Future, SessionError, Option[String]](Future.successful(Right(secondContactEmailAddress)))
          )
        when(
          mockSessionService.getOptional(
            ArgumentMatchers.eq(request.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.EclReference)
          )(any())
        )
          .thenReturn(EitherT[Future, SessionError, Option[String]](Future.successful(Right(Some(eclReference)))))

        val result: Future[Result] = controller.onPageLoad()(
          request
        )

        status(result) shouldBe OK

        contentAsString(result) shouldBe view(
          eclReference,
          firstContactEmailAddress,
          secondContactEmailAddress,
          Some(liabilityYear),
          Some(amlRegulatedActivity.toString)
        )(fakeRequest, messages).toString
    }

    "return OK and the correct view when information is not gathered from session" in {
      (
        internalId: String,
        groupId: String,
        eclReference: String,
        liabilityYear: LiabilityYear,
        amlRegulatedActivity: Option[String]
      ) =>
        when(
          mockSessionService.get(
            ArgumentMatchers.eq(fakeRequest.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.LiabilityYear)
          )(any())
        )
          .thenReturn(EitherT.fromEither[Future](Right(liabilityYear.asString)))

        when(
          mockSessionService.get(
            ArgumentMatchers.eq(fakeRequest.session),
            anyString(),
            ArgumentMatchers.eq(SessionKeys.AmlRegulatedActivity)
          )(
            any()
          )
        )
          .thenReturn(EitherT[Future, SessionError, String](Future.successful(Right(amlRegulatedActivity.toString))))

        val result =
          controller.onPageLoad()(AuthorisedRequest(fakeRequest, internalId, groupId, Some(eclReference)))

        status(result) shouldBe OK

        contentAsString(result) shouldBe outOfSessionRegistrationSubmittedView(
          "eclReference",
          Some(liabilityYear),
          amlRegulatedActivity
        )(fakeRequest, messages)
          .toString()
    }

  }

}
