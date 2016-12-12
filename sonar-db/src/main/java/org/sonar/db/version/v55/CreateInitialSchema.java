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
package org.sonar.db.version.v55;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.db.version.BigIntegerColumnDef;
import org.sonar.db.version.BooleanColumnDef;
import org.sonar.db.version.ColumnDef;
import org.sonar.db.version.CreateIndexBuilder;
import org.sonar.db.version.CreateTableBuilder;
import org.sonar.db.version.DdlChange;
import org.sonar.db.version.IntegerColumnDef;
import org.sonar.db.version.TinyIntColumnDef;
import org.sonar.db.version.VarcharColumnDef;

import static org.sonar.db.version.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.db.version.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.db.version.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.db.version.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.db.version.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;
import static org.sonar.db.version.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.db.version.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.db.version.TimestampColumnDef.newTimestampColumnDefBuilder;

public class CreateInitialSchema extends DdlChange {

  public CreateInitialSchema(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    createActiveDashboards(context);
    createActiveRuleParameters(context);
    createActiveRules(context);
    createActivities(context);
    createAuthors(context);
    createCeActivity(context);
    createCeQueue(context);
    createDashboards(context);
    createDuplicationsIndex(context);
    createEvents(context);
    createFileSources(context);
    createGroupRoles(context);
    createGroups(context);
    createGroupsUsers(context);
    createIssueChanges(context);
    createIssueFilterFavourites(context);
    createIssueFilters(context);
    createIssues(context);
    createLoadedTemplates(context);
    createManualMeasures(context);
    createMeasureFilterFavourites(context);
    createMeasureFilters(context);
    createMetrics(context);
    createNotifications(context);
    createPermissionTemplates(context);
    createPermTemplatesGroups(context);
    createPermTemplatesUsers(context);
    createProjectLinks(context);
    createProjectMeasures(context);
    createProjectQprofiles(context);
    createProjects(context);
    createProperties(context);
    createQualityGateConditions(context);
    createQualityGates(context);
    createResourceIndex(context);
    createRules(context);
    createRulesParameters(context);
    createRulesProfiles(context);
    createSnapshots(context);
    createUserRoles(context);
    createUserTokens(context);
    createUsers(context);
    createWidgetProperties(context);
    createWidgets(context);
  }

