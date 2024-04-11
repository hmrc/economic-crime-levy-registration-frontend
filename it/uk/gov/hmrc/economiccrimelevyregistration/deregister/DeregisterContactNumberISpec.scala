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
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.telephoneNumberMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, ContactDetails, NormalMode}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes._

class DeregisterContactNumberISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${DeregisterContactNumberController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithoutEnrolmentCheckRoute(
        DeregisterContactNumberController
          .onPageLoad(mode)
      )

      "respond with 200 status and the deregister name HTML view" in {
        stubAuthorisedWithEclEnrolment()
        val name = random[String]
        stubGetDeregistration(
          random[Deregistration].copy(contactDetails = ContactDetails(Some(name), None, None, None))
        )

        val result = callRoute(
          FakeRequest(
            DeregisterContactNumberController
              .onPageLoad(mode)
          )
        )

        status(result) shouldBe OK
        html(result)     should include(s"What is $name's telephone number?")
      }
    }

    s"POST ${DeregisterContactNumberController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithoutEnrolmentCheckRoute(
        DeregisterContactNumberController
          .onSubmit(mode)
      )

      "save the selected answer then redirect the deregister email page" in {
        stubAuthorisedWithEclEnrolment()
        val deregistration = random[Deregistration].copy(internalId = testInternalId)
        val number         = telephoneNumber(telephoneNumberMaxLength).sample.get
        stubGetDeregistration(deregistration)
        stubUpsertDeregistration(
          deregistration.copy(contactDetails = deregistration.contactDetails.copy(telephoneNumber = Some(number)))
        )

        val result = callRoute(
          FakeRequest(DeregisterContactNumberController.onSubmit(mode))
            .withFormUrlEncodedBody(("value", number))
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(
          DeregisterCheckYourAnswersController
            .onPageLoad()
            .url
        )
      }
    }
  }
}
