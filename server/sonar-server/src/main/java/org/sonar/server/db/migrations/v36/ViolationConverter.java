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
package org.sonar.server.db.migrations.v36;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.AbstractListHandler;
import org.sonar.api.rule.Severity;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.SqlUtil;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

class ViolationConverter implements Callable<Object> {

  private static final long ONE_YEAR = 365L * 24 * 60 * 60 * 1000;
  private static final Date ONE_YEAR_AGO = new Date(System.currentTimeMillis() - ONE_YEAR);

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

  private static final String SQL_ISSUE_COLUMNS = "kee, component_id, root_component_id, rule_id, severity, manual_severity, message, line, effort_to_fix, status, resolution, " +
    "checksum, reporter, assignee, action_plan_key, issue_attributes, issue_creation_date, issue_update_date, created_at, updated_at";

  private static final String SQL_INSERT_ISSUE = "INSERT INTO issues(" + SQL_ISSUE_COLUMNS + ")" +
    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

  private static final String SQL_INSERT_ISSUE_CHANGE = "INSERT INTO issue_changes(kee, issue_key, user_login, change_type, change_data, created_at, updated_at)" +
    " VALUES (?, ?, ?, 'comment', ?, ?, ?)";

  private static final String SQL_DELETE_RULE_FAILURES;

  static {
    StringBuilder sb = new StringBuilder("delete from rule_failures where ");
    for (int i = 0; i < Referentials.VIOLATION_GROUP_SIZE; i++) {
      if (i > 0) {
        sb.append(" or ");
      }
      sb.append("id=?");
    }
    SQL_DELETE_RULE_FAILURES = sb.toString();
  }

