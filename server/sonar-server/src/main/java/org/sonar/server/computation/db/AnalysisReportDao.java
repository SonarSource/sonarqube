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
import com.google.common.base.Charsets;
import org.sonar.api.utils.System2;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

import javax.annotation.CheckForNull;

import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

public class AnalysisReportDao extends BaseDao<AnalysisReportMapper, AnalysisReportDto, String> implements DaoComponent {

  private static final String INSERT_QUERY = "insert into analysis_reports\n" +
    "    (project_key, snapshot_id, report_status, report_data, created_at, updated_at, started_at, finished_at)\n" +
    "    values (?, ?, ?, ?, ?, ?, ?, ?)";

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

  public List<AnalysisReportDto> findAll(DbSession session) {
    return mapper(session).selectAll();
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
    Connection connection = session.getConnection();
    try {
      PreparedStatement preparedStatement = connection.prepareStatement(INSERT_QUERY);
      // (project_key, snapshot_id, report_status, report_data, created_at, updated_at, started_at, finished_at)
      preparedStatement.setString(1, report.getProjectKey());
      preparedStatement.setLong(2, report.getSnapshotId());
      preparedStatement.setString(3, report.getStatus().toString());
      InputStreamReader inputStreamReader = null;
      if (report.getData() != null) {
        inputStreamReader = new InputStreamReader(report.getData(), Charsets.UTF_8);
      }
      preparedStatement.setCharacterStream(4, inputStreamReader);
      preparedStatement.setTimestamp(5, dateToTimestamp(report.getCreatedAt()));
      preparedStatement.setTimestamp(6, dateToTimestamp(report.getUpdatedAt()));
      preparedStatement.setTimestamp(7, dateToTimestamp(report.getStartedAt()));
      preparedStatement.setTimestamp(8, dateToTimestamp(report.getFinishedAt()));

      preparedStatement.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      e.printStackTrace();
    }

    return report;
  }

  private Timestamp dateToTimestamp(Date date) {
    if (date == null) {
      return null;
    }

    return new Timestamp(date.getTime());
  }

  @Override
  protected String getSynchronizationStatementName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public AnalysisReportDto insert(DbSession session, AnalysisReportDto item) {
    return super.insert(session, item);
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
