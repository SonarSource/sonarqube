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
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.Severity;
import org.sonar.core.persistence.Database;
import org.sonar.core.persistence.dialect.Oracle;
import org.sonar.server.db.DatabaseMigration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Used in the Active Record Migration 401
 */
public class ConvertViolationsToIssues implements DatabaseMigration {

  private static final int GROUP_SIZE = 500;
  private static final String PROJECT_ID = "projectId";
  private static final String CREATED_AT = "createdAt";
  private static final String REVIEW_ID = "reviewId";
  private static final String SEVERITY = "severity";
  private static final String REVIEW_STATUS = "reviewStatus";
  private static final String REVIEW_MANUAL_SEVERITY = "reviewManualSeverity";
  private static final String REVIEW_SEVERITY = "reviewSeverity";
  private static final String REVIEW_UPDATED_AT = "reviewUpdatedAt";
  private static final String ROOT_PROJECT_ID = "rootProjectId";
  private static final String RULE_ID = "ruleId";
  private static final String MESSAGE = "message";
  private static final String LINE = "line";
  private static final String COST = "cost";
  private static final String CHECKSUM = "checksum";
  private static final String REVIEW_RESOLUTION = "reviewResolution";
  private static final String REVIEW_REPORTER_ID = "reviewReporterId";
  private static final String REVIEW_ASSIGNEE_ID = "reviewAssigneeId";
  private static final String REVIEW_DATA = "reviewData";
  private static final String REVIEW_MANUAL_VIOLATION = "reviewManualViolation";
  private static final String PLAN_ID = "planId";
  private static final String ISSUE_KEY = "issueKey";

  private static final String STATUS_OPEN = "OPEN";
  private static final String STATUS_CONFIRMED = "CONFIRMED";
  private static final String UPDATED_AT = "updatedAt";
  private static final String REVIEW_TEXT = "reviewText";
  private static final String USER_ID = "userId";
  private static final String SEVERITY_MAJOR = "MAJOR";

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
    private static final long ONE_YEAR = 365L * 24 * 60 * 60 * 1000;
    private String insertSql;
    private String insertChangeSql;
    private Date oneYearAgo = new Date(System.currentTimeMillis() - ONE_YEAR);
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
      String issuesColumnsWithoutId = "kee, component_id, root_component_id, rule_id, severity, manual_severity, message, line, effort_to_fix, status, resolution, " +
        "checksum, reporter, assignee, action_plan_key, issue_attributes, issue_creation_date, issue_update_date, created_at, updated_at";