  static final String SQL_SELECT_RULE_FAILURES;

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
    for (int i = 0; i < Referentials.VIOLATION_GROUP_SIZE; i++) {
      if (i > 0) {
        sb.append(" or ");
      }
      sb.append("rf.id=?");
    }
    SQL_SELECT_RULE_FAILURES = sb.toString();
  }

  private final Database db;
  private final Referentials referentials;
  private final Progress progress;

  ViolationConverter(Referentials referentials, Database db, Progress progress) {
    this.referentials = referentials;
    this.db = db;
    this.progress = progress;
  }

  @Override
  public Object call() throws SQLException {
    // For each group of 1000 violation ids:
    // - load related violations, reviews and action plans
    // - in a transaction
    // -- insert issues
    // -- insert issue_changes if there are review comments
    // -- delete violations

    Long[] violationIds = referentials.pollGroupOfViolationIds();
    while (violationIds.length > 0) {
      List<Map<String, Object>> rows = selectRows(violationIds);
      convert(rows, violationIds);

      violationIds = referentials.pollGroupOfViolationIds();
    }
    return null;
  }

  private List<Map<String, Object>> selectRows(Long[] violationIds) throws SQLException {
    Connection readConnection = null;
    try {
      readConnection = db.getDataSource().getConnection();
      ViolationHandler violationHandler = new ViolationHandler();
      return new QueryRunner().query(readConnection, SQL_SELECT_RULE_FAILURES, violationHandler, violationIds);

    } finally {
      DbUtils.closeQuietly(readConnection);
    }
  }

  private void convert(List<Map<String, Object>> rows, Long[] violationIds) throws SQLException {
    Connection readConnection = null;
    Connection writeConnection = null;
    try {
      readConnection = db.getDataSource().getConnection();
      writeConnection = db.getDataSource().getConnection();
      writeConnection.setAutoCommit(false);

      List<Object[]> allParams = Lists.newArrayList();
      List<Map<String, Object>> allComments = Lists.newArrayList();

      QueryRunner runner = new QueryRunner();
      for (Map<String, Object> row : rows) {
        Long componentId = (Long) row.get(PROJECT_ID);
        if (componentId == null) {
          continue;
        }
        String issueKey = Uuids.create();
        String status, severity, reporter = null;
        boolean manualSeverity;
        Object createdAt = Objects.firstNonNull(row.get(CREATED_AT), ONE_YEAR_AGO);
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
          updatedAt = Objects.firstNonNull(row.get(REVIEW_UPDATED_AT), ONE_YEAR_AGO);
          if ((Boolean) row.get(REVIEW_MANUAL_VIOLATION)) {
            reporter = referentials.userLogin((Long) row.get(REVIEW_REPORTER_ID));
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
        params[13] = referentials.userLogin((Long) row.get(REVIEW_ASSIGNEE_ID));
        params[14] = referentials.actionPlan((Long) row.get(PLAN_ID));
        params[15] = row.get(REVIEW_DATA);
        params[16] = createdAt;
        params[17] = updatedAt;
        params[18] = createdAt;
        params[19] = updatedAt;
        allParams.add(params);
      }
      runner.batch(writeConnection, SQL_INSERT_ISSUE, allParams.toArray(new Object[allParams.size()][]));
      insertComments(writeConnection, allComments);
      runner.update(writeConnection, SQL_DELETE_RULE_FAILURES, violationIds);
      writeConnection.commit();
      progress.increment(rows.size());

    } finally {
      DbUtils.closeQuietly(readConnection);
      DbUtils.closeQuietly(writeConnection);
    }
  }

  private void insertComments(Connection writeConnection, List<Map<String, Object>> comments) throws SQLException {
    List<Object[]> allParams = Lists.newArrayList();

    for (Map<String, Object> comment : comments) {
      String login = referentials.userLogin((Long) comment.get(USER_ID));
      if (login != null) {
        Object[] params = new Object[6];
        params[0] = Uuids.create();
        params[1] = comment.get(ISSUE_KEY);
        params[2] = login;
        params[3] = comment.get(REVIEW_TEXT);
        params[4] = comment.get(CREATED_AT);
        params[5] = comment.get(UPDATED_AT);
        allParams.add(params);
      }
    }
    if (!allParams.isEmpty()) {
      new QueryRunner().batch(writeConnection, SQL_INSERT_ISSUE_CHANGE, allParams.toArray(new Object[allParams.size()][]));
    }
  }

  private static class ReviewCommentsHandler extends AbstractListHandler<Map<String, Object>> {
    static final String SQL = "select created_at as createdAt, updated_at as updatedAt, user_id as userId, review_text as reviewText from review_comments where review_id=";

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(CREATED_AT, rs.getTimestamp(CREATED_AT));
      map.put(UPDATED_AT, rs.getTimestamp(UPDATED_AT));
      map.put(USER_ID, SqlUtil.getLong(rs, USER_ID));
      map.put(REVIEW_TEXT, rs.getString(REVIEW_TEXT));
      return map;
    }
  }

  private static class ViolationHandler extends AbstractListHandler<Map<String, Object>> {
    private static final Map<Integer, String> SEVERITIES = ImmutableMap.of(0, Severity.INFO, 1, Severity.MINOR, 2, Severity.MAJOR, 3, Severity.CRITICAL, 4, Severity.BLOCKER);

    @Override
    protected Map<String, Object> handleRow(ResultSet rs) throws SQLException {
      Map<String, Object> map = Maps.newHashMap();
      map.put(REVIEW_ID, SqlUtil.getLong(rs, REVIEW_ID));
      map.put(PROJECT_ID, SqlUtil.getLong(rs, PROJECT_ID));
      map.put(ROOT_PROJECT_ID, SqlUtil.getLong(rs, ROOT_PROJECT_ID));
      map.put(RULE_ID, SqlUtil.getLong(rs, RULE_ID));
      map.put(SEVERITY, Objects.firstNonNull(SEVERITIES.get(SqlUtil.getInt(rs, "failureLevel")), SEVERITY_MAJOR));
      map.put(MESSAGE, rs.getString(MESSAGE));
      map.put(LINE, SqlUtil.getInt(rs, LINE));
      map.put(COST, SqlUtil.getDouble(rs, COST));
      map.put(CHECKSUM, rs.getString(CHECKSUM));
      map.put(CREATED_AT, rs.getTimestamp(CREATED_AT));
      map.put(REVIEW_RESOLUTION, rs.getString(REVIEW_RESOLUTION));
      map.put(REVIEW_SEVERITY, Objects.firstNonNull(rs.getString(REVIEW_SEVERITY), SEVERITY_MAJOR));
      map.put(REVIEW_STATUS, rs.getString(REVIEW_STATUS));
      map.put(REVIEW_REPORTER_ID, SqlUtil.getLong(rs, REVIEW_REPORTER_ID));
      map.put(REVIEW_ASSIGNEE_ID, SqlUtil.getLong(rs, REVIEW_ASSIGNEE_ID));
      map.put(REVIEW_DATA, rs.getString(REVIEW_DATA));
      map.put(REVIEW_MANUAL_SEVERITY, rs.getBoolean(REVIEW_MANUAL_SEVERITY));
      map.put(REVIEW_UPDATED_AT, rs.getTimestamp(REVIEW_UPDATED_AT));
      map.put(REVIEW_MANUAL_VIOLATION, rs.getBoolean(REVIEW_MANUAL_VIOLATION));
      map.put(PLAN_ID, SqlUtil.getLong(rs, PLAN_ID));
      return map;
    }
  }
}
