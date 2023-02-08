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

package uk.gov.hmrc.economiccrimelevyregistration.testonly.utils

import java.util.Base64

object Base64Utils {

  def base64UrlEncode(valueToEncode: String): String = Base64.getEncoder
    .encodeToString(valueToEncode.getBytes)
    .replace("+", ".")
    .replace("/", "_")
    .replace("=", "-")

  def base64UrlDecode(valueToDecode: String): String = {
    val decodedBytes = Base64.getDecoder.decode(
      valueToDecode
        .replace(".", "+")
        .replace("_", "/")
        .replace("-", "=")
    )

    new String(decodedBytes)
  }

}
