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
import org.sonar.core.persistence.DatabaseUtils;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.BaseDao;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
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
    PreparedStatement ps = null;
    try {
      ps = connection.prepareStatement(INSERT_QUERY);
      // (project_key, snapshot_id, report_status, report_data, created_at, updated_at, started_at, finished_at)
      ps.setString(1, report.getProjectKey());
      ps.setLong(2, report.getSnapshotId());
      ps.setString(3, report.getStatus().toString());
      setReportDataStream(ps, 4, report.getData());
      ps.setTimestamp(5, dateToTimestamp(report.getCreatedAt()));
      ps.setTimestamp(6, dateToTimestamp(report.getUpdatedAt()));
      ps.setTimestamp(7, dateToTimestamp(report.getStartedAt()));
      ps.setTimestamp(8, dateToTimestamp(report.getFinishedAt()));

      ps.executeUpdate();
      connection.commit();
    } catch (SQLException e) {
      throw new IllegalStateException(String.format("Failed to insert %s in the database", report), e);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Failed to read report data of %s", report), e);
    } finally {
      DatabaseUtils.closeQuietly(ps);
    }

    return report;
  }

  private void setReportDataStream(PreparedStatement ps, int parameterIndex, @Nullable InputStream reportDataStream) throws IOException, SQLException {
    int streamSizeEstimate = 1;
    if (reportDataStream != null) {
      streamSizeEstimate = reportDataStream.available();
    }
    ps.setBinaryStream(parameterIndex, reportDataStream, streamSizeEstimate);
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
  protected Map<String, Object> getSynchronizationParams(Date date, Map<String, String> params) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void doDeleteByKey(DbSession session, String id) {
    mapper(session).delete(Long.valueOf(id));
  }
}
