package uk.gov.hmrc.economiccrimelevyregistration.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import play.api.data.Form
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.forms.$className$FormProvider
import uk.gov.hmrc.economiccrimelevyregistration.models.{NormalMode, Registration}
import uk.gov.hmrc.economiccrimelevyregistration.views.html.$className$View

import scala.concurrent.Future

class $className$ControllerSpec extends SpecBase {

  val view: $className$View = app.injector.instanceOf[$className$View]

  val formProvider = new $className$FormProvider()

  val form: Form[Int] = formProvider()

  val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]

  class TestFixture(data: Registration = emptyRegistration) {
    val controller = new $className$Controller(
      messagesApi = messagesApi,
      eclRegistrationConnector = mockEclRegistrationConnector,
      navigator = fakeNavigator,
      authorise = fakeAuthorisedAction,
      getRegistrationData = fakeDataRetrievalAction(data),
      formProvider = formProvider,
      controllerComponents = mcc,
      view = view
    )
  }

  "onPageLoad" should {

    "return OK and the correct view" in new TestFixture() {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form, NormalMode)(fakeRequest, messages).toString
    }

    "populate the view correctly when the question has previously been answered" in new TestFixture(
      emptyRegistration.copy(??? = Some(1)) // TODO Choose the data you are testing
    ) {
      val result: Future[Result] = controller.onPageLoad(NormalMode)(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(form.fill(1), NormalMode)(fakeRequest, messages).toString
    }

    "redirect to the next page when valid data is submitted" in new TestFixture() {
      when(mockEclRegistrationConnector.upsertRegistration(any())).thenReturn(Future.successful(emptyRegistration))

      val result: Future[Result] =
        controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> "1"))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe onwardRoute.url
    }

    "return a Bad Request and errors when invalid data is submitted" in new TestFixture() {
      val result: Future[Result] = controller.onSubmit(NormalMode)(fakeRequest.withFormUrlEncodedBody("value" -> ""))

      val formWithErrors: Form[Int] = form.bind(Map("value" -> ""))

      status(result)          shouldBe BAD_REQUEST
      contentAsString(result) shouldBe view(formWithErrors, NormalMode)(fakeRequest, messages).toString
    }
  }
}
