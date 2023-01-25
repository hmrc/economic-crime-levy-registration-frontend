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

import com.google.inject.AbstractModule
import play.api.{Configuration, Environment}
import uk.gov.hmrc.economiccrimelevyregistration.connectors._
import uk.gov.hmrc.economiccrimelevyregistration.controllers.actions._
import uk.gov.hmrc.economiccrimelevyregistration.testonly.connectors.stubs._

import java.time.{Clock, ZoneOffset}

class Module(environment: Environment, configuration: Configuration) extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[DataRetrievalAction])
      .to(classOf[RegistrationDataRetrievalAction])
      .asEagerSingleton()

    bind(classOf[ValidatedRegistrationAction]).to(classOf[ValidatedRegistrationActionImpl]).asEagerSingleton()

    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone.withZone(ZoneOffset.UTC))

    val grsStubEnabled = configuration.get[Boolean]("features.grsStubEnabled")
    val alfStubEnabled = configuration.get[Boolean]("features.alfStubEnabled")

    if (grsStubEnabled) {
      bind(classOf[IncorporatedEntityIdentificationFrontendConnector])
        .to(classOf[StubIncorporatedEntityIdentificationFrontendConnector])
        .asEagerSingleton()

      bind(classOf[PartnershipIdentificationFrontendConnector])
        .to(classOf[StubPartnershipIdentificationFrontendConnector])
        .asEagerSingleton()

      bind(classOf[SoleTraderIdentificationFrontendConnector])
        .to(classOf[StubSoleTraderIdentificationFrontendConnector])
        .asEagerSingleton()
    } else {
      bind(classOf[IncorporatedEntityIdentificationFrontendConnector])
        .to(classOf[IncorporatedEntityIdentificationFrontendConnectorImpl])
        .asEagerSingleton()

      bind(classOf[PartnershipIdentificationFrontendConnector])
        .to(classOf[PartnershipIdentificationFrontendConnectorImpl])
        .asEagerSingleton()

      bind(classOf[SoleTraderIdentificationFrontendConnector])
        .to(classOf[SoleTraderIdentificationFrontendConnectorImpl])
        .asEagerSingleton()
    }

    if (alfStubEnabled) {
      bind(classOf[AddressLookupFrontendConnector])
        .to(classOf[StubAddressLookupFrontendConnector])
        .asEagerSingleton()
    } else {
      bind(classOf[AddressLookupFrontendConnector])
        .to(classOf[AddressLookupFrontendConnectorImpl])
        .asEagerSingleton()
    }

  }
}
