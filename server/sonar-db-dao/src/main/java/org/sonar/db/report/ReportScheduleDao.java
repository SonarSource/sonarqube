/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Optional;
import org.sonar.db.Dao;
import org.sonar.db.DbSession;

public class ReportScheduleDao implements Dao {
  public Optional<ReportScheduleDto> selectByBranch(DbSession dbSession, String branchUuid) {
    return mapper(dbSession).selectByBranch(branchUuid);
  }

  public Optional<ReportScheduleDto> selectByPortfolio(DbSession dbSession, String portfolioUuid) {
    return mapper(dbSession).selectByPortfolio(portfolioUuid);
  }

  public void upsert(DbSession dbSession, ReportScheduleDto reportScheduleDto) {
    if (!mapper(dbSession).update(reportScheduleDto)) {
      mapper(dbSession).insert(reportScheduleDto);
    }
  }

  private static ReportScheduleMapper mapper(DbSession dbSession) {
    return dbSession.getMapper(ReportScheduleMapper.class);
  }

  public List<ReportScheduleDto> selectAll(DbSession dbSession) {
    return mapper(dbSession).selectAll();
  }
}
