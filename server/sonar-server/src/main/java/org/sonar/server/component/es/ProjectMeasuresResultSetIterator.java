/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

package org.sonar.server.component.es;

import com.google.common.base.Joiner;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Scopes;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.ResultSetIterator;

import static org.sonar.api.measures.Metric.ValueType.DATA;
import static org.sonar.api.measures.Metric.ValueType.DISTRIB;
import static org.sonar.db.DatabaseUtils.repeatCondition;

public class ProjectMeasuresResultSetIterator extends ResultSetIterator<ProjectMeasuresDoc> {

  private static final Joiner METRICS_JOINER = Joiner.on("','");

  private static final String SQL_PROJECTS = "SELECT p.uuid, p.kee, p.name, s.uuid, s.created_at FROM projects p " +
    "LEFT OUTER JOIN snapshots s ON s.component_uuid=p.uuid AND s.islast=? " +
    "WHERE p.enabled=? AND p.scope=? AND p.qualifier=?";

  private static final String DATE_FILTER = " AND s.created_at>?";

  private static final String PROJECT_FILTER = " AND p.uuid=?";

  private static final String SQL_METRICS = "SELECT m.id, m.name FROM metrics m " +
    "WHERE m.val_type NOT IN ('" + METRICS_JOINER.join(DATA.name(), DISTRIB.name()) + "') " +
    "AND m.enabled=? AND m.hidden=?";

  private static final String SQL_MEASURES = "SELECT pm.metric_id, pm.value, pm.variation_value_1 FROM project_measures pm " +
    "WHERE pm.component_uuid = ? AND pm.analysis_uuid = ? " +
    "AND pm.metric_id IN ({metricIds}) " +
    "AND (pm.value IS NOT NULL OR pm.variation_value_1 IS NOT NULL) " +
    "AND pm.person_id IS NULL ";

  private final DbSession dbSession;
  private final Map<Long, String> metrics;

  private ProjectMeasuresResultSetIterator(PreparedStatement stmt, DbSession dbSession, Map<Long, String> metrics) throws SQLException {
    super(stmt);
    this.dbSession = dbSession;
    this.metrics = metrics;
  }

  static ProjectMeasuresResultSetIterator create(DbClient dbClient, DbSession session, long afterDate, @Nullable String projectUuid) {
    try {
      PreparedStatement projectsStatement = createProjectsStatement(dbClient, session, afterDate, projectUuid);
      Map<Long, String> metricIds = selectMetricIds(session);
      return new ProjectMeasuresResultSetIterator(projectsStatement, session, metricIds);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select all project measures", e);
    }
  }

  private static PreparedStatement createProjectsStatement(DbClient dbClient, DbSession session, long afterDate, @Nullable String projectUuid) {
    try {
      String sql = SQL_PROJECTS;
      sql += afterDate <= 0L ? "" : DATE_FILTER;
      sql += projectUuid == null ? "" : PROJECT_FILTER;
      PreparedStatement stmt = dbClient.getMyBatis().newScrollingSelectStatement(session, sql);
      stmt.setBoolean(1, true);
      stmt.setBoolean(2, true);
      stmt.setString(3, Scopes.PROJECT);
      stmt.setString(4, Qualifiers.PROJECT);
      int index = 5;
      if (afterDate > 0L) {
        stmt.setLong(index, afterDate);
        index++;
      }
      if (projectUuid != null) {
        stmt.setString(index, projectUuid);
      }
      return stmt;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all project measures", e);
    }
  }

  private static Map<Long, String> selectMetricIds(DbSession session) {
    Map<Long, String> metrics = new HashMap<>();
    try (PreparedStatement stmt = createMetricsStatement(session);
      ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        metrics.put(rs.getLong(1), rs.getString(2));
      }
      return metrics;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select all metrics", e);
    }
  }

  private static PreparedStatement createMetricsStatement(DbSession session) throws SQLException {
    PreparedStatement stmt = session.getConnection().prepareStatement(SQL_METRICS);
    stmt.setBoolean(1, true);
    stmt.setBoolean(2, false);
    return stmt;
  }

  @Override
  protected ProjectMeasuresDoc read(ResultSet rs) throws SQLException {
    String projectUuid = rs.getString(1);
    ProjectMeasuresDoc doc = new ProjectMeasuresDoc()
      .setId(projectUuid)
      .setKey(rs.getString(2))
      .setName(rs.getString(3))
      .setMeasuresFromMap(selectMeasures(projectUuid, rs.getString(4)));
    long analysisDate = rs.getLong(5);
    doc.setAnalysedAt(rs.wasNull() ? null : new Date(analysisDate));
    return doc;
  }

  private Map<String, Object> selectMeasures(String projectUuid, String analysisUuid) {
    Map<String, Object> measures = new HashMap<>();
    try (PreparedStatement stmt = createMeasuresStatement(projectUuid, analysisUuid);
      ResultSet rs = stmt.executeQuery()) {
      while (rs.next()) {
        String metricKey = metrics.get(rs.getLong(1));
        Double value = metricKey.startsWith("new_") ? getDouble(rs, 3) : getDouble(rs, 2);
        measures.put(metricKey, value);
      }
      return measures;
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to execute request to select measures", e);
    }
  }

  private PreparedStatement createMeasuresStatement(String projectUuid, String analysisUuid) throws SQLException {
    String sql = StringUtils.replace(SQL_MEASURES, "{metricIds}", repeatCondition("?", metrics.size(), ","));
    PreparedStatement stmt = dbSession.getConnection().prepareStatement(sql);
    stmt.setString(1, projectUuid);
    stmt.setString(2, analysisUuid);
    int index = 3;
    for (Long metricId : metrics.keySet()) {
      stmt.setLong(index, metricId);
      index++;
    }
    return stmt;
  }

  private static Double getDouble(ResultSet rs, int index) {
    try {
      Double value = rs.getDouble(index);
      if (!rs.wasNull()) {
        return value;
      }
      throw new IllegalStateException("No value");
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to get double value", e);
    }
  }

}
