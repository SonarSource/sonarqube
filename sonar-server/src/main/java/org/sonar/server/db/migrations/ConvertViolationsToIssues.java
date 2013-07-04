/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.server.db.migrations;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.apache.commons.lang.time.DateUtils;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.Oracle;
import org.sonar.server.db.DatabaseMigration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Used in the Active Record Migration 401
 */
public class ConvertViolationsToIssues implements DatabaseMigration {

  private static int GROUP_SIZE = 500;
  private QueryRunner runner = new QueryRunner();

  @Override
  public void execute(Database db) {
    Connection readConnection = null, writeConnection = null;
    try {
      readConnection = db.getDataSource().getConnection();
      writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);
      truncateIssueTables(writeConnection);
      convertViolations(readConnection, new Converter(db, runner, readConnection, writeConnection));
    } catch (Exception e) {
      throw new IllegalStateException("Fail to convert violations to issues", e);
    } finally {
      DbUtils.closeQuietly(readConnection);
      DbUtils.closeQuietly(writeConnection);
    }
  }

  private void truncateIssueTables(Connection writeConnection) throws SQLException {
    // lower-case table names for SQLServer....
    runner.update(writeConnection, "TRUNCATE TABLE issues");
    runner.update(writeConnection, "TRUNCATE TABLE issue_changes");
    writeConnection.commit();
  }

  private void convertViolations(Connection readConnection, Converter converter) throws SQLException {
    runner.query(readConnection, "select id from rule_failures", new ViolationIdHandler(converter));
  }


  /**
   * Browse violation ids and process them by groups of {@link #GROUP_SIZE}.
   */
  private static class ViolationIdHandler implements ResultSetHandler {
    private Converter converter;
    private Object[] violationIds = new Object[GROUP_SIZE];
    private int cursor = 0;

    private ViolationIdHandler(Converter converter) {
      this.converter = converter;
    }

    @Override
    public Object handle(ResultSet rs) throws SQLException {
      int total = 0;
      while (rs.next()) {
        long violationId = rs.getLong(1);
        violationIds[cursor++] = violationId;
        if (cursor == GROUP_SIZE) {
          convert();
          Arrays.fill(violationIds, -1L);
          cursor = 0;
        }
        total++;
      }
      if (cursor > 0) {
        convert();
      }
      LoggerFactory.getLogger(getClass()).info(String.format("%d violations migrated to issues", total));
      return null;
    }

    private void convert() throws SQLException {
      if (cursor > 0) {
        converter.convert(violationIds);
      }
    }
  }

  private static class Converter {
    private String insertSql;
    private Date oneYearAgo = DateUtils.addYears(new Date(), -1);
    private QueryRunner runner;
    private Connection readConnection, writeConnection;
    private Map<Long, String> loginsByUserId;
    private Map<Long, String> plansById;

    private Converter(Database database, QueryRunner runner, Connection readConnection, Connection writeConnection) throws SQLException {
      this.runner = runner;
      this.readConnection = readConnection;
      this.writeConnection = writeConnection;
      initInsertSql(database);
      initUsers();
      initPlans();
    }

    private void initInsertSql(Database database) {
      if (Oracle.ID.equals(database.getDialect().getId())) {
        insertSql = "INSERT INTO issues(id, kee, component_id, root_component_id, rule_id, severity, manual_severity, message, line, effort_to_fix, status, resolution, " +
          " checksum, reporter, assignee, action_plan_key, issue_attributes, issue_creation_date, issue_update_date, created_at, updated_at) " +
          " VALUES (issues_seq.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      } else {
        insertSql = "INSERT INTO issues(kee, component_id, root_component_id, rule_id, severity, manual_severity, message, line, effort_to_fix, status, resolution, " +
          " checksum, reporter, assignee, action_plan_key, issue_attributes, issue_creation_date, issue_update_date, created_at, updated_at) " +
          " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      }
    }


    private void initUsers() throws SQLException {
      loginsByUserId = selectLongString("select id,login from users");
    }

    private void initPlans() throws SQLException {
      plansById = selectLongString("select id,kee from action_plans");
    }

    private Map<Long, String> selectLongString(String sql) throws SQLException {
      return runner.query(readConnection, sql, new ResultSetHandler<Map<Long, String>>() {
        @Override
        public Map<Long, String> handle(ResultSet rs) throws SQLException {
          Map<Long, String> map = Maps.newHashMap();
          while (rs.next()) {
            map.put(rs.getLong(1), rs.getString(2));
          }
          return map;
        }
      });
    }

    /**
     * Convert a group of maximum {@link #GROUP_SIZE} violations to issues
     */
    void convert(Object[] violationIds) throws SQLException {
      List<Map<String, Object>> rows = runner.query(readConnection, ViolationHandler.SQL, new ViolationHandler(), violationIds);
      List<Object[]> allParams = Lists.newArrayList();
      List<Map<String, Object>> allComments = Lists.newArrayList();

      for (Map<String, Object> row : rows) {
        Long componentId = (Long) row.get("projectId");
        if (componentId == null) {
          continue;
        }
        String issueKey = UUID.randomUUID().toString();
        String status, severity, reporter = null;
        boolean manualSeverity;
        Date createdAt = Objects.firstNonNull((Date) row.get("createdAt"), oneYearAgo);
        Date updatedAt;
        Long reviewId = (Long) row.get("reviewId");
        if (reviewId == null) {
          // violation without review
          status = "OPEN";
          manualSeverity = false;
          severity = (String) row.get("severity");
          updatedAt = createdAt;
        } else {
          // violation + review
          String reviewStatus = (String) row.get("reviewStatus");
          status = ("OPEN".equals(reviewStatus) ? "CONFIRMED" : reviewStatus);
          manualSeverity = Objects.firstNonNull((Boolean) row.get("reviewManualSeverity"), false);
          severity = (String) row.get("reviewSeverity");
          updatedAt = Objects.firstNonNull((Date) row.get("reviewUpdatedAt"), oneYearAgo);
          if ((Boolean) row.get("reviewManualViolation")) {
            reporter = login((Long) row.get("reviewReporterId"));
          }

          List<Map<String, Object>> comments = runner.query(readConnection, ReviewCommentsHandler.SQL + reviewId, new ReviewCommentsHandler());
          for (Map<String, Object> comment : comments) {
            comment.put("issueKey", issueKey);
            allComments.add(comment);
          }
        }

        Object[] params = new Object[20];
        params[0] = issueKey;
        params[1] = componentId;
        params[2] = row.get("rootProjectId");
        params[3] = row.get("ruleId");
        params[4] = severity;
        params[5] = manualSeverity;
        params[6] = row.get("message");
        params[7] = row.get("line");
        params[8] = row.get("cost");
        params[9] = status;
        params[10] = row.get("reviewResolution");
        params[11] = row.get("checksum");
        params[12] = reporter;
        params[13] = login((Long) row.get("reviewAssigneeId"));
        params[14] = plan((Long) row.get("planId"));
        params[15] = row.get("reviewData");
        params[16] = createdAt;
        params[17] = updatedAt;
        params[18] = createdAt;
        params[19] = updatedAt;

        allParams.add(params);
      }
      runner.batch(writeConnection, insertSql, allParams.toArray(new Object[allParams.size()][]));
      writeConnection.commit();

      insertComments(writeConnection, allComments);
    }

    private void insertComments(Connection writeConnection, List<Map<String, Object>> comments) throws SQLException {
      List<Object[]> allParams = Lists.newArrayList();

      for (Map<String, Object> comment : comments) {
        String login = login((Long) comment.get("userId"));
        if (login != null) {
          Object[] params = new Object[6];
          params[0] = UUID.randomUUID().toString();
          params[1] = comment.get("issueKey");
          params[2] = login;
          params[3] = comment.get("reviewText");
          params[4] = comment.get("createdAt");
          params[5] = comment.get("updatedAt");
          allParams.add(params);
        }
      }
      if (!allParams.isEmpty()) {
        runner.batch(writeConnection, "INSERT INTO issue_changes(kee, issue_key, user_login, change_type, change_data, created_at, updated_at) VALUES (?, ?, ?, 'comment', ?, ?, ?)", allParams.toArray(new Object[allParams.size()][]));
        writeConnection.commit();
      }
    }

    @CheckForNull
    private String login(@Nullable Long userId) {
      if (userId != null) {
        return loginsByUserId.get(userId);
      }
      return null;
    }

    @CheckForNull
    private String plan(@Nullable Long planId) {
      if (planId != null) {
        return plansById.get(planId);
      }
      return null;
    }
  }


  private static class ViolationHandler extends AbstractListHandler<Map<String, Object>> {
    private static String SQL = "select rev.id as reviewId, s.project_id as projectId, rf.rule_id as ruleId, rf.failure_level as failureLevel, rf.message as message, rf.line as line, " +
      "  rf.cost as cost, rf.created_at as createdAt, rf.checksum as checksum, rev.user_id as reviewReporterId, rev.assignee_id as reviewAssigneeId, rev.status as reviewStatus, " +
      "  rev.severity as reviewSeverity, rev.resolution as reviewResolution, rev.manual_severity as reviewManualSeverity, rev.data as reviewData, rev.updated_at as reviewUpdatedAt, " +
      "  s.root_project_id as rootProjectId, rev.manual_violation as reviewManualViolation, planreviews.action_plan_id as planId " +
      " from rule_failures rf " +
      " inner join snapshots s on s.id=rf.snapshot_id " +
      " left join reviews rev on rev.rule_failure_permanent_id=rf.permanent_id " +
      " left join action_plans_reviews planreviews on planreviews.review_id=rev.id " +
      " where ";

    static {
      for (int i = 0; i < GROUP_SIZE; i++) {
        if (i > 0) {
          SQL += " or ";
        }
        SQL += "rf.id=?";
      }
    }

    private static Map<Integer, String> SEVERITIES = ImmutableMap.of(1, Severity.INFO, 2, Severity.MINOR, 3, Severity.MAJOR, 4, Severity.CRITICAL, 5, Severity.BLOCKER);

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put("reviewId", getLong(rs, "reviewId"));
      map.put("projectId", getLong(rs, "projectId"));
      map.put("rootProjectId", getLong(rs, "rootProjectId"));
      map.put("ruleId", getLong(rs, "ruleId"));
      map.put("severity", Objects.firstNonNull(SEVERITIES.get(getInt(rs, "failureLevel")), "MAJOR"));
      map.put("message", rs.getString("message"));
      map.put("line", getInt(rs, "line"));
      map.put("cost", getDouble(rs, "cost"));
      map.put("checksum", rs.getString("checksum"));
      map.put("createdAt", rs.getTimestamp("createdAt"));
      map.put("reviewResolution", rs.getString("reviewResolution"));
      map.put("reviewSeverity", Objects.firstNonNull(rs.getString("reviewSeverity"), "MAJOR"));
      map.put("reviewStatus", rs.getString("reviewStatus"));
      map.put("reviewReporterId", getLong(rs, "reviewReporterId"));
      map.put("reviewAssigneeId", getLong(rs, "reviewAssigneeId"));
      map.put("reviewData", rs.getString("reviewData"));
      map.put("reviewManualSeverity", rs.getBoolean("reviewManualSeverity"));
      map.put("reviewUpdatedAt", rs.getTimestamp("reviewUpdatedAt"));
      map.put("reviewManualViolation", rs.getBoolean("reviewManualViolation"));
      map.put("planId", getLong(rs, "planId"));
      return map;
    }
  }

  private static class ReviewCommentsHandler extends AbstractListHandler<Map<String, Object>> {
    static String SQL = "select created_at as createdAt, updated_at as updatedAt, user_id as userId, review_text as reviewText from review_comments where review_id=";

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put("createdAt", rs.getTimestamp("createdAt"));
      map.put("updatedAt", rs.getTimestamp("updatedAt"));
      map.put("userId", getLong(rs, "userId"));
      map.put("reviewText", rs.getString("reviewText"));
      return map;
    }
  }

  @CheckForNull
  static Long getLong(ResultSet rs, String columnName) throws SQLException {
    long l = rs.getLong(columnName);
    return rs.wasNull() ? null : l;
  }

  @CheckForNull
  static Double getDouble(ResultSet rs, String columnName) throws SQLException {
    double d = rs.getDouble(columnName);
    return rs.wasNull() ? null : d;
  }

  @CheckForNull
  static Integer getInt(ResultSet rs, String columnName) throws SQLException {
    int i = rs.getInt(columnName);
    return rs.wasNull() ? null : i;
  }


}
