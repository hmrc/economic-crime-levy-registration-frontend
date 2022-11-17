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

package uk.gov.hmrc.economiccrimelevyregistration.models

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase

class AmlSupervisorSpec extends SpecBase {
  "writes" should {
    "return the Aml supervisor serialized to its JSON representation" in forAll { (amlSupervisor: AmlSupervisor) =>
      val result = Json.toJson(amlSupervisor)

      result shouldBe JsString(amlSupervisor.toString)
    }
  }

  "reads" should {
    "return the Aml supervisor deserialized from its JSON representation" in forAll { (amlSupervisor: AmlSupervisor) =>
      val json = Json.toJson(amlSupervisor)

      json.as[AmlSupervisor] shouldBe amlSupervisor
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[AmlSupervisor](JsString("Test"))

      result shouldBe JsError("Test is not a valid AmlSupervisor")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[AmlSupervisor](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
