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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.RoleMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{ContactDetails, GetSubscriptionResponse}

class DeregisterCheckYourAnswersISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterCheckYourAnswersController.onPageLoad().url}"  should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterCheckYourAnswersController
        .onPageLoad()
    )

    "respond with 200 status and the deregister name HTML view" in {
      stubAuthorisedWithEclEnrolment()
      val deregistration = random[Deregistration].copy(internalId = testInternalId)
      stubGetDeregistration(deregistration)
      stubUpsertDeregistration(
        deregistration.copy(eclReference = Some(testEclRegistrationReference))
      )
      stubGetSubscription(random[GetSubscriptionResponse])

      val result = callRoute(
        FakeRequest(
          uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterCheckYourAnswersController
            .onPageLoad()
        )
      )

      status(result) shouldBe OK
      html(result)     should include(s"Check your answers")
    }
  }

  s"POST ${uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterCheckYourAnswersController.onPageLoad().url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterCheckYourAnswersController
        .onSubmit()
    )

    "delete the deregistration then redirect the account dashboard" in {
      stubAuthorisedWithEclEnrolment()
      val deregistration = random[Deregistration].copy(internalId = testInternalId)
      val role           = stringsWithMaxLength(RoleMaxLength).sample.get
      stubGetDeregistration(deregistration)
      stubDeleteDeregistration()

      val result = callRoute(
        FakeRequest(
          uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterCheckYourAnswersController
            .onSubmit()
        )
          .withFormUrlEncodedBody(("value", role))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(appConfig.yourEclAccountUrl)
    }
  }

}
