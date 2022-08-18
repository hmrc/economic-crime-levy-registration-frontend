package uk.gov.hmrc.economiccrimelevyregistration.forms

import javax.inject.Inject

import uk.gov.hmrc.economiccrimelevyregistration.forms.mappings.Mappings
import uk.gov.hmrc.economiccrimelevyregistration.models.$className$
import play.api.data.Form
import play.api.data.Forms._

class $className$FormProvider @Inject() extends Mappings {

   def apply(): Form[$className$] = Form(
     mapping(
      "$field1Name$" -> text("$className;format="decap"$.error.$field1Name$.required")
        .verifying(maxLength($field1MaxLength$, "$className;format="decap"$.error.$field1Name$.length")),
      "$field2Name$" -> text("$className;format="decap"$.error.$field2Name$.required")
        .verifying(maxLength($field2MaxLength$, "$className;format="decap"$.error.$field2Name$.length"))
    )($className$.apply)($className$.unapply)
   )

}
