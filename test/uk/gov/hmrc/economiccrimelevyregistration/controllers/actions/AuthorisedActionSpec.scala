/*
 * Copyright 2022 HM Revenue & Customs
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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyregistration.services.EnrolmentStoreProxyService
import uk.gov.hmrc.economiccrimelevyregistration.{EnrolmentsWithEcl, EnrolmentsWithoutEcl}

import scala.concurrent.Future

class AuthorisedActionSpec extends SpecBase {

  val defaultBodyParser: BodyParsers.Default                     = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector                           = mock[AuthConnector]
  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]

  val authorisedAction =
    new BaseAuthorisedAction(mockAuthConnector, mockEnrolmentStoreProxyService, appConfig, defaultBodyParser)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  val eclEnrolmentKey: String = EclEnrolment.ServiceName

  val expectedRetrievals: Retrieval[Option[String] ~ Enrolments ~ Option[String]] =
    Retrievals.internalId and Retrievals.allEnrolments and Retrievals.groupIdentifier

  "invokeBlock" should {
    "execute the block and return the result if authorised" in forAll {
      (internalId: String, enrolmentsWithoutEcl: EnrolmentsWithoutEcl, groupId: String) =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(Some(internalId) and enrolmentsWithoutEcl.enrolments and Some(groupId)))

        when(mockEnrolmentStoreProxyService.groupHasEnrolment(ArgumentMatchers.eq(groupId))(any()))
          .thenReturn(Future.successful(false))

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

    "redirect the user to the already registered page if they have the ECL enrolment" in forAll {
      (internalId: String, enrolmentsWithEcl: EnrolmentsWithEcl, groupId: String) =>
        when(
          mockAuthConnector
            .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
        )
          .thenReturn(Future(Some(internalId) and enrolmentsWithEcl.enrolments and Some(groupId)))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Already registered - user already has enrolment"
    }

    "redirect the user to the group already registered page if they do not have the ECL enrolment but the group does" in forAll {
      (
        internalId: String,
        enrolmentsWithoutEcl: EnrolmentsWithoutEcl,
        groupId: String
      ) =>
        when(
          mockAuthConnector
            .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
        )
          .thenReturn(Future(Some(internalId) and enrolmentsWithoutEcl.enrolments and Some(groupId)))

        when(mockEnrolmentStoreProxyService.groupHasEnrolment(ArgumentMatchers.eq(groupId))(any()))
          .thenReturn(Future.successful(true))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Group already has the enrolment - assign the enrolment to the user"
    }

    "redirect the user to the unauthorised page if there is an authorisation exception" in {
      List(
        InsufficientConfidenceLevel(),
        InsufficientEnrolments(),
        UnsupportedAffinityGroup(),
        UnsupportedCredentialRole(),
        UnsupportedAuthProvider(),
        IncorrectCredentialStrength(),
        InternalError()
      ).foreach { exception =>
        when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)                 shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe routes.UnauthorisedController.onPageLoad().url
      }
    }

    "throw an IllegalStateException if there is no internal id" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(None and Enrolments(Set.empty) and Some("")))

      val result = intercept[IllegalStateException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Unable to retrieve internalId"
    }

    "throw an IllegalStateException if there is no group id" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(Some("") and Enrolments(Set.empty) and None))

      val result = intercept[IllegalStateException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Unable to retrieve groupIdentifier"
    }
  }

}
