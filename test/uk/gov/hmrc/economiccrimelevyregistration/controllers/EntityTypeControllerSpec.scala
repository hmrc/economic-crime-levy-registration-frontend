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

package uk.gov.hmrc.economiccrimelevyregistration.controllers

import play.api.data.Form
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.forms.EntityTypeFormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.EntityType
import uk.gov.hmrc.economiccrimelevyregistration.views.html.EntityTypeView

import scala.concurrent.Future

class EntityTypeControllerSpec extends SpecBase {

  val view: EntityTypeView                 = app.injector.instanceOf[EntityTypeView]
  val formProvider: EntityTypeFormProvider = new EntityTypeFormProvider()
  val form: Form[EntityType]               = formProvider()

  val controller = new EntityTypeController(
    mcc,
    fakeAuthorisedAction,
    fakeDataRetrievalAction(),
    formProvider,
    view
  )

  "onPageLoad" should {
    "return OK and the correct view" in {
      val result: Future[Result] = controller.onPageLoad()(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe view(form)(fakeRequest, messages).toString
    }
  }

  "onSubmit" should {
    "redirect to the GRS UK Limited Company journey when the UK Limited Company option is selected" in {
      val result: Future[Result] = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "UkLimitedCompany")))

      status(result) shouldBe SEE_OTHER

      redirectLocation(result) shouldBe Some("test-url")
    }

    "return a Bad Request with form errors when invalid data is submitted" in {
      val result: Future[Result] = controller.onSubmit()(fakeRequest.withFormUrlEncodedBody(("value", "")))
      val formWithErrors: Form[EntityType] = form.bind(Map("value" -> ""))

      status(result) shouldBe BAD_REQUEST

      contentAsString(result) shouldBe view(formWithErrors)(fakeRequest, messages).toString
    }
  }
}
