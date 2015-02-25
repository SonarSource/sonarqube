/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.computation.db;

import com.google.common.annotations.VisibleForTesting;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;

import javax.annotation.CheckForNull;

import java.util.List;

import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

public class AnalysisReportDao implements DaoComponent {

  private System2 system2;

  public AnalysisReportDao() {
    this(System2.INSTANCE);
  }

  public AnalysisReportDao(System2 system2) {
    this.system2 = system2;
  }

  /**
   * Update all rows with: STATUS='PENDING', STARTED_AT=NULL, UPDATED_AT={now}
   */
  public void resetAllToPendingStatus(DbSession session) {
    mapper(session).resetAllToPendingStatus(system2.now());
  }

  public void truncate(DbSession session) {
    mapper(session).truncate();
  }

  public List<AnalysisReportDto> selectByProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectByProjectKey(projectKey);
  }

  @VisibleForTesting
  AnalysisReportDto selectById(DbSession session, long id) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public AnalysisReportDto pop(DbSession session) {
    List<Long> reportIds = mapper(session).selectAvailables(PENDING, WORKING);
    if (reportIds.isEmpty()) {
      return null;
    }

    long reportId = reportIds.get(0);
    return tryToPop(session, reportId);
  }

  @VisibleForTesting
  AnalysisReportDto tryToPop(DbSession session, long reportId) {
    AnalysisReportMapper mapper = mapper(session);
    int nbOfReportBooked = mapper.updateWithBookingReport(reportId, system2.now(), PENDING, WORKING);
    if (nbOfReportBooked == 0) {
      return null;
    }

    AnalysisReportDto result = mapper.selectById(reportId);
    session.commit();
    return result;
  }

  public List<AnalysisReportDto> selectAll(DbSession session) {
    return mapper(session).selectAll();
  }

  public AnalysisReportDto insert(DbSession session, AnalysisReportDto report) {
    report.setCreatedAt(system2.now());
    report.setUpdatedAt(system2.now());
    mapper(session).insert(report);
    return report;
  }

  public void delete(DbSession session, long id) {
    mapper(session).delete(id);
  }

  private AnalysisReportMapper mapper(DbSession session) {
    return session.getMapper(AnalysisReportMapper.class);
  }
}
