package uk.gov.hmrc.economiccrimelevyregistration

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import org.scalacheck.Gen
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyregistration.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyregistration.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.controllers.routes
import uk.gov.hmrc.economiccrimelevyregistration.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyregistration.models.AmlSupervisorType.{FinancialConductAuthority, GamblingCommission, Hmrc, Other}
import uk.gov.hmrc.economiccrimelevyregistration.models._
import org.scalatest.prop.TableDrivenPropertyChecks._
import org.scalatest.prop.TableFor2

class AmlSupervisorISpec extends ISpecBase with AuthorisedBehaviour {

  val combinations: TableFor2[Mode, RegistrationType] = Table(
    ("Mode", "RegistrationType"),
    (NormalMode, Initial),
    (NormalMode, Amendment),
    (CheckMode, Initial),
    (CheckMode, Amendment)
  )

  forAll(combinations) { (mode: Mode, registrationType: RegistrationType) =>
    s"GET ${routes.AmlSupervisorController.onPageLoad(mode, registrationType).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.AmlSupervisorController.onPageLoad(mode, registrationType)
      )

      "respond with 200 status and the AML supervisor HTML view" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue())
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)
        stubSessionForStoreUrl()

        val result = callRoute(FakeRequest(routes.AmlSupervisorController.onPageLoad(mode, registrationType)))

        status(result) shouldBe OK

        html(result) should include("Your anti-money laundering (AML) supervisor")
      }
    }
  }

  forAll(combinations) { (mode: Mode, registrationType: RegistrationType) =>
    s"POST ${routes.AmlSupervisorController.onSubmit(mode, registrationType).url}" should {
      behave like authorisedActionWithEnrolmentCheckRoute(
        routes.AmlSupervisorController.onPageLoad(mode, registrationType)
      )

      s"$mode save the selected option then redirect to the correct page when the answer is either HMRC or another professional body for $registrationType registration type" in {
        stubAuthorisedWithNoGroupEnrolment()

        val carriedOutAmlRegulatedActivityInCurrentFy = random[Boolean]
        val registration                              =
          random[Registration]
            .copy(
              entityType = Some(random[EntityType]),
              relevantApRevenue = Some(randomApRevenue())
            )
            .copy(carriedOutAmlRegulatedActivityInCurrentFy = Some(carriedOutAmlRegulatedActivityInCurrentFy))
        val validRegistration                         = registration.copy(registrationType = Some(registrationType))
        val additionalInfo                            = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)

        val supervisorType        = Gen.oneOf[AmlSupervisorType](Seq(Hmrc, Other)).sample.get
        val otherProfessionalBody = Gen.oneOf(appConfig.amlProfessionalBodySupervisors).sample.get
        val amlSupervisor         =
          AmlSupervisor(supervisorType, if (supervisorType == Hmrc) None else Some(otherProfessionalBody))

        val formData: Seq[(String, String)] = amlSupervisor match {
          case AmlSupervisor(Other, Some(otherProfessionalBody)) =>
            Seq(("value", Other.toString), ("otherProfessionalBody", otherProfessionalBody))
          case _                                                 => Seq(("value", amlSupervisor.supervisorType.toString))
        }

        stubGetRegistrationWithEmptyAdditionalInfo(validRegistration)

        val updatedRegistration = validRegistration.copy(amlSupervisor = Some(amlSupervisor))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.AmlSupervisorController.onSubmit(mode, registrationType))
            .withFormUrlEncodedBody(formData: _*)
        )

        status(result) shouldBe SEE_OTHER

        mode match {
          case NormalMode =>
            if (registrationType == Amendment) {
              redirectLocation(result) shouldBe Some(routes.BusinessSectorController.onPageLoad(NormalMode).url)
            } else {
              registration.carriedOutAmlRegulatedActivityInCurrentFy match {
                case Some(true)  =>
                  redirectLocation(result) shouldBe Some(
                    routes.RelevantAp12MonthsController.onPageLoad(NormalMode).url
                  )
                case Some(false) =>
                  redirectLocation(result) shouldBe Some(routes.EntityTypeController.onPageLoad(NormalMode).url)
                case None        => fail()
              }
            }
          case _          =>
            redirectLocation(result) shouldBe Some(
              routes.CheckYourAnswersController.onPageLoad().url
            )
        }
      }

      s"$mode save the selected option then redirect to the register with GC page when the answer is GC for $registrationType registration type" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration   = random[Registration]
          .copy(
            internalId = testInternalId,
            entityType = Some(random[EntityType]),
            relevantApRevenue = Some(randomApRevenue()),
            registrationType = Some(registrationType)
          )
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)

        val amlSupervisor =
          AmlSupervisor(GamblingCommission, None)

        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val updatedRegistration = registration.copy(amlSupervisor = Some(amlSupervisor))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.AmlSupervisorController.onSubmit(mode, registrationType))
            .withFormUrlEncodedBody("value" -> GamblingCommission.toString)
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.RegisterWithGcController.onPageLoad().url)
      }

      s"$mode save the selected option then redirect to the register with FCA page when the answer is FCA for $registrationType registration type" in {
        stubAuthorisedWithNoGroupEnrolment()

        val registration = random[Registration]
          .copy(
            entityType = Some(random[EntityType]),
            internalId = testInternalId,
            registrationType = Some(registrationType),
            relevantApRevenue = Some(randomApRevenue())
          )

        val amlSupervisor  =
          AmlSupervisor(FinancialConductAuthority, None)
        val additionalInfo = random[RegistrationAdditionalInfo]

        stubGetRegistrationAdditionalInfo(additionalInfo)
        stubGetRegistrationWithEmptyAdditionalInfo(registration)

        val updatedRegistration = registration.copy(amlSupervisor = Some(amlSupervisor))

        stubUpsertRegistration(updatedRegistration)

        val result = callRoute(
          FakeRequest(routes.AmlSupervisorController.onSubmit(mode, registrationType))
            .withFormUrlEncodedBody("value" -> FinancialConductAuthority.toString)
        )

        status(result) shouldBe SEE_OTHER

        redirectLocation(result) shouldBe Some(routes.RegisterWithFcaController.onPageLoad().url)
      }
    }
  }
}
