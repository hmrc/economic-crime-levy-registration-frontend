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

package uk.gov.hmrc.economiccrimelevyregistration.forms

import play.api.data.Forms.mapping
import play.api.data.{Form, Forms}
import uk.gov.hmrc.economiccrimelevyregistration.config.AppConfig
import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{Other, values}
import uk.gov.hmrc.economiccrimelevyregistration.models.{AmlSupervisor, AmlSupervisorType, Enumerable}
import uk.gov.voa.play.form.ConditionalMappings.mandatoryIfEqual

class AmlSupervisorFormProvider extends Mappings {

  implicit val enumerable: Enumerable[AmlSupervisorType] = Enumerable(values.map(v => (v.toString, v)): _*)

  def apply(appConfig: AppConfig): Form[AmlSupervisor] = Form(
    mapping(
      "value"                 -> enumerable[AmlSupervisorType]("amlSupervisor.error.required"),
      "otherProfessionalBody" -> mandatoryIfEqual(
        "value",
        Other.toString,
        Forms.text.verifying("amlSupervisor.selectFromList.error", appConfig.amlProfessionalBodySupervisors.contains(_))
      )
    )(AmlSupervisor.apply)(a => Some((a.supervisorType, a.otherProfessionalBody)))
  )

}
