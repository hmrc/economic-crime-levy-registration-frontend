package uk.gov.hmrc.economiccrimelevyregistration.forms

import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.models.$className$
import play.api.data.Form
import play.api.data.Forms.set

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[Set[$className$]] =
    Form(
      "value" -> set(enumerable[$className$]("$className;format="decap"$.error.required")).verifying(nonEmptySet("$className;format="decap"$.error.required"))
    )

}
