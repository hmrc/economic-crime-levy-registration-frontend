package uk.gov.hmrc.economiccrimelevyregistration.forms

import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.CheckboxFieldBehaviours
import uk.gov.hmrc.economiccrimelevyregistration.models.$className$
import play.api.data.FormError

class $className$FormProviderSpec extends CheckboxFieldBehaviours {

  val form = new $className$FormProvider()()

  "value" should {

    val fieldName = "value"
    val requiredKey = "$className;format="decap"$.error.required"

    behave like checkboxField[$className$](
      form,
      fieldName,
      validValues  = $className$.values,
      invalidError = FormError(s"\$fieldName[0]", "error.invalid")
    )

    behave like mandatoryCheckboxField(
      form,
      fieldName,
      requiredKey
    )
  }
}