      if (Oracle.ID.equals(database.getDialect().getId())) {
        insertSql = "INSERT INTO issues(id, "+ issuesColumnsWithoutId + ") " +
          " VALUES (issues_seq.nextval, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        insertChangeSql = "INSERT INTO issue_changes(id, kee, issue_key, user_login, change_type, change_data, created_at, updated_at) " +
          " VALUES (issue_changes_seq.nextval, ?, ?, ?, 'comment', ?, ?, ?)";
      } else {
        insertSql = "INSERT INTO issues("+ issuesColumnsWithoutId + ") " +
          " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        insertChangeSql = "INSERT INTO issue_changes(kee, issue_key, user_login, change_type, change_data, created_at, updated_at) VALUES (?, ?, ?, 'comment', ?, ?, ?)";
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
        Long componentId = (Long) row.get(PROJECT_ID);
        if (componentId == null) {
          continue;
        }
        String issueKey = UUID.randomUUID().toString();
        String status, severity, reporter = null;
        boolean manualSeverity;
        Object createdAt = Objects.firstNonNull(row.get(CREATED_AT), oneYearAgo);
        Object updatedAt;
        Long reviewId = (Long) row.get(REVIEW_ID);
        if (reviewId == null) {
          // violation without review
          status = STATUS_OPEN;
          manualSeverity = false;
          severity = (String) row.get(SEVERITY);
          updatedAt = createdAt;
        } else {
          // violation + review
          String reviewStatus = (String) row.get(REVIEW_STATUS);
          status = (STATUS_OPEN.equals(reviewStatus) ? STATUS_CONFIRMED : reviewStatus);
          manualSeverity = Objects.firstNonNull((Boolean) row.get(REVIEW_MANUAL_SEVERITY), false);
          severity = (String) row.get(REVIEW_SEVERITY);
          updatedAt = Objects.firstNonNull(row.get(REVIEW_UPDATED_AT), oneYearAgo);
          if ((Boolean) row.get(REVIEW_MANUAL_VIOLATION)) {
            reporter = login((Long) row.get(REVIEW_REPORTER_ID));
          }

          List<Map<String, Object>> comments = runner.query(readConnection, ReviewCommentsHandler.SQL + reviewId, new ReviewCommentsHandler());
          for (Map<String, Object> comment : comments) {
            comment.put(ISSUE_KEY, issueKey);
            allComments.add(comment);
          }
        }

        Object[] params = new Object[20];
        params[0] = issueKey;
        params[1] = componentId;
        params[2] = row.get(ROOT_PROJECT_ID);
        params[3] = row.get(RULE_ID);
        params[4] = severity;
        params[5] = manualSeverity;
        params[6] = row.get(MESSAGE);
        params[7] = row.get(LINE);
        params[8] = row.get(COST);
        params[9] = status;
        params[10] = row.get(REVIEW_RESOLUTION);
        params[11] = row.get(CHECKSUM);
        params[12] = reporter;
        params[13] = login((Long) row.get(REVIEW_ASSIGNEE_ID));
        params[14] = plan((Long) row.get(PLAN_ID));
        params[15] = row.get(REVIEW_DATA);
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
        String login = login((Long) comment.get(USER_ID));
        if (login != null) {
          Object[] params = new Object[6];
          params[0] = UUID.randomUUID().toString();
          params[1] = comment.get(ISSUE_KEY);
          params[2] = login;
          params[3] = comment.get(REVIEW_TEXT);
          params[4] = comment.get(CREATED_AT);
          params[5] = comment.get(UPDATED_AT);
          allParams.add(params);
        }
      }
      if (!allParams.isEmpty()) {
        runner.batch(writeConnection, insertChangeSql, allParams.toArray(new Object[allParams.size()][]));
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
    static final String SQL;
    static {
      StringBuilder sb = new StringBuilder("select rev.id as reviewId, s.project_id as projectId, rf.rule_id as ruleId, " +
        "  rf.failure_level as failureLevel, rf.message as message, rf.line as line, " +
        "  rf.cost as cost, rf.created_at as createdAt, rf.checksum as checksum, rev.user_id as reviewReporterId, " +
        "  rev.assignee_id as reviewAssigneeId, rev.status as reviewStatus, " +
        "  rev.severity as reviewSeverity, rev.resolution as reviewResolution, rev.manual_severity as reviewManualSeverity, " +
        "  rev.data as reviewData, rev.updated_at as reviewUpdatedAt, " +
        "  s.root_project_id as rootProjectId, rev.manual_violation as reviewManualViolation, planreviews.action_plan_id as planId " +
        " from rule_failures rf " +
        " inner join snapshots s on s.id=rf.snapshot_id " +
        " left join reviews rev on rev.rule_failure_permanent_id=rf.permanent_id " +
        " left join action_plans_reviews planreviews on planreviews.review_id=rev.id " +
        " where ");
      for (int i = 0; i < GROUP_SIZE; i++) {
        if (i > 0) {
          sb.append(" or ");
        }
        sb.append("rf.id=?");
      }
      SQL = sb.toString();
    }

    private static final Map<Integer, String> SEVERITIES = ImmutableMap.of(1, Severity.INFO, 2, Severity.MINOR, 3, Severity.MAJOR, 4, Severity.CRITICAL, 5, Severity.BLOCKER);

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(REVIEW_ID, getLong(rs, REVIEW_ID));
      map.put(PROJECT_ID, getLong(rs, PROJECT_ID));
      map.put(ROOT_PROJECT_ID, getLong(rs, ROOT_PROJECT_ID));
      map.put(RULE_ID, getLong(rs, RULE_ID));
      map.put(SEVERITY, Objects.firstNonNull(SEVERITIES.get(getInt(rs, "failureLevel")), SEVERITY_MAJOR));
      map.put(MESSAGE, rs.getString(MESSAGE));
      map.put(LINE, getInt(rs, LINE));
      map.put(COST, getDouble(rs, COST));
      map.put(CHECKSUM, rs.getString(CHECKSUM));
      map.put(CREATED_AT, rs.getTimestamp(CREATED_AT));
      map.put(REVIEW_RESOLUTION, rs.getString(REVIEW_RESOLUTION));
      map.put(REVIEW_SEVERITY, Objects.firstNonNull(rs.getString(REVIEW_SEVERITY), SEVERITY_MAJOR));
      map.put(REVIEW_STATUS, rs.getString(REVIEW_STATUS));
      map.put(REVIEW_REPORTER_ID, getLong(rs, REVIEW_REPORTER_ID));
      map.put(REVIEW_ASSIGNEE_ID, getLong(rs, REVIEW_ASSIGNEE_ID));
      map.put(REVIEW_DATA, rs.getString(REVIEW_DATA));
      map.put(REVIEW_MANUAL_SEVERITY, rs.getBoolean(REVIEW_MANUAL_SEVERITY));
      map.put(REVIEW_UPDATED_AT, rs.getTimestamp(REVIEW_UPDATED_AT));
      map.put(REVIEW_MANUAL_VIOLATION, rs.getBoolean(REVIEW_MANUAL_VIOLATION));
      map.put(PLAN_ID, getLong(rs, PLAN_ID));
      return map;
    }
  }

  private static class ReviewCommentsHandler extends AbstractListHandler<Map<String, Object>> {
    static final String SQL = "select created_at as createdAt, updated_at as updatedAt, user_id as userId, review_text as reviewText from review_comments where review_id=";

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(CREATED_AT, rs.getTimestamp(CREATED_AT));
      map.put(UPDATED_AT, rs.getTimestamp(UPDATED_AT));
      map.put(USER_ID, getLong(rs, USER_ID));
      map.put(REVIEW_TEXT, rs.getString(REVIEW_TEXT));
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
