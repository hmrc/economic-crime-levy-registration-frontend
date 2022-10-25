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

package uk.gov.hmrc.economiccrimelevyregistration.connectors

import org.scalacheck.Gen
import play.api.http.Status._
import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http.{HttpResponse, UpstreamErrorResponse}

class PackageSpec extends SpecBase {

  case class Foo(bar: String)

  object Foo {
    implicit val format: OFormat[Foo] = Json.format[Foo]
  }

  "readOptionOfNotFoundOrNoContent" should {
    "return None when the HTTP status is 204 NO_CONTENT" in {
      readOptionOfNotFoundOrNoContent[Foo].read("", "/", HttpResponse(status = NO_CONTENT, body = "")) shouldBe None
    }

    "return None when the HTTP status is 404 NOT_FOUND" in {
      readOptionOfNotFoundOrNoContent[Foo].read("", "/", HttpResponse(status = NOT_FOUND, body = "")) shouldBe None
    }

    "return the deserialized response body when the HTTP status is 200 OK" in {
      readOptionOfNotFoundOrNoContent[Foo]
        .read(
          "",
          "/",
          HttpResponse(status = OK, json = Json.toJson(Foo("bar")), headers = Map.empty)
        ) shouldBe Some(Foo("bar"))
    }

    "throw an UpstreamErrorResponse exception when the HTTP status is some other 4XX or 5XX" in forAll(
      Gen.choose(400, 599).suchThat(_ != 404)
    ) { httpStatus =>
      intercept[UpstreamErrorResponse] {
        readOptionOfNotFoundOrNoContent[Foo]
          .read(
            "",
            "/",
            HttpResponse(status = httpStatus, body = "")
          )
      }
    }
  }
}
