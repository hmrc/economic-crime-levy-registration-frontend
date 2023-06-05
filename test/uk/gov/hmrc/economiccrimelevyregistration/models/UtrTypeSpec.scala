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

package uk.gov.hmrc.economiccrimelevyregistration.models

import play.api.libs.json.{JsBoolean, JsError, JsString, Json}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._

class UtrTypeSpec extends SpecBase {
  "writes" should {
    "return the Utr type serialized to its JSON representation" in forAll { (utrType: UtrType) =>
      val result = Json.toJson(utrType)

      result shouldBe JsString(utrType.toString)
    }
  }

  "reads" should {
    "return the Utr type deserialized from its JSON representation" in forAll { (utrType: UtrType) =>
      val json = Json.toJson(utrType)

      json.as[UtrType] shouldBe utrType
    }

    "return a JsError when passed an invalid string value" in {
      val result = Json.fromJson[UtrType](JsString("Test"))

      result shouldBe JsError("Test is not a valid UtrType")
    }

    "return a JsError when passed a type that is not a string" in {
      val result = Json.fromJson[UtrType](JsBoolean(true))

      result shouldBe a[JsError]
    }
  }
}
