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
import org.sonar.server.db.BaseDao;

import javax.annotation.CheckForNull;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

public class AnalysisReportDao extends BaseDao<AnalysisReportMapper, AnalysisReportDto, String> implements DaoComponent {

  private System2 system2;

  public AnalysisReportDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  public AnalysisReportDao(System2 system2) {
    super(AnalysisReportMapper.class, system2);
    this.system2 = system2;
  }

  /**
   * startup task use only
   */
  public void cleanWithUpdateAllToPendingStatus(DbSession session) {
    mapper(session).cleanWithUpdateAllToPendingStatus(PENDING, new Date(system2.now()));
  }

  /**
   * startup task use only
   */
  public void cleanWithTruncate(DbSession session) {
    mapper(session).cleanWithTruncate();
  }

  public List<AnalysisReportDto> findByProjectKey(DbSession session, String projectKey) {
    return mapper(session).selectByProjectKey(projectKey);
  }

  public AnalysisReportDto getNextAvailableReport(DbSession session) {
    // TODO to improve – the query should return one element or null
    List<AnalysisReportDto> reports = mapper(session).selectNextAvailableReport(PENDING, WORKING);

    if (reports.isEmpty()) {
      return null;
    }

    return reports.get(0);
  }

  @VisibleForTesting
  AnalysisReportDto getById(DbSession session, Long id) {
    return mapper(session).selectById(id);
  }

  @CheckForNull
  public AnalysisReportDto bookAnalysisReport(DbSession session, AnalysisReportDto report) {
    checkNotNull(report.getId());

    int nbOfReportBooked = mapper(session).updateWithBookingReport(report.getId(), new Date(system2.now()), PENDING, WORKING);

    if (nbOfReportBooked == 0) {
      return null;
    }

    return mapper(session).selectById(report.getId());
  }

  @Override
  protected AnalysisReportDto doGetNullableByKey(DbSession session, String projectKey) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected AnalysisReportDto doUpdate(DbSession session, AnalysisReportDto report) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected AnalysisReportDto doInsert(DbSession session, AnalysisReportDto report) {
    mapper(session).insert(report);
    return report;
  }

  @Override
  protected String getSynchronizationStatementName() {
    throw new UnsupportedOperationException();
  }

  @Override
  protected Map<String, Object> getSynchronizationParams(Date date, Map<String, String> params) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void doDeleteByKey(DbSession session, String id) {
    mapper(session).delete(Long.valueOf(id));
  }
}
