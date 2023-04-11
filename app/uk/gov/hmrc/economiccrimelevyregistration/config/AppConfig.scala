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

package uk.gov.hmrc.economiccrimelevyregistration.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost                  = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = configuration.get[String]("contact-frontend.serviceId")

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  def feedbackUrl(backUrl: String): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(backUrl).encodedUrl}"

  val signInUrl: String                 = configuration.get[String]("urls.signIn")
  val signOutUrl: String                = configuration.get[String]("urls.signOut")
  val eclSignOutUrl: String             = configuration.get[String]("urls.eclSignOut")
  val grsContinueUrl: String            = configuration.get[String]("urls.grsContinue")
  val alfContinueUrl: String            = configuration.get[String]("urls.alfContinue")
  val submitReturnUrl: String           = configuration.get[String]("urls.submitReturn")
  val taxAndSchemeManagementUrl: String = configuration.get[String]("urls.taxAndSchemeManagement")

  val accessibilityStatementServicePath: String =
    configuration.get[String]("accessibility-statement.service-path")

  val accessibilityStatementPath: String =
    s"/accessibility-statement$accessibilityStatementServicePath"

  private val exitSurveyHost              = configuration.get[String]("feedback-frontend.host")
  private val exitSurveyServiceIdentifier = configuration.get[String]("feedback-frontend.serviceId")

  val exitSurveyUrl: String = s"$exitSurveyHost/feedback/$exitSurveyServiceIdentifier"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val eclRegistrationBaseUrl: String = servicesConfig.baseUrl("economic-crime-levy-registration")
  val eclCalculatorBaseUrl: String   = servicesConfig.baseUrl("economic-crime-levy-calculator")

  val incorporatedEntityIdentificationFrontendBaseUrl: String =
    servicesConfig.baseUrl("incorporated-entity-identification-frontend")
  val soleTraderEntityIdentificationFrontendBaseUrl: String   =
    servicesConfig.baseUrl("sole-trader-identification-frontend")
  val partnershipEntityIdentificationFrontendBaseUrl: String  =
    servicesConfig.baseUrl("partnership-identification-frontend")

  val incorporatedEntityBvEnabled: Boolean                = configuration.get[Boolean]("features.incorporatedEntityBvEnabled")
  val partnershipBvEnabled: Boolean                       = configuration.get[Boolean]("features.partnershipBvEnabled")
  val soleTraderBvEnabled: Boolean                        = configuration.get[Boolean]("features.soleTraderBvEnabled")
  val enrolmentStoreProxyStubReturnsEclReference: Boolean =
    configuration.get[Boolean]("features.enrolmentStoreProxyStubReturnsEclReference")

  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")

  val addressLookupFrontendBaseUrl: String = servicesConfig.baseUrl("address-lookup-frontend")

  val emailBaseUrl: String = servicesConfig.baseUrl("email")

  val amlProfessionalBodySupervisors: Seq[String] = configuration.get[Seq[String]]("amlProfessionalBodySupervisors")

  val privateBetaAccessCode: String   = configuration.get[String]("features.privateBeta.accessCode")
  val privateBetaContactEmail: String = configuration.get[String]("features.privateBeta.contactEmail")
  val privateBetaEnabled: Boolean     = configuration.get[Boolean]("features.privateBeta.enabled")

}
