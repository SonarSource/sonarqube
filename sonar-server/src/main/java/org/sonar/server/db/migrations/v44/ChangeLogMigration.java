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
package org.sonar.server.db.migrations.v44;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.activity.Activity;
import org.sonar.core.activity.db.ActivityDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.qualityprofile.db.ActiveRuleKey;
import org.sonar.core.rule.SeverityUtil;
import org.sonar.server.activity.ActivityService;
import org.sonar.server.activity.db.ActivityDao;
import org.sonar.server.db.DbClient;
import org.sonar.server.db.migrations.DatabaseMigration;
import org.sonar.server.qualityprofile.ActiveRuleChange;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * SONAR-5329
 * Transition ActiveRuleChanges to ActivityLog
 * <p/>
 * Used in the Active Record Migration 548.
 *
 * @since 4.4
 */
public class ChangeLogMigration implements DatabaseMigration {

  private static final Logger LOGGER = LoggerFactory.getLogger(ChangeLogMigration.class);

  private static final String PARAM_VALUE = "param_value";
  private static final String PARAM_NAME = "param_name";
  private static final String RULE_NAME = "rule_name";
  private static final String CREATED_AT = "created_at";
  private static final String SEVERITY = "severity";
  private static final String USER_LOGIN = "user_login";
  private static final String RULE_KEY = "rule_key";
  private static final String REPOSITORY = "rule_repository";
  private static final String PROFILE_KEY = "profile_key";

  private final ActivityDao dao;
  private DbSession session;
  private final DbClient db;

  private final String allActivation =
    "select" +
      "   rule_change.id," +
      "   rule_change.change_date as " + CREATED_AT + "," +
      "   users.login as " + USER_LOGIN + "," +
      "   rule_def.plugin_name as " + RULE_KEY + "," +
      "   rule_def.plugin_rule_key as " + REPOSITORY + "," +
      "   profile.kee as " + PROFILE_KEY + "," +
      "   rule_change.new_severity as " + SEVERITY + "," +
      "   rule_def.name as " + RULE_NAME + "," +
      "   rule_def_param.name as " + PARAM_NAME + "," +
      "   rule_param_change.new_value as " + PARAM_VALUE +
      " from active_rule_changes rule_change" +
      "   left join users on users.name = rule_change.username" +
      "   left join rules rule_def on rule_def.id = rule_change.rule_id" +
      "   left join rules_profiles profile on profile.id = rule_change.profile_id" +
      "   left join active_rule_param_changes rule_param_change on rule_param_change.active_rule_change_id = rule_change.id" +
      "   left join rules_parameters rule_def_param on rule_def_param.id = rule_param_change.rules_parameter_id" +
      " WHERE rule_change.enabled is true" +
      "       AND profile.name is not null" +
      "       AND profile.language is not null" +
      "       AND rule_def.plugin_name is not null" +
      "       AND rule_def.plugin_name is not null" +
      " order by rule_change.id ASC;";

  private final String allUpdates =
    "select" +
      "   rule_change.id," +
      "   rule_change.change_date as " + CREATED_AT + "," +
      "   users.login as " + USER_LOGIN + "," +
      "   rule_def.plugin_name as " + RULE_KEY + "," +
      "   rule_def.plugin_rule_key as " + REPOSITORY + "," +
      "   profile.kee as " + PROFILE_KEY + "," +
      "   rule_change.new_severity as " + SEVERITY + "," +
      "   rule_def.name as " + RULE_NAME + "," +
      "   rule_def_param.name as " + PARAM_NAME + "," +
      "   rule_param_change.new_value as " + PARAM_VALUE +
      " from active_rule_changes rule_change" +
      "   left join users on users.name = rule_change.username" +
      "   left join rules rule_def on rule_def.id = rule_change.rule_id" +
      "   left join rules_profiles profile on profile.id = rule_change.profile_id" +
      "   left join active_rule_param_changes rule_param_change on rule_param_change.active_rule_change_id = rule_change.id" +
      "   left join rules_parameters rule_def_param on rule_def_param.id = rule_param_change.rules_parameter_id" +
      " WHERE rule_change.enabled is null" +
      "       AND profile.name is not null" +
      "       AND profile.language is not null" +
      "       AND rule_def.plugin_name is not null" +
      "       AND rule_def.plugin_name is not null" +
      " order by rule_change.id ASC;";

