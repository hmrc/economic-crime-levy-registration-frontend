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

package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, SessionData, SessionKeys}

class SavedResponsesISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.SavedResponsesController.onPageLoad.url}" should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.SavedResponsesController.onPageLoad)

    "respond with 200 status and the start HTML view" in {
      stubAuthorisedWithNoGroupEnrolment()

      val result = callRoute(FakeRequest(routes.SavedResponsesController.onPageLoad))

      status(result) shouldBe OK
      html(result)     should include("Your answers have been saved")
    }
  }

  s"POST ${routes.SavedResponsesController.onSubmit.url}"  should {
    behave like authorisedActionWithEnrolmentCheckRoute(routes.SavedResponsesController.onSubmit)

    "respond with the next page" in {
      stubAuthorisedWithNoGroupEnrolment()
      val url    = random[String]
      stubGetSession(SessionData(random[String], Map(SessionKeys.UrlToReturnTo -> url)))

      val result = callRoute(
        FakeRequest(routes.SavedResponsesController.onSubmit)
          .withFormUrlEncodedBody("value" -> "true")
      )

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some(url)
    }
  }
}
