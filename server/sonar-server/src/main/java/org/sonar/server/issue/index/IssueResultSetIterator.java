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
package org.sonar.server.issue.index;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.rule.RuleKey;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.ResultSetIterator;
import org.sonar.server.db.migrations.SqlUtil;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import static org.sonar.api.utils.DateUtils.longToDate;
import static org.sonar.server.db.migrations.SqlUtil.getLong;

/**
 * Scrolls over table ISSUES and reads documents to populate
 * the issues index
 */
class IssueResultSetIterator extends ResultSetIterator<IssueDoc> {

  private static final String[] FIELDS = {
    // column 1
    "i.kee",
    "root.uuid",
    "i.updated_at",
    "i.action_plan_key",
    "i.assignee",
    "i.effort_to_fix",
    "i.issue_attributes",
    "i.line",
    "i.message",
    "i.resolution",

    // column 11
    "i.severity",
    "i.manual_severity",
    "i.checksum",
    "i.status",
    "i.technical_debt",
    "i.reporter",
    "i.author_login",
    "i.issue_close_date",
    "i.issue_creation_date",
    "i.issue_update_date",

    // column 21
    "r.plugin_name",
    "r.plugin_rule_key",
    "r.language",
    "p.uuid",
    "p.module_uuid_path",
    "p.path",
    "p.scope",
    "i.tags"
  };

  private static final String SQL_ALL = "select " + StringUtils.join(FIELDS, ",") + " from issues i " +
    "inner join rules r on r.id=i.rule_id " +
    "inner join projects p on p.uuid=i.component_uuid " +
    "inner join projects root on root.uuid=i.project_uuid";

  private static final String SQL_AFTER_DATE = SQL_ALL + " where i.updated_at>?";

  private static final Splitter TAGS_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  private static final Splitter MODULE_PATH_SPLITTER = Splitter.on('.').trimResults().omitEmptyStrings();

  private IssueResultSetIterator(PreparedStatement stmt) throws SQLException {
    super(stmt);
  }

  static IssueResultSetIterator create(DbClient dbClient, Connection connection, long afterDate) {
    try {
      String sql = afterDate > 0L ? SQL_AFTER_DATE : SQL_ALL;
      PreparedStatement stmt = dbClient.newScrollingSelectStatement(connection, sql);
      if (afterDate > 0L) {
        stmt.setLong(1, afterDate);
      }
      return new IssueResultSetIterator(stmt);
    } catch (SQLException e) {
      throw new IllegalStateException("Fail to prepare SQL request to select all issues", e);
    }
  }

  @CheckForNull
  private static String extractDirPath(@Nullable String filePath, String scope) {
    if (filePath != null) {
      if (Scopes.DIRECTORY.equals(scope)) {
        return filePath;
      }
      int lastSlashIndex = CharMatcher.anyOf("/").lastIndexIn(filePath);
      if (lastSlashIndex > 0) {
        return filePath.substring(0, lastSlashIndex);
      }
      return "/";
    }
    return null;
  }

  @CheckForNull
  private static String extractFilePath(@Nullable String filePath, String scope) {
    // On modules, the path contains the relative path of the module starting from its parent, and in E/S we're only interested in the path
    // of files and directories.
    // That's why the file path should be null on modules and projects.
    if (filePath != null && !Scopes.PROJECT.equals(scope)) {
      return filePath;
    }
    return null;
  }

  private static String extractModule(String moduleUuidPath) {
    return Iterators.getLast(MODULE_PATH_SPLITTER.split(moduleUuidPath).iterator());
  }

  @Override
  protected IssueDoc read(ResultSet rs) throws SQLException {
    IssueDoc doc = new IssueDoc(Maps.<String, Object>newHashMapWithExpectedSize(30));

    String key = rs.getString(1);
    String projectUuid = rs.getString(2);

    // all the fields must be present, even if value is null
    doc.setKey(key);
    doc.setProjectUuid(projectUuid);
    doc.setTechnicalUpdateDate(new Date(rs.getLong(3)));
    doc.setActionPlanKey(rs.getString(4));
    doc.setAssignee(rs.getString(5));
    doc.setEffortToFix(SqlUtil.getDouble(rs, 6));
    doc.setAttributes(rs.getString(7));
    doc.setLine(SqlUtil.getInt(rs, 8));
    doc.setMessage(rs.getString(9));
    doc.setResolution(rs.getString(10));
    doc.setSeverity(rs.getString(11));
    doc.setManualSeverity(rs.getBoolean(12));
    doc.setChecksum(rs.getString(13));
    doc.setStatus(rs.getString(14));
    doc.setDebt(getLong(rs, 15));
    doc.setReporter(rs.getString(16));
    doc.setAuthorLogin(rs.getString(17));
    doc.setFuncCloseDate(longToDate(getLong(rs, 18)));
    doc.setFuncCreationDate(longToDate(getLong(rs, 19)));
    doc.setFuncUpdateDate(longToDate(getLong(rs, 20)));
    String ruleRepo = rs.getString(21);
    String ruleKey = rs.getString(22);
    doc.setRuleKey(RuleKey.of(ruleRepo, ruleKey).toString());
    doc.setLanguage(rs.getString(23));
    doc.setComponentUuid(rs.getString(24));
    String moduleUuidPath = rs.getString(25);
    doc.setModuleUuid(extractModule(moduleUuidPath));
    doc.setModuleUuidPath(moduleUuidPath);
    String scope = rs.getString(27);
    String filePath = extractFilePath(rs.getString(26), scope);
    doc.setFilePath(filePath);
    doc.setDirectoryPath(extractDirPath(doc.filePath(), scope));
    String tags = rs.getString(28);
    doc.setTags(ImmutableList.copyOf(TAGS_SPLITTER.split(tags == null ? "" : tags)));
    return doc;
  }
}
