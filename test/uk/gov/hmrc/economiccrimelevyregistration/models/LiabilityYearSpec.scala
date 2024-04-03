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

package uk.gov.hmrc.economiccrimelevyregistration.models

import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.economiccrimelevyregistration.base.SpecBase
import uk.gov.hmrc.economiccrimelevyregistration.utils.EclTaxYear

import java.time.LocalDate

//class LiabilityYearSpec extends SpecBase with Matchers {
//
//  class setUp(taxYear: Int) {
//    def givenALiabilityStartDateOf(when: LocalDate): LiabilityYear = {
//      val year              = EclTaxYear.taxYearFor(when)
//      val mockLiabilityYear = new LiabilityYear(year.startYear) {
//        override def currentTaxYear(): EclTaxYear = EclTaxYear(taxYear)
//      }
//      mockLiabilityYear
//    }
//  }
//
//  "isCurrentFY" should {
//    "return true if liability start date is set to 10th Feb 2023 and the current tax year is 2022" in new setUp(2022) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2023, 2, 10))
//      liabilityYear.isCurrentFY shouldBe true
//      liabilityYear.asString    shouldBe "2022"
//    }
//
//    "return true if liability start date is set to 10th Feb 2024 and the current tax year is 2023" in new setUp(2023) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 2, 10))
//      liabilityYear.isCurrentFY shouldBe true
//      liabilityYear.asString    shouldBe "2023"
//    }
//
//    "return false if liability start date is set to 10th Feb 2024 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 2, 10))
//      liabilityYear.isCurrentFY shouldBe false
//      liabilityYear.asString    shouldBe "2023"
//    }
//
//    "return false if liability start date is set to 31st Mar 2024 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 3, 31))
//      liabilityYear.isCurrentFY shouldBe false
//      liabilityYear.asString    shouldBe "2023"
//    }
//
//    "return true if liability start date is set to 6th Apr 2024 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 4, 6))
//      liabilityYear.isCurrentFY shouldBe true
//      liabilityYear.asString    shouldBe "2024"
//    }
//
//    "return true if liability start date is set to 14th Apr 2029 and the current tax year is 2029" in new setUp(2029) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2029, 4, 14))
//      liabilityYear.isCurrentFY shouldBe true
//      liabilityYear.asString    shouldBe "2029"
//    }
//
//    "return true if liability start date is set to 20th Dec 2024 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 12, 20))
//      liabilityYear.isCurrentFY shouldBe true
//      liabilityYear.asString    shouldBe "2024"
//    }
//
//    "return false if liability start date is set to 29th Feb 2024 and the current tax year is 2026" in new setUp(2026) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 2, 29))
//      liabilityYear.isCurrentFY shouldBe false
//      liabilityYear.asString    shouldBe "2023"
//    }
//  }
//
//  "isNotCurrentFY" should {
//    "return false if liability start date is set to 12th Apr 2022 and the current tax year is 2022" in new setUp(2022) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2022, 4, 12))
//      liabilityYear.isNotCurrentFY shouldBe false
//      liabilityYear.asString       shouldBe "2022"
//    }
//
//    "return false if liability start date is set to 22nd Jul 2024 and the current tax year is 2023" in new setUp(2023) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 7, 22))
//      liabilityYear.isNotCurrentFY shouldBe false
//      liabilityYear.asString       shouldBe "2024"
//    }
//
//    "return false if liability start date is set to 10th Sep 2024 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 9, 10))
//      liabilityYear.isNotCurrentFY shouldBe false
//      liabilityYear.asString       shouldBe "2024"
//    }
//
//    "return true if liability start date is set to 31st Mar 2024 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 3, 31))
//      liabilityYear.isNotCurrentFY shouldBe true
//      liabilityYear.asString       shouldBe "2023"
//    }
//
//    "return true if liability start date is set to 31 Aug 2024 and the current tax year is 2025" in new setUp(2025) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2024, 8, 31))
//      liabilityYear.isNotCurrentFY shouldBe true
//      liabilityYear.asString       shouldBe "2024"
//    }
//
//    "return true if liability start date is set to 20th Nov 2023 and the current tax year is 2024" in new setUp(2024) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2023, 11, 20))
//      liabilityYear.isNotCurrentFY shouldBe true
//      liabilityYear.asString       shouldBe "2023"
//    }
//
//    "return false if liability start date is set to 16th Oct 2028 and the current tax year is 2028" in new setUp(2028) {
//      val liabilityYear: LiabilityYear = givenALiabilityStartDateOf(LocalDate.of(2028, 10, 16))
//      liabilityYear.isNotCurrentFY shouldBe false
//      liabilityYear.asString       shouldBe "2028"
//    }
//  }
//
//}
