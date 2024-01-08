/*
 * Copyright 2023 HM Revenue & Customs
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

import com.google.inject.Inject
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyregistration.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions.{AuthorisedActionWithEnrolmentCheck, DataRetrievalAction, ValidatedRegistrationAction}
import uk.gov.hmrc.economiccrimelevyregistration.models.RegistrationType.{Amendment, Initial}
import uk.gov.hmrc.economiccrimelevyregistration.models.requests.RegistrationDataRequest
import uk.gov.hmrc.economiccrimelevyregistration.models.{Base64EncodedFields, EntityType, Registration, RegistrationType, SessionKeys}
import uk.gov.hmrc.economiccrimelevyregistration.services.{EclRegistrationService, EmailService, RegistrationAdditionalInfoService}
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.checkAnswers._
import uk.gov.hmrc.economiccrimelevyregistration.viewmodels.govuk.summarylist._
import uk.gov.hmrc.economiccrimelevyregistration.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyregistration.views.html.{AmendRegistrationPdfView, AnswersAreInvalidView, CheckYourAnswersView, OtherRegistrationPdfView}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import java.util.Base64
import javax.inject.Singleton
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CheckYourAnswersController @Inject() (
  override val messagesApi: MessagesApi,
  authorise: AuthorisedActionWithEnrolmentCheck,
  getRegistrationData: DataRetrievalAction,
  registrationService: EclRegistrationService,
  registrationAdditionalInfoService: RegistrationAdditionalInfoService,
  val controllerComponents: MessagesControllerComponents,
  view: CheckYourAnswersView,
  validateRegistrationData: ValidatedRegistrationAction,
  emailService: EmailService,
  answersAreInvalidView: AnswersAreInvalidView,
  otherRegistrationPdfView: OtherRegistrationPdfView,
  amendRegistrationPdfView: AmendRegistrationPdfView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  private def eclDetails()(implicit request: RegistrationDataRequest[_]): SummaryList = SummaryListViewModel(
    rows = Seq(
      EclReferenceNumberSummary.row()
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  private def organisationDetails(liabilityRow: Option[SummaryListRow] = None)(implicit
    request: RegistrationDataRequest[_]
  ): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        EntityTypeSummary.row(),
        EntityNameSummary.row(),
        CompanyNumberSummary.row(),
        CtUtrSummary.row(),
        SaUtrSummary.row(),
        NinoSummary.row(),
        DateOfBirthSummary.row(),
        AmlRegulatedActivitySummary.row(),
        liabilityRow,
        RelevantAp12MonthsSummary.row(),
        RelevantApLengthSummary.row(),
        UkRevenueSummary.row(),
        AmlSupervisorSummary.row(),
        BusinessSectorSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")

  private def contactDetails()(implicit request: RegistrationDataRequest[_]): SummaryList = SummaryListViewModel(
    rows = Seq(
      FirstContactNameSummary.row(),
      FirstContactRoleSummary.row(),
      FirstContactEmailSummary.row(),
      FirstContactNumberSummary.row(),
      SecondContactSummary.row(),
      SecondContactNameSummary.row(),
      SecondContactRoleSummary.row(),
      SecondContactEmailSummary.row(),
      SecondContactNumberSummary.row(),
      UseRegisteredAddressSummary.row(),
      ContactAddressSummary.row()
    ).flatten
  ).withCssClass("govuk-!-margin-bottom-9")

  def onPageLoad(): Action[AnyContent] =
    (authorise andThen getRegistrationData andThen validateRegistrationData) { implicit request =>
      Ok(
        view(
          eclDetails(),
          organisationDetails(),
          contactDetails(),
          otherEntityDetails(),
          request.registration.registrationType,
          request.eclRegistrationReference
        )
      )
    }

  private def getBase64EncodedPdf(registration: Registration)(implicit
    request: RegistrationDataRequest[_]
  ) =
    (registration.entityType, registration.registrationType) match {
      case (Some(_), Some(Amendment))                    =>
        createAndEncodeHtmlForPdf(registration.registrationType)
      case (Some(value), _) if EntityType.isOther(value) =>
        createAndEncodeHtmlForPdf(registration.registrationType)
      case (None, Some(Amendment))                       =>
        createAndEncodeHtmlForPdf(registration.registrationType)
      case _                                             =>
        ""
    }

  def onSubmit(): Action[AnyContent] = (authorise andThen getRegistrationData).async { implicit request =>
    val htmlView = view(
      eclDetails(),
      organisationDetails(),
      contactDetails(),
      otherEntityDetails(),
      request.registration.registrationType,
      request.eclRegistrationReference
    )

    val registration = request.registration

    val base64EncodedHtmlView: String = base64EncodeHtmlView(htmlView.body)
    val base64EncodedHtmlViewForPdf   = getBase64EncodedPdf(registration)

    (for {
      _        <- registrationService.upsertRegistration(registration =
                    registration.copy(
                      base64EncodedFields = Some(
                        Base64EncodedFields(
                          nrsSubmissionHtml = Some(base64EncodedHtmlView),
                          dmsSubmissionHtml = (registration.entityType, registration.registrationType) match {
                            case (Some(_), Some(Amendment))                    =>
                              Some(base64EncodedHtmlViewForPdf)
                            case (Some(value), _) if EntityType.isOther(value) =>
                              Some(base64EncodedHtmlViewForPdf)
                            case (None, Some(Amendment))                       =>
                              Some(base64EncodedHtmlViewForPdf)
                            case _                                             =>
                              None
                          }
                        )
                      )
                    )
                  ).asResponseError
      response <- registrationService.submitRegistration(request.internalId).asResponseError
      _        <- registrationService.deleteRegistration(request.internalId).asResponseError
      _        <- registrationAdditionalInfoService.delete(request.internalId).asResponseError
    } yield response).fold {
      _        => Future.successful(Ok(answersAreInvalidView())),
      response => {

        (for {
          result <- request.registration.registrationType match {
            case Some(registrationType) =>
              registrationType match {
                case Initial =>
                  emailService.sendRegistrationSubmittedEmails(
                    registration.contacts,
                    response.eclReference,
                    registration.entityType,
                    request.additionalInfo,
                    request.registration.carriedOutAmlRegulatedActivityInCurrentFy
                  )
                case Amendment => emailService.sendAmendRegistrationSubmitted(registration.contacts).asResponseError
              }
            case None => throw new IllegalStateException("Invalid contact details")
          }
        } yield result).fold(
          _ => Future.successful(Ok(answersAreInvalidView())),
          _ => {
            val session = registration.entityType match {
              case Some(value) if EntityType.isOther(value) => request.session
              case _ =>
                request.session ++ Seq(
                  SessionKeys.EclReference -> response.eclReference
                )
            }

            (for {
              result <- getField(
                "First contact email address not found in registration data",
                registration.contacts.firstContactDetails.emailAddress
              ).asResponseError
            } yield result).fold(
              _ => Future.successful(Ok(answersAreInvalidView())),
              emailAddress => {
                val updatedSession = session ++ Seq(
                  SessionKeys.FirstContactEmailAddress -> emailAddress
                )

                Redirect((registration.entityType, registration.registrationType) match {
                  case (Some(value), Some(Initial)) if EntityType.isOther(value) =>
                    routes.RegistrationReceivedController.onPageLoad()
                  case (Some(_), Some(Amendment)) =>
                    routes.AmendmentRequestedController.onPageLoad()
                  case (None, Some(Amendment)) =>
                    routes.AmendmentRequestedController.onPageLoad()
                  case _ =>
                    routes.RegistrationSubmittedController.onPageLoad()
                }).withSession(
                  registration.contacts.secondContactDetails.emailAddress.fold(updatedSession)(email =>
                    updatedSession ++ Seq(SessionKeys.SecondContactEmailAddress -> email)
                  )
                )
              }
            )
          }
        )
      }
    }
  }

  private def base64EncodeHtmlView(html: String): String = Base64.getEncoder
    .encodeToString(html.getBytes)

  def createAndEncodeHtmlForPdf(
    registrationType: Option[RegistrationType]
  )(implicit request: RegistrationDataRequest[_]): String = {
    val date         = LocalDate.now
    val organisation = organisationDetails(LiabilityYearSummary.row())
    val contact      = contactDetails()
    val otherEntity  = otherEntityDetails()

    registrationType match {
      case Some(Amendment) =>
        base64EncodeHtmlView(
          amendRegistrationPdfView(
            ViewUtils.formatLocalDate(date),
            eclDetails(),
            organisation.copy(rows = organisation.rows.map(_.copy(actions = None))),
            contact.copy(rows = contact.rows.map(_.copy(actions = None)))
          ).toString()
        )
      case _               =>
        base64EncodeHtmlView(
          otherRegistrationPdfView(
            ViewUtils.formatLocalDate(date),
            organisation.copy(rows = organisation.rows.map(_.copy(actions = None))),
            contact.copy(rows = contact.rows.map(_.copy(actions = None))),
            otherEntity.copy(rows = otherEntity.rows.map(_.copy(actions = None)))
          ).toString()
        )
    }
  }

  def otherEntityDetails()(implicit request: RegistrationDataRequest[_]): SummaryList =
    SummaryListViewModel(
      rows = Seq(
        BusinessNameSummary.row(),
        CharityRegistrationNumberSummary.row(),
        DoYouHaveCrnSummary.row(),
        CompanyRegistrationNumberSummary.row(),
        DoYouHaveCtUtrSummary.row(),
        UtrTypeSummary.row(),
        OtherEntitySaUtrSummary.row(),
        OtherEntityCtUtrSummary.row(),
        OtherEntityPostcodeSummary.row()
      ).flatten
    ).withCssClass("govuk-!-margin-bottom-9")
}
