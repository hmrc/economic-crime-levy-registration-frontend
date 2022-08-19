package uk.gov.hmrc.economiccrimelevyregistration.forms

import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError

class $className$FormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "$className;format="decap"$.error.required"
  val invalidKey = "error.boolean"

  val form = new $className$FormProvider()()

  "value" should {

    val fieldName = "value"

    behave like booleanField(
      form,
      fieldName,
      invalidError = FormError(fieldName, invalidKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
