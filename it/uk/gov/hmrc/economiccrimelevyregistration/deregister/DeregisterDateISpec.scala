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
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration
import uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes._

import java.time.LocalDate

class DeregisterDateISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${DeregisterDateController.onPageLoad(NormalMode).url}"  should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      DeregisterDateController
        .onPageLoad(NormalMode)
    )

    "respond with 200 status and the deregister date HTML view" in {
      stubAuthorisedWithEclEnrolment()
      stubGetDeregistration(random[Deregistration])

      val result = callRoute(
        FakeRequest(
          DeregisterDateController
            .onPageLoad(NormalMode)
        )
      )

      status(result) shouldBe OK
      html(result)     should include("Enter the date you were no longer liable")
    }
  }

  s"POST ${DeregisterDateController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      DeregisterDateController
        .onSubmit(NormalMode)
    )

    "save the selected answer then redirect the deregister name page" in {
      stubAuthorisedWithEclEnrolment()
      val deregistration = random[Deregistration].copy(internalId = testInternalId)
      val date           = LocalDate.now().minusDays(1)
      stubGetDeregistration(deregistration)
      stubUpsertDeregistration(deregistration.copy(date = Some(date)))

      val result = callRoute(
        FakeRequest(
          DeregisterDateController
            .onSubmit(NormalMode)
        )
          .withFormUrlEncodedBody(
            ("value.day", date.getDayOfMonth.toString),
            ("value.month", date.getMonthValue.toString),
            ("value.year", date.getYear.toString)
          )
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        DeregisterContactNameController
          .onPageLoad(NormalMode)
          .url
      )
    }
  }

}
