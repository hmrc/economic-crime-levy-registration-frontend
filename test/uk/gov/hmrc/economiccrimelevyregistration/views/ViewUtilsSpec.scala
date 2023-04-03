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

package uk.gov.hmrc.economiccrimelevyregistration.views

import play.api.data.Form
import play.api.data.Forms.{single, text}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

import java.time.LocalDate

class ViewUtilsSpec extends SpecBase {

  val testForm: Form[String] = Form(
    single("testValue" -> text)
  )

  val testTitle: String = "Test Title"

  "titleWithForm" should {
    "return a correctly formatted title when a form has no errors" in {
      ViewUtils.titleWithForm(testForm, testTitle, None)(
        messages
      ) shouldBe "Test Title - Register for the Economic Crime Levy - GOV.UK"
    }

    "return a correctly formatted title when a form has errors" in {
      ViewUtils.titleWithForm(testForm.withError(key = "testErrorKey", message = "testErrorMessage"), testTitle, None)(
        messages
      ) shouldBe "Error: Test Title - Register for the Economic Crime Levy - GOV.UK"
    }
  }

  "title" should {
    "return a correctly formatted title when there is no section" in {
      ViewUtils.title(testTitle, None)(
        messages
      ) shouldBe "Test Title - Register for the Economic Crime Levy - GOV.UK"
    }

    "return a correctly formatted title when there is a section" in {
      ViewUtils.title(testTitle, Some("Test Section"))(
        messages
      ) shouldBe "Test Title - Test Section - Register for the Economic Crime Levy - GOV.UK"
    }
  }

  "formatLocalDate" should {
    "correctly format a translated local date" in {
      val localDate = LocalDate.parse("2007-12-03")

      ViewUtils.formatLocalDate(localDate)(messages) shouldBe "3 December 2007"
    }

    "correctly format a non-translated local date" in {
      val localDate = LocalDate.parse("2007-12-03")

      ViewUtils.formatLocalDate(localDate, translate = false)(messages) shouldBe "3 December 2007"
    }
  }

  "formatMoney" should {
    "format a monetary amount with commas" in {
      val amount = 1000000000

      ViewUtils.formatMoney(amount) shouldBe "1,000,000,000"
    }
  }

}
