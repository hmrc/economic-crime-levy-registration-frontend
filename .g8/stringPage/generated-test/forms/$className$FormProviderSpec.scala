package forms

import uk.gov.hmrc.economiccrimelevyregistration.forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class $className$FormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "$className;format="decap"$.error.required"
  val lengthKey = "$className;format="decap"$.error.length"
  val maxLength = $maxLength$

  val form = new $className$FormProvider()()

  "value" should {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
