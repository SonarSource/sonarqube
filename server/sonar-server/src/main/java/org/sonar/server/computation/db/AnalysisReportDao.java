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
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.ZipUtils;
import org.sonar.core.computation.db.AnalysisReportDto;
import org.sonar.core.computation.db.AnalysisReportMapper;
import org.sonar.core.persistence.DaoComponent;
import org.sonar.core.persistence.DatabaseUtils;
import org.sonar.core.persistence.DbSession;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import static org.sonar.core.computation.db.AnalysisReportDto.Status.PENDING;
import static org.sonar.core.computation.db.AnalysisReportDto.Status.WORKING;

public class AnalysisReportDao implements DaoComponent {

  private System2 system2;

  public AnalysisReportDao() {
    this(System2.INSTANCE);
  }

  @VisibleForTesting
  AnalysisReportDao(System2 system2) {
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

    Connection connection = session.getConnection();
    PreparedStatement ps = null;
    ResultSet generatedIdRs = null;
    try {
      ps = connection.prepareStatement(
        "insert into analysis_reports " +
          " (project_key, snapshot_id, report_status, report_data, created_at, updated_at, started_at, finished_at)" +
          " values (?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
      ps.setString(1, report.getProjectKey());
      ps.setLong(2, report.getSnapshotId());
      ps.setString(3, report.getStatus().toString());
      setData(ps, 4, report.getData());
      ps.setLong(5, report.getCreatedAt());
      setLong(ps, 6, report.getUpdatedAt());
      setLong(ps, 7, report.getStartedAt());
      setLong(ps, 8, report.getFinishedAt());

      if (ps.executeUpdate() == 1) {
        generatedIdRs = ps.getGeneratedKeys();
        generatedIdRs.next();
        report.setId(generatedIdRs.getLong(1));
      }
      connection.commit();
    } catch (SQLException | IOException e) {
      throw new IllegalStateException(String.format("Failed to insert %s in the database", report), e);
    } finally {
      DatabaseUtils.closeQuietly(generatedIdRs);
      DatabaseUtils.closeQuietly(ps);
    }

    return report;
  }

  private void setLong(PreparedStatement ps, int index, @Nullable Long time) throws SQLException {
    if (time == null) {
      ps.setNull(index, Types.BIGINT);
    } else {
      ps.setLong(index, time);
    }
  }

  private void setData(PreparedStatement ps, int parameterIndex, @Nullable InputStream reportDataStream) throws IOException, SQLException {
    if (reportDataStream == null) {
      ps.setBytes(parameterIndex, null);
    } else {
      ps.setBytes(parameterIndex, ByteStreams.toByteArray(reportDataStream));
    }
  }

  @CheckForNull
  public void selectAndDecompressToDir(DbSession session, long id, File toDir) {
    Connection connection = session.getConnection();
    InputStream stream = null;
    PreparedStatement ps = null;
    ResultSet rs = null;
    try {
      ps = connection.prepareStatement("select report_data from analysis_reports where id=?");
      ps.setLong(1, id);

      rs = ps.executeQuery();
      if (rs.next()) {
        stream = rs.getBinaryStream(1);
        if (stream != null) {
          ZipUtils.unzip(stream, toDir);
        }
      }
      // TODO what to do if id not found or no stream ?
    } catch (SQLException e) {
      throw new IllegalStateException(String.format("Failed to read report '%d' in the database", id), e);
    } catch (IOException e) {
      throw new IllegalStateException(String.format("Failed to decompress report '%d'", id), e);
    } finally {
      IOUtils.closeQuietly(stream);
      DatabaseUtils.closeQuietly(rs);
      DatabaseUtils.closeQuietly(ps);
    }
  }

  public void delete(DbSession session, long id) {
    mapper(session).delete(id);
  }

  private AnalysisReportMapper mapper(DbSession session) {
    return session.getMapper(AnalysisReportMapper.class);
  }
}
