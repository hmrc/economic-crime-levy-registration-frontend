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
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.MaxLengths.NameMaxLength
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.NormalMode
import uk.gov.hmrc.economiccrimelevyregistration.models.deregister.Deregistration

class DeregisterContactNameISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactNameController.onPageLoad(NormalMode).url}"  should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactNameController
        .onPageLoad(NormalMode)
    )

    "respond with 200 status and the deregister name HTML view" in {
      stubAuthorisedWithEclEnrolment()
      stubGetDeregistration(random[Deregistration])

      val result = callRoute(
        FakeRequest(
          uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactNameController
            .onPageLoad(NormalMode)
        )
      )

      status(result) shouldBe OK
      html(result)     should include("Provide a contact name")
    }
  }

  s"POST ${uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactNameController.onPageLoad(NormalMode).url}" should {
    behave like authorisedActionWithoutEnrolmentCheckRoute(
      uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactNameController
        .onSubmit(NormalMode)
    )

    "save the selected answer then redirect the deregister role page" in {
      stubAuthorisedWithEclEnrolment()
      val deregistration = random[Deregistration].copy(internalId = testInternalId)
      val name           = stringsWithMaxLength(NameMaxLength).sample.get
      stubGetDeregistration(deregistration)
      stubUpsertDeregistration(
        deregistration.copy(contactDetails = deregistration.contactDetails.copy(name = Some(name)))
      )

      val result = callRoute(
        FakeRequest(
          uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactNameController
            .onSubmit(NormalMode)
        )
          .withFormUrlEncodedBody(("value", name))
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(
        uk.gov.hmrc.economiccrimelevyregistration.controllers.deregister.routes.DeregisterContactRoleController
          .onPageLoad(NormalMode)
          .url
      )
    }
  }

}