  private String allDeactivation =
    "select" +
      "  rule_change.id as id," +
      "  rule_change.change_date as " + CREATED_AT + "," +
      "  users.login as " + USER_LOGIN + "," +
      "  rule_def.plugin_name as " + RULE_KEY + "," +
      "  rule_def.plugin_rule_key as " + REPOSITORY + "," +
      "  profile.kee as " + PROFILE_KEY +
      " from active_rule_changes rule_change" +
      "  left join users on users.name = rule_change.username" +
      "  left join rules rule_def on rule_def.id = rule_change.rule_id" +
      "  left join rules_profiles profile on profile.id = rule_change.profile_id" +
      " WHERE rule_change.enabled is false" +
      "      AND profile.name is not null" +
      "      AND profile.language is not null" +
      "      AND rule_def.plugin_name is not null" +
      "      AND rule_def.plugin_name is not null" +
      " order by rule_change.id ASC";

  public ChangeLogMigration(ActivityService service, ActivityDao dao, DbClient db) {
    this.dao = dao;
    this.db = db;
  }

  @Override
  public void execute() {
    try {
      this.session = db.openSession(false);
      executeUpsert(ActiveRuleChange.Type.ACTIVATED, allActivation);
      executeUpsert(ActiveRuleChange.Type.UPDATED, allUpdates);
      executeUpsert(ActiveRuleChange.Type.DEACTIVATED, allDeactivation);
      session.commit();
    } finally {
      session.close();
    }
  }

  private void executeUpsert(ActiveRuleChange.Type type, String sql) {
    Connection connection = null;
    try {
      connection = db.database().getDataSource().getConnection();
      ResultSet result = connection.createStatement().executeQuery(sql);

      // startCase
      boolean hasNext = result.next();
      if (hasNext) {
        int currentId = result.getInt("id");
        Date currentTimeStamp = result.getTimestamp(CREATED_AT);
        String currentAuthor = getAuthor(result);
        ActiveRuleChange ruleChange = newActiveRuleChance(type, result);
        processRuleChange(ruleChange, result);

        while (result.next()) {
          int id = result.getInt("id");
          if (id != currentId) {
            saveActiveRuleChange(ruleChange, currentAuthor, currentTimeStamp);
            currentId = id;
            currentTimeStamp = result.getTimestamp(CREATED_AT);
            currentAuthor = getAuthor(result);
            ruleChange = newActiveRuleChance(type, result);
          }
          processRuleChange(ruleChange, result);
        }
        // save the last
        saveActiveRuleChange(ruleChange, currentAuthor, currentTimeStamp);
      }

    } catch (Exception e) {
      throw new IllegalStateException(e);

    } finally {
      DbUtils.closeQuietly(connection);
    }
  }

  private String getAuthor(ResultSet result) {
    try {
      String author = result.getString(USER_LOGIN);
      if (StringUtils.isNotEmpty(author) && !author.equals("null")) {
        return author;
      } else {
        return "unknown";
      }
    } catch (Exception e) {
      return "unknown";
    }
  }

  private void saveActiveRuleChange(ActiveRuleChange ruleChange, String author, Date currentTimeStamp) {
    ActivityDto activity = ActivityDto.createFor(ruleChange);
    activity.setType(Activity.Type.QPROFILE);
    activity.setAuthor(author);
    activity.setCreatedAt(currentTimeStamp);
    dao.insert(session, activity);
  }

  private void processRuleChange(ActiveRuleChange ruleChange, ResultSet result) throws SQLException {

    try {
      ruleChange.setSeverity(SeverityUtil.getSeverityFromOrdinal(result.getInt(SEVERITY)));
    } catch (Exception e) {
      // System.out.println("e.getMessage() = " + e.getMessage());
    }
    try {
      String param_value = result.getString(PARAM_VALUE);
      String param_name = result.getString(PARAM_NAME);
      if (StringUtils.isNotEmpty(param_name) && !param_name.equals("null")) {
        ruleChange.setParameter(param_name, param_value);
      }
    } catch (Exception e) {
      // System.out.println("e.getMessage() = " + e.getMessage());
    }
  }

  private ActiveRuleChange newActiveRuleChance(ActiveRuleChange.Type type, ResultSet result) throws SQLException {
    return ActiveRuleChange.createFor(
      type, ActiveRuleKey.of(
        result.getString(PROFILE_KEY), RuleKey.of(result.getString(REPOSITORY), result.getString(RULE_KEY))));
  }
}
