package uk.gov.hmrc.economiccrimelevyregistration.forms

import java.time.LocalDate
import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import play.api.data.Form

class $className$FormProvider @Inject() extends Mappings {

  def apply(): Form[LocalDate] =
    Form(
      "value" -> localDate(
        invalidKey     = "$className;format="decap"$.error.invalid",
        allRequiredKey = "$className;format="decap"$.error.required.all",
        twoRequiredKey = "$className;format="decap"$.error.required.two",
        requiredKey    = "$className;format="decap"$.error.required"
      )
    )
}
