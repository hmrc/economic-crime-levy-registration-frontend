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
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Organisation}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.models.errors.EnrolmentStoreProxyError
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyregistration.{EnrolmentsWithEcl, ValidRegistrationWithRegistrationType}

import scala.concurrent.Future

class AuthorisedActionWithoutEnrolmentCheckSpec extends SpecBase {

  val defaultBodyParser: BodyParsers.Default                     = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector                           = mock[AuthConnector]
  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockEclRegistrationService: EclRegistrationService         = mock[EclRegistrationService]

  val authorisedAction =
    new AuthorisedActionWithoutEnrolmentCheckImpl(
      mockAuthConnector,
      mockEclRegistrationService,
      mockEnrolmentStoreProxyService,
      appConfig,
      defaultBodyParser
    )

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  val eclEnrolmentKey: String = EclEnrolment.serviceName

  val expectedRetrievals
    : Retrieval[Option[String] ~ Enrolments ~ Option[String] ~ Option[AffinityGroup] ~ Option[CredentialRole]] =
    Retrievals.internalId and Retrievals.allEnrolments and Retrievals.groupIdentifier and Retrievals.affinityGroup and Retrievals.credentialRole

  "invokeBlock" should {
    "execute the block and return the result if authorised" in forAll {
      (
        internalId: String,
        enrolmentsWithEcl: EnrolmentsWithEcl,
        groupId: String,
        validRegistration: ValidRegistrationWithRegistrationType
      ) =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(
            Future(
              Some(internalId) and enrolmentsWithEcl.enrolments and Some(groupId) and Some(Organisation) and Some(
                User
              )
            )
          )

        when(mockEclRegistrationService.getOrCreate(ArgumentMatchers.eq(internalId))(any()))
          .thenReturn(EitherT.fromEither[Future](Right(validRegistration.registration)))

        when(mockEnrolmentStoreProxyService.getEclReferenceFromGroupEnrolment(ArgumentMatchers.eq(groupId))(any()))
          .thenReturn(EitherT.fromEither[Future](Left(EnrolmentStoreProxyError.InternalUnexpectedError("", None))))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Test"
    }

    "redirect the user to sign in when there is no active session" in {
      List(BearerTokenExpired(), MissingBearerToken(), InvalidBearerToken(), SessionRecordNotFound()).foreach {
        exception =>
          when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

          val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

          status(result)               shouldBe SEE_OTHER
          redirectLocation(result).value should startWith(appConfig.signInUrl)
      }
    }

    "redirect the user to the agent not supported page if they have an agent affinity group" in forAll {
      (internalId: String, enrolmentsWithEcl: EnrolmentsWithEcl, groupId: String) =>
        when(
          mockAuthConnector
            .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
        )
          .thenReturn(
            Future(
              Some(internalId) and enrolmentsWithEcl.enrolments and Some(groupId) and Some(Agent) and Some(User)
            )
          )

        when(mockEnrolmentStoreProxyService.getEclReferenceFromGroupEnrolment(ArgumentMatchers.eq(groupId))(any()))
          .thenReturn(EitherT.fromEither[Future](Left(EnrolmentStoreProxyError.InternalUnexpectedError("", None))))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.agentCannotRegister().url
    }

    "redirect the user to the assistant not supported page if they have an assistant credential role" in forAll {
      (internalId: String, enrolmentsWithEcl: EnrolmentsWithEcl, groupId: String) =>
        when(
          mockAuthConnector
            .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
        )
          .thenReturn(
            Future(
              Some(internalId) and enrolmentsWithEcl.enrolments and Some(groupId) and Some(Organisation) and Some(
                Assistant
              )
            )
          )

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.NotableErrorController.assistantCannotRegister().url
    }

    "throw an IllegalStateException if there is no internal id" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(None and Enrolments(Set.empty) and Some("") and Some(Organisation) and Some(User)))

      val result = intercept[Exception] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Failed to authorise due to missing data"
    }

    "throw an IllegalStateException if there is no group id" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(Some("") and Enrolments(Set.empty) and None and Some(Organisation) and Some(User)))

      val result = intercept[Exception] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Failed to authorise due to missing data"
    }

    "throw an IllegalStateException if there is no affinity group" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(Some("") and Enrolments(Set.empty) and Some("") and None and Some(User)))

      val result = intercept[Exception] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Failed to authorise due to missing data"
    }

    "throw an IllegalStateException if there is no credential role" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(Some("") and Enrolments(Set.empty) and Some("") and Some(Organisation) and None))

      val result = intercept[Exception] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Failed to authorise due to missing data"
    }
  }

}
