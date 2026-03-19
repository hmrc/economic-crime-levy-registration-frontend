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

package uk.gov.hmrc.economiccrimelevyregistration.deregister

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.{GetSubscriptionResponse, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister._
import uk.gov.hmrc.economiccrimelevyregistration.base.ItTestData
import org.scalacheck.Arbitrary.arbitrary
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries.given

class DeregistrationRequestedISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.DeregistrationRequestedController.onPageLoad().url}" should {

    "respond with 200 status and the start HTML view" in {
      stubAuthorisedWithEclEnrolment()
      val email                 = ItTestData.email()
      val deregistration        =
        arbitrary[Deregistration].sample.get.copy(
          contactDetails = validContactDetails.copy(emailAddress = Some(email))
        )
      val updatedDeregistration =
        deregistration.copy(
          internalId = testInternalId,
          contactDetails = deregistration.contactDetails.copy(
            emailAddress = Some(arbitrary[String].sample.get)
          )
        )
      stubGetDeregistration(updatedDeregistration)
      stubDeleteDeregistration()
      stubGetSubscription(arbitrary[GetSubscriptionResponse].sample.get)

      val result = callRoute(
        FakeRequest(
          routes.DeregistrationRequestedController
            .onPageLoad()
        ).withSession(SessionKeys.firstContactEmail -> email)
      )

      status(result) shouldBe OK
      html(result)     should include("Deregistration requested")
    }
  }

  "respond with an error is no email on session" in {
    stubAuthorisedWithEclEnrolment()
    val email                 = ItTestData.email()
    val deregistration        =
      arbitrary[Deregistration].sample.get
        .copy(contactDetails = validContactDetails.copy(emailAddress = Some(email)))
    val updatedDeregistration =
      deregistration.copy(
        internalId = testInternalId,
        contactDetails = deregistration.contactDetails.copy(emailAddress = Some(email))
      )
    stubGetDeregistration(updatedDeregistration)
    stubDeleteDeregistration()
    stubGetSubscription(arbitrary[GetSubscriptionResponse].sample.get)

    val result = callRoute(
      FakeRequest(
        routes.DeregistrationRequestedController
          .onPageLoad()
      )
    )

    status(result) shouldBe INTERNAL_SERVER_ERROR
  }
}
