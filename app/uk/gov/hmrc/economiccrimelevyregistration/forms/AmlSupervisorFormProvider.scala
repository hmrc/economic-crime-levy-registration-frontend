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

package uk.gov.hmrc.economiccrimelevyregistration.forms

import play.api.data.Forms.{mapping, optional}
import play.api.data.{Form, Forms}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, Other}

class AmlSupervisorFormProvider extends Mappings {

  def apply(appConfig: AppConfig): Form[AmlSupervisor] = Form(
    mapping(
      "value"                 -> enumerable[AmlSupervisorType]("amlSupervisor.error.required"),
      "otherProfessionalBody" -> optional(Forms.text)
    )(AmlSupervisor.apply)(AmlSupervisor.unapply)
      .verifying { amlSupervisor =>
        amlSupervisor.supervisorType match {
          case Other =>
            amlSupervisor.otherProfessionalBody match {
              case Some(opb) => appConfig.amlProfessionalBodySupervisors.contains(opb)
              case None      => false
            }
          case _     => true
        }
      }
      .transform[AmlSupervisor](
        amlSupervisor =>
          if (amlSupervisor.supervisorType != Other) {
            amlSupervisor.copy(otherProfessionalBody = None)
          } else {
            identity(amlSupervisor)
          },
        identity
      )
  )

}
