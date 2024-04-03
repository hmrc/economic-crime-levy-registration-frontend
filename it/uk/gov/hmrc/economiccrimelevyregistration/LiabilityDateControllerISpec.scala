package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.Initial
import uk.gov.hmrc.economiccrimelevyregistration.models.{CheckMode, EntityType, LiabilityYear, NormalMode, Registration, RegistrationAdditionalInfo, RegistrationType}
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

class LiabilityDateControllerISpec extends ISpecBase with AuthorisedBehaviour {

  Seq(NormalMode, CheckMode).foreach { mode =>
    s"GET ${routes.LiabilityDateController.onPageLoad(mode).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.LiabilityDateController.onPageLoad(mode)
      )

      "respond with 200 status and the liability date HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()
        stubSessionForStoreUrl()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val result = callRoute(FakeRequest(routes.LiabilityDateController.onPageLoad(mode)))

        status(result) shouldBe OK

        html(result) should include("Enter the date you became liable for ECL")
      }
    }

    s"POST ${routes.LiabilityDateController.onSubmit(mode).url}"  should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.LiabilityDateController.onSubmit(mode)
      )

      "save the entered date then redirect to the check your answers page" in {
        stubAuthorisedWithNoGroupEnrolment()

        val date = LocalDate.now()
        val year = EclTaxYear.taxYearFor(date)

        val registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            registrationType = Some(random[RegistrationType]),
            relevantApRevenue = Some(randomApRevenue())
          )

        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        val info = RegistrationAdditionalInfo(
          testInternalId,
          Some(LiabilityYear(year.startYear)),
          Some(date),
          None,
          None,
          None
        )

        stubUpsertRegistrationAdditionalInfo(info)
        stubUpsertRegistration(registration)

        val result = callRoute(
          FakeRequest(routes.LiabilityDateController.onSubmit(mode))
            .withFormUrlEncodedBody(
              ("value.day", date.getDayOfMonth.toString),
              ("value.month", date.getMonthValue.toString),
              ("value.year", date.getYear.toString)
            )
        )

        status(result) shouldBe SEE_OTHER
        mode match {
          case NormalMode =>
            registration.amlSupervisor match {
              case None =>
                redirectLocation(result) shouldBe Some(
                  routes.AmlSupervisorController
                    .onPageLoad(NormalMode, registration.registrationType.getOrElse(Initial))
                    .url
                )
              case _    => redirectLocation(result) shouldBe Some(routes.EntityTypeController.onPageLoad(NormalMode).url)
            }

          case CheckMode =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad(registration.registrationType.getOrElse(Initial)).url
            )
        }
      }
    }
  }
}