  private void createUserTokens(Context context) throws SQLException {
    VarcharColumnDef loginCol = newVarcharBuilder("login").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef nameCol = newVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    VarcharColumnDef tokenHashCol = newVarcharBuilder("token_hash").setLimit(255).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "user_tokens")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(loginCol)
        .addColumn(nameCol)
        .addColumn(tokenHashCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .build());
    addIndex(context, "user_tokens", "user_tokens_login_name", true, loginCol, nameCol);
    addIndex(context, "user_tokens", "user_tokens_token_hash", true, tokenHashCol);
  }

  private void createCeActivity(Context context) throws SQLException {
    VarcharColumnDef uuidCol = newVarcharBuilder("uuid").setLimit(40).setIsNullable(false).build();
    VarcharColumnDef isLastKeyCol = newVarcharBuilder("is_last_key").setLimit(55).setIsNullable(false).build();
    BooleanColumnDef isLastCol = newBooleanColumnDefBuilder().setColumnName("is_last").setIsNullable(false).build();
    VarcharColumnDef statusCol = newVarcharBuilder("status").setLimit(15).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newVarcharBuilder("component_uuid").setLimit(40).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "ce_activity")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newVarcharBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(componentUuidCol)
        .addColumn(statusCol)
        .addColumn(isLastCol)
        .addColumn(isLastKeyCol)
        .addColumn(newVarcharBuilder("submitter_login").setLimit(255).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("submitted_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("executed_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("execution_time_ms").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("snapshot_id").build())
        .build());
    addIndex(context, "ce_activity", "ce_activity_component_uuid", false, componentUuidCol);
    addIndex(context, "ce_activity", "ce_activity_islast_status", false, isLastCol, statusCol);
    addIndex(context, "ce_activity", "ce_activity_islastkey", false, isLastKeyCol);
    addIndex(context, "ce_activity", "ce_activity_uuid", true, uuidCol);
  }

  private void createCeQueue(Context context) throws SQLException {
    VarcharColumnDef uuidCol = newVarcharBuilder("uuid").setLimit(40).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newVarcharBuilder("component_uuid").setLimit(40).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "ce_queue")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newVarcharBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(componentUuidCol)
        .addColumn(newVarcharBuilder("status").setLimit(15).build())
        .addColumn(newVarcharBuilder("submitter_login").setLimit(255).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, "ce_queue", "ce_queue_component_uuid", false, componentUuidCol);
    addIndex(context, "ce_queue", "ce_queue_uuid", true, uuidCol);
  }

  private void createFileSources(Context context) throws SQLException {
    VarcharColumnDef projectUuidCol = newVarcharBuilder("project_uuid").setLimit(50).setIsNullable(false).build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build();
    VarcharColumnDef dataTypeCol = newVarcharBuilder("data_type").setLimit(20).build();
    VarcharColumnDef fileUuidCol = newVarcharBuilder("file_uuid").setLimit(50).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "file_sources")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectUuidCol)
        .addColumn(fileUuidCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("line_hashes").build())
        .addColumn(newVarcharBuilder("data_hash").setLimit(50).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(updatedAtCol)
        .addColumn(newVarcharBuilder("src_hash").setLimit(50).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("binary_data").build())
        .addColumn(dataTypeCol)
        .addColumn(newVarcharBuilder("revision").setLimit(100).build())
        .build());
    addIndex(context, "file_sources", "file_sources_project_uuid", false, projectUuidCol);
    addIndex(context, "file_sources", "file_sources_updated_at", false, updatedAtCol);
    addIndex(context, "file_sources", "file_sources_uuid_type", true, fileUuidCol, dataTypeCol);
  }

  private void createActivities(Context context) throws SQLException {
    VarcharColumnDef keeCol = newVarcharBuilder("log_key").setLimit(255).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "activities")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("data_field").build())
        .addColumn(newVarcharBuilder("log_type").setLimit(50).build())
        .addColumn(newVarcharBuilder("log_action").setLimit(50).build())
        .addColumn(newVarcharBuilder("log_message").setLimit(4000).build())
        .addColumn(keeCol)
        .build());

    addIndex(context, "activities", "activities_log_key", true, keeCol);
  }

  private void createPermTemplatesGroups(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "perm_templates_groups")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("group_id").build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(false).build())
        .addColumn(newVarcharBuilder("permission_reference").setLimit(64).setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
  }

  private void createPermTemplatesUsers(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "perm_templates_users")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(false).build())
        .addColumn(newVarcharBuilder("permission_reference").setLimit(64).setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
  }

  private void createPermissionTemplates(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "permission_templates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("kee").setLimit(100).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newVarcharBuilder("key_pattern").setLimit(500).build())
        .build());
  }

  private void createIssueFilterFavourites(Context context) throws SQLException {
    VarcharColumnDef loginCol = newVarcharBuilder("user_login").setLimit(255).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "issue_filter_favourites")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(loginCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("issue_filter_id").setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .build());
    addIndex(context, "issue_filter_favourites", "issue_filter_favs_user", false, loginCol);
  }

  private void createIssueFilters(Context context) throws SQLException {
    VarcharColumnDef nameCol = newVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "issue_filters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("shared").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("data").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "issue_filters", "issue_filters_name", false, nameCol);
  }

  private void createIssueChanges(Context context) throws SQLException {
    VarcharColumnDef issueKeyCol = newVarcharBuilder("issue_key").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef keeCol = newVarcharBuilder("kee").setLimit(50).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "issue_changes")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(keeCol)
        .addColumn(issueKeyCol)
        .addColumn(newVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newVarcharBuilder("change_type").setLimit(20).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("change_data").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_change_creation_date").build())
        .build());
    addIndex(context, "issue_changes", "issue_changes_issue_key", false, issueKeyCol);
    addIndex(context, "issue_changes", "issue_changes_kee", false, keeCol);
  }

  private void createIssues(Context context) throws SQLException {
    VarcharColumnDef assigneeCol = newVarcharBuilder("assignee").setLimit(255).build();
    VarcharColumnDef componentUuidCol = newVarcharBuilder("component_uuid").setLimit(50).build();
    BigIntegerColumnDef issueCreationDateCol = newBigIntegerColumnDefBuilder().setColumnName("issue_creation_date").build();
    VarcharColumnDef keeCol = newVarcharBuilder("kee").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef projectUuidCol = newVarcharBuilder("project_uuid").setLimit(50).build();
    VarcharColumnDef resolutionCol = newVarcharBuilder("resolution").setLimit(20).build();
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").build();
    context.execute(
      new CreateTableBuilder(getDialect(), "issues")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(keeCol)
        .addColumn(ruleIdCol)
        .addColumn(newVarcharBuilder("severity").setLimit(10).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("manual_severity").setIsNullable(false).build())
        // unit has been fixed in SonarQube 5.6 (see migration 1151, SONAR-7493)
        .addColumn(newVarcharBuilder("message").setIgnoreOracleUnit(false).setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line").build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("gap").setPrecision(30).setScale(20).build())
        .addColumn(newVarcharBuilder("status").setLimit(20).build())
        .addColumn(resolutionCol)
        .addColumn(newVarcharBuilder("checksum").setLimit(1000).build())
        .addColumn(newVarcharBuilder("reporter").setLimit(255).build())
        .addColumn(assigneeCol)
        .addColumn(newVarcharBuilder("author_login").setLimit(255).build())
        .addColumn(newVarcharBuilder("action_plan_key").setLimit(50).build())
        .addColumn(newVarcharBuilder("issue_attributes").setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("effort").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(updatedAtCol)
        .addColumn(issueCreationDateCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_update_date").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_close_date").build())
        .addColumn(newVarcharBuilder("tags").setLimit(4000).build())
        .addColumn(componentUuidCol)
        .addColumn(projectUuidCol)
        .addColumn(newBlobColumnDefBuilder().setColumnName("locations").build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("issue_type").build())
        .build());
    addIndex(context, "issues", "issues_assignee", false, assigneeCol);
    addIndex(context, "issues", "issues_component_uuid", false, componentUuidCol);
    addIndex(context, "issues", "issues_creation_date", false, issueCreationDateCol);
    addIndex(context, "issues", "issues_kee", true, keeCol);
    addIndex(context, "issues", "issues_project_uuid", false, projectUuidCol);
    addIndex(context, "issues", "issues_resolution", false, resolutionCol);
    addIndex(context, "issues", "issues_rule_id", false, ruleIdCol);
    addIndex(context, "issues", "issues_updated_at", false, updatedAtCol);
  }

  private void createMeasureFilterFavourites(Context context) throws SQLException {
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "measure_filter_favourites")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(userIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("measure_filter_id").setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .build());
    addIndex(context, "measure_filter_favourites", "measure_filter_favs_userid", false, userIdCol);
  }

  private void createMeasureFilters(Context context) throws SQLException {
    VarcharColumnDef nameCol = newVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "measure_filters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("shared").setDefaultValue(false).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("data").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "measure_filters", "measure_filters_name", false, nameCol);
  }

  private void createAuthors(Context context) throws SQLException {
    VarcharColumnDef loginCol = newVarcharBuilder("login").setLimit(255).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "authors")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("person_id").setIsNullable(false).build())
        .addColumn(loginCol)
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "authors", "uniq_author_logins", true, loginCol);
  }

  private void createResourceIndex(Context context) throws SQLException {
    VarcharColumnDef keeCol = newVarcharBuilder("kee").setLimit(400).setIsNullable(false).build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "resource_index")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(keeCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("position").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("name_size").setIsNullable(false).build())
        .addColumn(resourceIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("root_project_id").setIsNullable(false).build())
        .addColumn(newVarcharBuilder("qualifier").setLimit(10).setIsNullable(false).build())
        .build());
    addIndex(context, "resource_index", "resource_index_key", false, keeCol);
    addIndex(context, "resource_index", "resource_index_rid", false, resourceIdCol);
  }

  private void createLoadedTemplates(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "loaded_templates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("kee").setLimit(200).build())
        .addColumn(newVarcharBuilder("template_type").setLimit(15).build())
        .build());
  }

  private void createMetrics(Context context) throws SQLException {
    VarcharColumnDef nameCol = newVarcharBuilder("name").setLimit(64).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "metrics")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newVarcharBuilder("description").setLimit(255).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("direction").setIsNullable(false).setDefaultValue(0).build())
        .addColumn(newVarcharBuilder("domain").setLimit(64).build())
        .addColumn(newVarcharBuilder("short_name").setLimit(64).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("qualitative").setDefaultValue(false).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("val_type").setLimit(8).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("user_managed").setDefaultValue(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("enabled").setDefaultValue(true).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("worst_value").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("best_value").setPrecision(38).setScale(20).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("optimized_best_value").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("hidden").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("delete_historical_data").build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("decimal_scale").build())
        .build());
    addIndex(context, "metrics", "metrics_unique_name", true, nameCol);
  }

  private void createDashboards(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "dashboards")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").build())
        .addColumn(newVarcharBuilder("name").setLimit(256).build())
        .addColumn(newVarcharBuilder("description").setLimit(1000).build())
        .addColumn(newVarcharBuilder("column_layout").setLimit(20).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("shared").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_global").build())
        .build());
  }

  private void createUsers(Context context) throws SQLException {
    VarcharColumnDef loginCol = newVarcharBuilder("login").setLimit(255).build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").build();
    context.execute(
      new CreateTableBuilder(getDialect(), "users")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(loginCol)
        .addColumn(newVarcharBuilder("name").setLimit(200).build())
        .addColumn(newVarcharBuilder("email").setLimit(100).build())
        .addColumn(newVarcharBuilder("crypted_password").setLimit(40).build())
        .addColumn(newVarcharBuilder("salt").setLimit(40).build())
        .addColumn(newVarcharBuilder("remember_token").setLimit(500).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("remember_token_expires_at").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("active").setDefaultValue(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(updatedAtCol)
        .addColumn(newVarcharBuilder("scm_accounts").setLimit(4000).build())
        .addColumn(newVarcharBuilder("external_identity").setLimit(255).build())
        .addColumn(newVarcharBuilder("external_identity_provider").setLimit(100).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("user_local").build())
        .build());
    addIndex(context, "users", "users_login", true, loginCol);
    addIndex(context, "users", "users_updated_at", false, updatedAtCol);
  }

  private void createActiveRuleParameters(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "active_rule_parameters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("active_rule_id").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rules_parameter_id").setIsNullable(false).build())
        .addColumn(newVarcharBuilder("value").setLimit(4000).build())
        .addColumn(newVarcharBuilder("rules_parameter_key").setLimit(128).build())
        .build());
  }

  private void createActiveRules(Context context) throws SQLException {
    IntegerColumnDef profileIdCol = newIntegerColumnDefBuilder().setColumnName("profile_id").setIsNullable(false).build();
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "active_rules")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(profileIdCol)
        .addColumn(ruleIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("failure_level").setIsNullable(false).build())
        .addColumn(newVarcharBuilder("inheritance").setLimit(10).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "active_rules", "uniq_profile_rule_ids", true, profileIdCol, ruleIdCol);
  }

  private void createUserRoles(Context context) throws SQLException {
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").build();
    context.execute(
      new CreateTableBuilder(getDialect(), "user_roles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(userIdCol)
        .addColumn(resourceIdCol)
        .addColumn(newVarcharBuilder("role").setLimit(64).setIsNullable(false).build())
        .build());
    addIndex(context, "user_roles", "user_roles_resource", false, resourceIdCol);
    addIndex(context, "user_roles", "user_roles_user", false, userIdCol);
  }

  private void createActiveDashboards(Context context) throws SQLException {
    IntegerColumnDef dashboardIdCol = newIntegerColumnDefBuilder().setColumnName("dashboard_id").setIsNullable(false).build();
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").build();
    context.execute(
      new CreateTableBuilder(getDialect(), "active_dashboards")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(dashboardIdCol)
        .addColumn(userIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("order_index").build())
        .build());
    addIndex(context, "active_dashboards", "active_dashboards_dashboardid", false, dashboardIdCol);
    addIndex(context, "active_dashboards", "active_dashboards_userid", false, userIdCol);
  }

  private void createNotifications(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "notifications")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newBlobColumnDefBuilder().setColumnName("data").build())
        .build());
  }

  private void createSnapshots(Context context) throws SQLException {
    IntegerColumnDef projectIdCol = newIntegerColumnDefBuilder().setColumnName("project_id").setIsNullable(false).build();
    IntegerColumnDef rootProjectIdCol = newIntegerColumnDefBuilder().setColumnName("root_project_id").setIsNullable(true).build();
    IntegerColumnDef parentSnapshotIdCol = newIntegerColumnDefBuilder().setColumnName("parent_snapshot_id").setIsNullable(true).build();
    VarcharColumnDef qualifierCol = newVarcharBuilder("qualifier").setLimit(10).setIsNullable(true).build();
    IntegerColumnDef rootSnapshotIdCol = newIntegerColumnDefBuilder().setColumnName("root_snapshot_id").setIsNullable(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "snapshots")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectIdCol)
        .addColumn(parentSnapshotIdCol)
        .addColumn(newVarcharBuilder("status").setLimit(4).setIsNullable(false).setDefaultValue("U").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("islast").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newVarcharBuilder("scope").setLimit(3).setIsNullable(true).build())
        .addColumn(qualifierCol)
        .addColumn(rootSnapshotIdCol)
        .addColumn(newVarcharBuilder("version").setLimit(500).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("path").setLimit(500).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("depth").setIsNullable(true).build())
        .addColumn(rootProjectIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("purge_status").setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period1_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period1_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period2_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period2_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period3_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period3_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period4_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period4_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period5_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("period5_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("build_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period1_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period2_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period3_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period4_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period5_date").setIsNullable(true).build())
        .build());
    addIndex(context, "snapshots", "snapshot_project_id", false, projectIdCol);
    addIndex(context, "snapshots", "snapshots_parent", false, parentSnapshotIdCol);
    addIndex(context, "snapshots", "snapshots_qualifier", false, qualifierCol);
    addIndex(context, "snapshots", "snapshots_root", false, rootSnapshotIdCol);
    addIndex(context, "snapshots", "snapshots_root_project_id", false, rootProjectIdCol);
  }

  private void createGroups(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "groups")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("name").setLimit(500).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("description").setLimit(200).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
  }

  private void createWidgets(Context context) throws SQLException {
    IntegerColumnDef dashboardId = newIntegerColumnDefBuilder().setColumnName("dashboard_id").setIsNullable(false).build();
    VarcharColumnDef widgetKey = newVarcharBuilder("widget_key").setLimit(256).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "widgets")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(dashboardId)
        .addColumn(widgetKey)
        .addColumn(newVarcharBuilder("name").setLimit(256).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("description").setLimit(1000).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("column_index").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("row_index").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("configured").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build())
        .build());
    addIndex(context, "widgets", "widgets_dashboards", false, dashboardId);
    addIndex(context, "widgets", "widgets_widgetkey", false, widgetKey);
  }

  private void createProjectQprofiles(Context context) throws SQLException {
    VarcharColumnDef projectUuid = newVarcharBuilder("project_uuid").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef profileKey = newVarcharBuilder("profile_key").setLimit(50).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "project_qprofiles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectUuid)
        .addColumn(profileKey)
        .build());
    addIndex(context, "project_qprofiles", "uniq_project_qprofiles", true, projectUuid, profileKey);
  }

  private void createRulesProfiles(Context context) throws SQLException {
    VarcharColumnDef keeCol = newVarcharBuilder("kee").setLimit(255).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "rules_profiles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(keeCol)
        .addColumn(newVarcharBuilder("parent_kee").setLimit(255).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("rules_updated_at").setLimit(100).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_default").setIsNullable(false).build())
        .build());
    addIndex(context, "rules_profiles", "uniq_qprof_key", true, keeCol);
  }

  private void createRulesParameters(Context context) throws SQLException {
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "rules_parameters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(ruleIdCol)
        .addColumn(newVarcharBuilder("name").setLimit(128).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("param_type").setLimit(512).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("default_value").setLimit(4000).setIsNullable(true).build())
        .build());
    addIndex(context, "rules_parameters", "rules_parameters_rule_id", false, ruleIdCol);
  }

  private void createGroupsUsers(Context context) throws SQLException {
    BigIntegerColumnDef userIdCol = newBigIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build();
    BigIntegerColumnDef groupIdCol = newBigIntegerColumnDefBuilder().setColumnName("group_id").setIsNullable(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "groups_users")
        .addColumn(userIdCol)
        .addColumn(groupIdCol)
        .build());
    addIndex(context, "groups_users", "index_groups_users_on_user_id", false, userIdCol);
    addIndex(context, "groups_users", "index_groups_users_on_group_id", false, groupIdCol);
    addIndex(context, "groups_users", "groups_users_unique", true, groupIdCol, userIdCol);
  }

  private void createProjectMeasures(Context context) throws SQLException {
    IntegerColumnDef personIdCol = newIntegerColumnDefBuilder().setColumnName("person_id").build();
    IntegerColumnDef metricIdCol = newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(false).build();
    IntegerColumnDef snapshotIdCol = newIntegerColumnDefBuilder().setColumnName("snapshot_id").setIsNullable(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "project_measures")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
        .addColumn(metricIdCol)
        .addColumn(snapshotIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rules_category_id").setIsNullable(true).build())
        .addColumn(newVarcharBuilder("text_value").setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("tendency").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("measure_date").build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("project_id").build())
        .addColumn(newVarcharBuilder("alert_status").setLimit(5).build())
        .addColumn(newVarcharBuilder("alert_text").setLimit(4000).build())
        .addColumn(newVarcharBuilder("url").setLimit(2000).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rule_priority").build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("characteristic_id").build())
        .addColumn(personIdCol)
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_1").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_2").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_3").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_4").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_5").setPrecision(38).setScale(20).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("measure_data").build())
        .build());
    addIndex(context, "project_measures", "measures_sid_metric", false, snapshotIdCol, metricIdCol);
    addIndex(context, "project_measures", "measures_person", false, personIdCol);
  }

  private void createManualMeasures(Context context) throws SQLException {
    VarcharColumnDef componentUuidCol = newVarcharBuilder("component_uuid").setLimit(50).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "manual_measures")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(false).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
        .addColumn(newVarcharBuilder("text_value").setLimit(4000).build())
        .addColumn(newVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(componentUuidCol)
        .build());
    addIndex(context, "manual_measures", "manual_measures_component_uuid", false, componentUuidCol);
  }

  private void createProjects(Context context) throws SQLException {
    VarcharColumnDef keeCol = newVarcharBuilder("kee").setLimit(400).build();
    VarcharColumnDef moduleUuidCol = newVarcharBuilder("module_uuid").setLimit(50).build();
    VarcharColumnDef projectUuidCol = newVarcharBuilder("project_uuid").setLimit(50).build();
    VarcharColumnDef qualifierCol = newVarcharBuilder("qualifier").setLimit(10).build();
    IntegerColumnDef rootIdCol = newIntegerColumnDefBuilder().setColumnName("root_id").build();
    VarcharColumnDef uuidCol = newVarcharBuilder("uuid").setLimit(50).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "projects")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("name").setLimit(2000).build())
        .addColumn(newVarcharBuilder("description").setLimit(2000).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("enabled").setDefaultValue(true).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("scope").setLimit(3).build())
        .addColumn(qualifierCol)
        .addColumn(keeCol)
        .addColumn(rootIdCol)
        .addColumn(newVarcharBuilder("language").setLimit(20).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("copy_resource_id").build())
        .addColumn(newVarcharBuilder("long_name").setLimit(2000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("person_id").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newVarcharBuilder("path").setLimit(2000).build())
        .addColumn(newVarcharBuilder("deprecated_kee").setLimit(400).build())
        .addColumn(uuidCol)
        .addColumn(projectUuidCol)
        .addColumn(moduleUuidCol)
        .addColumn(newVarcharBuilder("module_uuid_path").setLimit(4000).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("authorization_updated_at").build())
        .build());
    addIndex(context, "projects", "projects_kee", true, keeCol);
    addIndex(context, "projects", "projects_module_uuid", false, moduleUuidCol);
    addIndex(context, "projects", "projects_project_uuid", false, projectUuidCol);
    addIndex(context, "projects", "projects_qualifier", false, qualifierCol);
    addIndex(context, "projects", "projects_root_id", false, rootIdCol);
    addIndex(context, "projects", "projects_uuid", true, uuidCol);
  }

  private void createGroupRoles(Context context) throws SQLException {
    IntegerColumnDef groupIdCol = newIntegerColumnDefBuilder().setColumnName("group_id").setIsNullable(true).build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build();
    VarcharColumnDef roleCol = newVarcharBuilder("role").setLimit(64).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "group_roles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(groupIdCol)
        .addColumn(resourceIdCol)
        .addColumn(roleCol)
        .build());
    addIndex(context, "group_roles", "group_roles_group", false, groupIdCol);
    addIndex(context, "group_roles", "group_roles_resource", false, resourceIdCol);
    addIndex(context, "group_roles", "group_roles_role", false, roleCol);
    addIndex(context, "group_roles", "uniq_group_roles", true, groupIdCol, resourceIdCol, roleCol);
  }

  private void createRules(Context context) throws SQLException {
    VarcharColumnDef pluginRuleKeyCol = newVarcharBuilder("plugin_rule_key").setLimit(200).setIsNullable(false).build();
    VarcharColumnDef pluginNameCol = newVarcharBuilder("plugin_name").setLimit(255).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "rules")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("name").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginRuleKeyCol)
        .addColumn(newVarcharBuilder("plugin_config_key").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginNameCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("description").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("priority").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(true).build())
        .addColumn(newVarcharBuilder("status").setLimit(40).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("note_created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("note_updated_at").setIsNullable(true).build())
        .addColumn(newVarcharBuilder("note_user_login").setLimit(255).setIsNullable(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("note_data").setIsNullable(true).build())
        .addColumn(newVarcharBuilder("remediation_function").setLimit(200).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("def_remediation_function").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("def_remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("def_remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("gap_description").setLimit(4000).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("tags").setLimit(4000).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("system_tags").setLimit(4000).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_template").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newVarcharBuilder("description_format").setLimit(20).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("rule_type").setIsNullable(true).build())
        .build());
    addIndex(context, "rules", "rules_repo_key", true, pluginRuleKeyCol, pluginNameCol);
  }

  private void createWidgetProperties(Context context) throws SQLException {
    IntegerColumnDef widgetIdCol = newIntegerColumnDefBuilder().setColumnName("widget_id").setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "widget_properties")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(widgetIdCol)
        .addColumn(newVarcharBuilder("kee").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("text_value").setLimit(4000).setIsNullable(true).build())
        .build());
    addIndex(context, "widget_properties", "widget_properties_widgets", false, widgetIdCol);
  }

  private void createEvents(Context context) throws SQLException {
    VarcharColumnDef componentUuid = newVarcharBuilder("component_uuid").setLimit(50).setIsNullable(true).build();
    IntegerColumnDef snapshotId = newIntegerColumnDefBuilder().setColumnName("snapshot_id").setIsNullable(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "events")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("name").setLimit(400).setIsNullable(true).build())
        .addColumn(snapshotId)
        .addColumn(newVarcharBuilder("category").setLimit(50).build())
        .addColumn(newVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newVarcharBuilder("event_data").setLimit(4000).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("event_date").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(componentUuid)
        .build());
    addIndex(context, "events", "events_component_uuid", false, componentUuid);
    addIndex(context, "events", "events_snapshot_id", false, snapshotId);
  }

  private void createQualityGates(Context context) throws SQLException {
    VarcharColumnDef nameCol = newVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "quality_gates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
    addIndex(context, "quality_gates", "uniq_quality_gates", true, nameCol);
  }

  private void createQualityGateConditions(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "quality_gate_conditions")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("qgate_id").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("period").setIsNullable(true).build())
        .addColumn(newVarcharBuilder("operator").setLimit(3).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("value_error").setLimit(64).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("value_warning").setLimit(64).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
  }

  private void createProperties(Context context) throws SQLException {
    VarcharColumnDef propKey = newVarcharBuilder("prop_key").setLimit(512).setIsNullable(true).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "properties")
        // do not define as primary key on purpose -> already set in org.sonar.db.version.v61.CreateTableProperties2
        .addColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build())
        .addColumn(propKey)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("text_value").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build())
        .build());
    addIndex(context, "properties", "properties_key", false, propKey);
  }

  private void createProjectLinks(Context context) throws SQLException {
    context.execute(
      new CreateTableBuilder(getDialect(), "project_links")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newVarcharBuilder("link_type").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("name").setLimit(128).setIsNullable(true).build())
        .addColumn(newVarcharBuilder("href").setLimit(2048).setIsNullable(false).build())
        .addColumn(newVarcharBuilder("component_uuid").setLimit(2048).setIsNullable(true).build())
        .build());
  }

  private void createDuplicationsIndex(Context context) throws SQLException {
    VarcharColumnDef hashCol = newVarcharBuilder("hash").setLimit(50).setIsNullable(false).build();
    IntegerColumnDef snapshotIdCol = newIntegerColumnDefBuilder().setColumnName("snapshot_id").setIsNullable(false).build();
    context.execute(
      new CreateTableBuilder(getDialect(), "duplications_index")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("project_snapshot_id").setIsNullable(false).build())
        .addColumn(snapshotIdCol)
        .addColumn(hashCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("index_in_file").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("start_line").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("end_line").setIsNullable(false).build())
        .build());
    addIndex(context, "duplications_index", "duplications_index_hash", false, hashCol);
    addIndex(context, "duplications_index", "duplications_index_sid", false, snapshotIdCol);
  }

  private void addIndex(Context context, String table, String index, boolean unique, ColumnDef... columns) throws SQLException {
    CreateIndexBuilder builder = new CreateIndexBuilder(getDialect())
      .setTable(table)
      .setName(index)
      .setUnique(unique);
    for (ColumnDef column : columns) {
      builder.addColumn(column);
    }
    context.execute(builder.build());
  }

  private static VarcharColumnDef.Builder newVarcharBuilder(String column) {
    return new VarcharColumnDef.Builder().setColumnName(column).setIgnoreOracleUnit(true);
  }
}
