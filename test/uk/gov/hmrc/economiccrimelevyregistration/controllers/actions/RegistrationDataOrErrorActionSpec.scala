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

package uk.gov.hmrc.economiccrimelevyregistration.controllers.actions

import cats.data.EitherT
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Amendment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.DataRetrievalError
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.{AuthorisedRequest, RegistrationDataRequest}
import uk.gov.hmrc.economiccrimelevyregistration.models.{LiabilityYear, Registration, RegistrationAdditionalInfo}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, RegistrationAdditionalInfoService}

import scala.concurrent.Future

class RegistrationDataOrErrorActionSpec extends SpecBase {

  val mockEclRegistrationService: EclRegistrationService                       = mock[EclRegistrationService]
  val mockRegistrationAdditionalInfoService: RegistrationAdditionalInfoService = mock[RegistrationAdditionalInfoService]

  class TestRegistrationDataAction
      extends RegistrationDataOrErrorActionImpl(
        mockEclRegistrationService,
        mockRegistrationAdditionalInfoService
      ) {
    override def refine[A](request: AuthorisedRequest[A]): Future[Either[Result, RegistrationDataRequest[A]]] =
      super.refine(request)
  }

  val RegistrationDataAction =
    new TestRegistrationDataAction

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  private def getRedirectUrl(request: AuthorisedRequest[_]) =
    request.uri match {
      case amendUrl if amendUrl == routes.CheckYourAnswersController.onPageLoad(Amendment).url =>
        routes.NotableErrorController.youAlreadyRequestedToAmend()
      case _                                                                                   => routes.NotableErrorController.youHaveAlreadyRegistered()
    }

  "refine" should {
    "return internal server error if additional info cannot be retrieved" in forAll {
      (internalId: String, groupId: String, registration: Registration) =>
        when(mockEclRegistrationService.get(ArgumentMatchers.eq(internalId))(any(), any()))
          .thenReturn(
            EitherT[Future, DataRetrievalError, Option[Registration]](Future.successful(Right(Some(registration))))
          )

        val error = "Internal server error"
        when(mockRegistrationAdditionalInfoService.getOrCreate(ArgumentMatchers.eq(internalId), any())(any()))
          .thenReturn(
            EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo](
              Future.successful(Left(DataRetrievalError.InternalUnexpectedError(error, None)))
            )
          )

        val result = intercept[Exception] {
          await(
            RegistrationDataAction.refine(
              AuthorisedRequest(fakeRequest, internalId, groupId, Some("ECLRefNumber12345"))
            )
          )
        }

        result.getMessage shouldBe error
    }

    "transform an AuthorisedRequest into a RegistrationDataRequest when data retrieval succeeds" in forAll {
      (internalId: String, groupId: String, registration: Registration, liabilityYear: LiabilityYear) =>
        when(mockEclRegistrationService.get(any())(any(), any()))
          .thenReturn(
            EitherT[Future, DataRetrievalError, Option[Registration]](Future.successful(Right(Some(registration))))
          )

        val info = RegistrationAdditionalInfo(
          registration.internalId,
          Some(liabilityYear),
          None,
          None,
          None,
          None
        )
        when(mockRegistrationAdditionalInfoService.getOrCreate(ArgumentMatchers.eq(internalId), any())(any()))
          .thenReturn(EitherT[Future, DataRetrievalError, RegistrationAdditionalInfo](Future.successful(Right(info))))

        val result: Future[Either[Result, RegistrationDataRequest[AnyContentAsEmpty.type]]] =
          RegistrationDataAction.refine(AuthorisedRequest(fakeRequest, internalId, groupId, Some("ECLRefNumber12345")))

        await(result) shouldBe Right(
          RegistrationDataRequest(fakeRequest, internalId, registration, Some(info), Some("ECLRefNumber12345"))
        )
    }

    "redirect to error page when data retrieval fails" in forAll { (internalId: String, groupId: String) =>
      when(mockEclRegistrationService.get(any())(any(), any()))
        .thenReturn(EitherT[Future, DataRetrievalError, Option[Registration]](Future.successful(Right(None))))

      val request                                                                         = AuthorisedRequest(fakeRequest, internalId, groupId, Some("ECLRefNumber12345"))
      val result: Future[Either[Result, RegistrationDataRequest[AnyContentAsEmpty.type]]] =
        RegistrationDataAction.refine(request)

      await(result) shouldBe Left(Redirect(getRedirectUrl(request)))
    }
  }

}
