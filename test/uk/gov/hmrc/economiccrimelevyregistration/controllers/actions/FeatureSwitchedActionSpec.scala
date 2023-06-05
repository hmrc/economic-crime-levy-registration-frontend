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

import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.handlers.ErrorHandler

import scala.concurrent.Future

class FeatureSwitchedActionSpec extends SpecBase {

  val errorHandler: ErrorHandler    = app.injector.instanceOf[ErrorHandler]
  val parser: BodyParsers.Default   = app.injector.instanceOf[BodyParsers.Default]
  override val appConfig: AppConfig = mock[AppConfig]

  def featureSwitchedAction(enabled: Boolean): FeatureSwitchedAction =
    new FeatureSwitchedAction(errorHandler, parser) {
      override val featureEnabled: Boolean = enabled
    }

  val testAction: Request[_] => Future[Result] = { _ =>
    Future.successful(Ok("Test"))
  }

  "feature switched action" should {
    "return Ok if enabled" in {
      when(appConfig.privateBetaEnabled).thenReturn(false)

      val enabled = featureSwitchedAction(true)

      val result = enabled.invokeBlock(fakeRequest, testAction)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe "Test"
    }

    "return not found if disabled" in {
      when(appConfig.privateBetaEnabled).thenReturn(true)

      val enabled = featureSwitchedAction(false)

      val result = enabled.invokeBlock(fakeRequest, testAction)

      status(result)          shouldBe NOT_FOUND
      contentAsString(result) shouldBe errorHandler.notFoundTemplate(fakeRequest).toString()
    }
  }

}
