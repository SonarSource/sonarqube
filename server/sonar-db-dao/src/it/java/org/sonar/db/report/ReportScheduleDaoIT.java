/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.db.report;

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.utils.System2;
import org.sonar.db.DbTester;

import static org.assertj.core.api.Assertions.assertThat;

class ReportScheduleDaoIT {

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final ReportScheduleDao underTest = db.getDbClient().reportScheduleDao();

  @Test
  void upsert_shouldPersistCorrectDataOnBranch() {
    ReportScheduleDto reportScheduleDto = newReportScheduleDto("uuid").setBranchUuid("branch_uuid").setLastSendTimeInMs(1L);
    underTest.upsert(db.getSession(), reportScheduleDto);

    List<ReportScheduleDto> actual = underTest.selectAll(db.getSession());
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0)).extracting(ReportScheduleDto::getBranchUuid, ReportScheduleDto::getLastSendTimeInMs).containsExactly(
      "branch_uuid", 1L);
  }

  @NotNull
  private static ReportScheduleDto newReportScheduleDto(String uuid) {
    return new ReportScheduleDto().setUuid(uuid);
  }

  @Test
  void upsert_shouldPersistCorrectDataOnPortfolio() {
    ReportScheduleDto reportScheduleDto = newReportScheduleDto("uuid").setPortfolioUuid("portfolio_uuid").setLastSendTimeInMs(1L);
    underTest.upsert(db.getSession(), reportScheduleDto);

    List<ReportScheduleDto> actual = underTest.selectAll(db.getSession());
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0)).extracting(ReportScheduleDto::getPortfolioUuid, ReportScheduleDto::getLastSendTimeInMs).containsExactly(
      "portfolio_uuid", 1L);
  }

  @Test
  void upsert_shouldUpdateLastSendTimeInMsOnBranch() {
    ReportScheduleDto reportScheduleDto = newReportScheduleDto("uuid").setBranchUuid("branch_uuid").setLastSendTimeInMs(1L);
    underTest.upsert(db.getSession(), reportScheduleDto);

    reportScheduleDto.setLastSendTimeInMs(2L);
    underTest.upsert(db.getSession(), reportScheduleDto);

    List<ReportScheduleDto> actual = underTest.selectAll(db.getSession());
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0)).extracting(ReportScheduleDto::getBranchUuid, ReportScheduleDto::getLastSendTimeInMs).containsExactly(
      "branch_uuid", 2L);

  }

  @Test
  void upsert_shouldUpdateLastSendTimeInMsOnPortfolio() {
    ReportScheduleDto reportScheduleDto = newReportScheduleDto("uuid").setPortfolioUuid("portfolio_uuid").setLastSendTimeInMs(1L);
    underTest.upsert(db.getSession(), reportScheduleDto);

    reportScheduleDto.setLastSendTimeInMs(2L);
    underTest.upsert(db.getSession(), reportScheduleDto);

    List<ReportScheduleDto> actual = underTest.selectAll(db.getSession());
    assertThat(actual).hasSize(1);
    assertThat(actual.get(0)).extracting(ReportScheduleDto::getPortfolioUuid, ReportScheduleDto::getLastSendTimeInMs).containsExactly(
      "portfolio_uuid", 2L);
  }

  @Test
  void selectByBranch_shouldRetrieveCorrectInformationOnBranch() {
    ReportScheduleDto reportScheduleDto = newReportScheduleDto("uuid").setBranchUuid("branch_uuid").setLastSendTimeInMs(1L);
    underTest.upsert(db.getSession(), reportScheduleDto);
    assertThat(underTest.selectByBranch(db.getSession(), "branch_uuid"))
      .isPresent().get()
      .extracting(ReportScheduleDto::getBranchUuid, ReportScheduleDto::getLastSendTimeInMs).containsExactly("branch_uuid", 1L);
  }

  @Test
  void selectByPortfolio_shouldRetrieveCorrectInformationOnPortfolio() {
    ReportScheduleDto reportScheduleDto = newReportScheduleDto("uuid").setPortfolioUuid("portfolio_uuid").setLastSendTimeInMs(1L);
    underTest.upsert(db.getSession(), reportScheduleDto);
    assertThat(underTest.selectByPortfolio(db.getSession(), "portfolio_uuid"))
      .isPresent().get()
      .extracting(ReportScheduleDto::getPortfolioUuid, ReportScheduleDto::getLastSendTimeInMs).containsExactly("portfolio_uuid", 1L);
  }
}
