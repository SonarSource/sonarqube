/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
package org.sonar.server.platform.db.migration.version.v56;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.def.TinyIntColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;

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
    VarcharColumnDef loginCol = newLenientVarcharBuilder("login").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    VarcharColumnDef tokenHashCol = newLenientVarcharBuilder("token_hash").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder("user_tokens")
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
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(40).setIsNullable(false).build();
    VarcharColumnDef isLastKeyCol = newLenientVarcharBuilder("is_last_key").setLimit(55).setIsNullable(false).build();
    BooleanColumnDef isLastCol = newBooleanColumnDefBuilder().setColumnName("is_last").setIsNullable(false).build();
    VarcharColumnDef statusCol = newLenientVarcharBuilder("status").setLimit(15).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(40).build();
    context.execute(
      newTableBuilder("ce_activity")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newLenientVarcharBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(componentUuidCol)
        .addColumn(statusCol)
        .addColumn(isLastCol)
        .addColumn(isLastKeyCol)
        .addColumn(newLenientVarcharBuilder("submitter_login").setLimit(255).build())
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
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(40).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(40).build();
    context.execute(
      newTableBuilder("ce_queue")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newLenientVarcharBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(componentUuidCol)
        .addColumn(newLenientVarcharBuilder("status").setLimit(15).build())
        .addColumn(newLenientVarcharBuilder("submitter_login").setLimit(255).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, "ce_queue", "ce_queue_component_uuid", false, componentUuidCol);
    addIndex(context, "ce_queue", "ce_queue_uuid", true, uuidCol);
  }

  private void createFileSources(Context context) throws SQLException {
    VarcharColumnDef projectUuidCol = newLenientVarcharBuilder("project_uuid").setLimit(50).setIsNullable(false).build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build();
    VarcharColumnDef dataTypeCol = newLenientVarcharBuilder("data_type").setLimit(20).build();
    VarcharColumnDef fileUuidCol = newLenientVarcharBuilder("file_uuid").setLimit(50).setIsNullable(false).build();
    context.execute(
      newTableBuilder("file_sources")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectUuidCol)
        .addColumn(fileUuidCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("line_hashes").build())
        .addColumn(newLenientVarcharBuilder("data_hash").setLimit(50).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(updatedAtCol)
        .addColumn(newLenientVarcharBuilder("src_hash").setLimit(50).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("binary_data").build())
        .addColumn(dataTypeCol)
        .addColumn(newLenientVarcharBuilder("revision").setLimit(100).build())
        .build());
    addIndex(context, "file_sources", "file_sources_project_uuid", false, projectUuidCol);
    addIndex(context, "file_sources", "file_sources_updated_at", false, updatedAtCol);
    addIndex(context, "file_sources", "file_sources_uuid_type", true, fileUuidCol, dataTypeCol);
  }

  private void createActivities(Context context) throws SQLException {
    VarcharColumnDef keeCol = newLenientVarcharBuilder("log_key").setLimit(255).build();
    context.execute(
      newTableBuilder("activities")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newLenientVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("data_field").build())
        .addColumn(newLenientVarcharBuilder("log_type").setLimit(50).build())
        .addColumn(newLenientVarcharBuilder("log_action").setLimit(50).build())
        .addColumn(newLenientVarcharBuilder("log_message").setLimit(4000).build())
        .addColumn(keeCol)
        .build());

    addIndex(context, "activities", "activities_log_key", true, keeCol);
  }

  private void createPermTemplatesGroups(Context context) throws SQLException {
    context.execute(
      newTableBuilder("perm_templates_groups")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("group_id").build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("permission_reference").setLimit(64).setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
  }

  private void createPermTemplatesUsers(Context context) throws SQLException {
    context.execute(
      newTableBuilder("perm_templates_users")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("permission_reference").setLimit(64).setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
  }

  private void createPermissionTemplates(Context context) throws SQLException {
    context.execute(
      newTableBuilder("permission_templates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("kee").setLimit(100).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newLenientVarcharBuilder("key_pattern").setLimit(500).build())
        .build());
  }

  private void createIssueFilterFavourites(Context context) throws SQLException {
    VarcharColumnDef loginCol = newLenientVarcharBuilder("user_login").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder("issue_filter_favourites")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(loginCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("issue_filter_id").setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .build());
    addIndex(context, "issue_filter_favourites", "issue_filter_favs_user", false, loginCol);
  }

  private void createIssueFilters(Context context) throws SQLException {
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    context.execute(
      newTableBuilder("issue_filters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newLenientVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("shared").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("data").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "issue_filters", "issue_filters_name", false, nameCol);
  }

  private void createIssueChanges(Context context) throws SQLException {
    VarcharColumnDef issueKeyCol = newLenientVarcharBuilder("issue_key").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(50).build();
    context.execute(
      newTableBuilder("issue_changes")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(keeCol)
        .addColumn(issueKeyCol)
        .addColumn(newLenientVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newLenientVarcharBuilder("change_type").setLimit(20).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("change_data").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_change_creation_date").build())
        .build());
    addIndex(context, "issue_changes", "issue_changes_issue_key", false, issueKeyCol);
    addIndex(context, "issue_changes", "issue_changes_kee", false, keeCol);
  }

  private void createIssues(Context context) throws SQLException {
    VarcharColumnDef assigneeCol = newLenientVarcharBuilder("assignee").setLimit(255).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(50).build();
    BigIntegerColumnDef issueCreationDateCol = newBigIntegerColumnDefBuilder().setColumnName("issue_creation_date").build();
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef projectUuidCol = newLenientVarcharBuilder("project_uuid").setLimit(50).build();
    VarcharColumnDef resolutionCol = newLenientVarcharBuilder("resolution").setLimit(20).build();
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").build();
    context.execute(
      newTableBuilder("issues")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(keeCol)
        .addColumn(ruleIdCol)
        .addColumn(newLenientVarcharBuilder("severity").setLimit(10).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("manual_severity").setIsNullable(false).build())
        // unit has been fixed in SonarQube 5.6 (see migration 1151, SONAR-7493)
        .addColumn(newLenientVarcharBuilder("message").setIgnoreOracleUnit(false).setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line").build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("gap").setPrecision(30).setScale(20).build())
        .addColumn(newLenientVarcharBuilder("status").setLimit(20).build())
        .addColumn(resolutionCol)
        .addColumn(newLenientVarcharBuilder("checksum").setLimit(1000).build())
        .addColumn(newLenientVarcharBuilder("reporter").setLimit(255).build())
        .addColumn(assigneeCol)
        .addColumn(newLenientVarcharBuilder("author_login").setLimit(255).build())
        .addColumn(newLenientVarcharBuilder("action_plan_key").setLimit(50).build())
        .addColumn(newLenientVarcharBuilder("issue_attributes").setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("effort").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(updatedAtCol)
        .addColumn(issueCreationDateCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_update_date").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_close_date").build())
        .addColumn(newLenientVarcharBuilder("tags").setLimit(4000).build())
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
      newTableBuilder("measure_filter_favourites")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(userIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("measure_filter_id").setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .build());
    addIndex(context, "measure_filter_favourites", "measure_filter_favs_userid", false, userIdCol);
  }

  private void createMeasureFilters(Context context) throws SQLException {
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    context.execute(
      newTableBuilder("measure_filters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("shared").setDefaultValue(false).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("data").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "measure_filters", "measure_filters_name", false, nameCol);
  }

  private void createAuthors(Context context) throws SQLException {
    VarcharColumnDef loginCol = newLenientVarcharBuilder("login").setLimit(255).build();
    context.execute(
      newTableBuilder("authors")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("person_id").setIsNullable(false).build())
        .addColumn(loginCol)
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "authors", "uniq_author_logins", true, loginCol);
  }

  private void createResourceIndex(Context context) throws SQLException {
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(400).setIsNullable(false).build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder("resource_index")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(keeCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("position").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("name_size").setIsNullable(false).build())
        .addColumn(resourceIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("root_project_id").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("qualifier").setLimit(10).setIsNullable(false).build())
        .build());
    addIndex(context, "resource_index", "resource_index_key", false, keeCol);
    addIndex(context, "resource_index", "resource_index_rid", false, resourceIdCol);
  }

  private void createLoadedTemplates(Context context) throws SQLException {
    context.execute(
      newTableBuilder("loaded_templates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("kee").setLimit(200).build())
        .addColumn(newLenientVarcharBuilder("template_type").setLimit(15).build())
        .build());
  }

  private void createMetrics(Context context) throws SQLException {
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(64).setIsNullable(false).build();
    context.execute(
      newTableBuilder("metrics")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newLenientVarcharBuilder("description").setLimit(255).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("direction").setIsNullable(false).setDefaultValue(0).build())
        .addColumn(newLenientVarcharBuilder("domain").setLimit(64).build())
        .addColumn(newLenientVarcharBuilder("short_name").setLimit(64).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("qualitative").setDefaultValue(false).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("val_type").setLimit(8).build())
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
      newTableBuilder("dashboards")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("user_id").build())
        .addColumn(newLenientVarcharBuilder("name").setLimit(256).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(1000).build())
        .addColumn(newLenientVarcharBuilder("column_layout").setLimit(20).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("shared").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_global").build())
        .build());
  }

  private void createUsers(Context context) throws SQLException {
    VarcharColumnDef loginCol = newLenientVarcharBuilder("login").setLimit(255).build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").build();
    context.execute(
      newTableBuilder("users")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(loginCol)
        .addColumn(newLenientVarcharBuilder("name").setLimit(200).build())
        .addColumn(newLenientVarcharBuilder("email").setLimit(100).build())
        .addColumn(newLenientVarcharBuilder("crypted_password").setLimit(40).build())
        .addColumn(newLenientVarcharBuilder("salt").setLimit(40).build())
        .addColumn(newLenientVarcharBuilder("remember_token").setLimit(500).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("remember_token_expires_at").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("active").setDefaultValue(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(updatedAtCol)
        .addColumn(newLenientVarcharBuilder("scm_accounts").setLimit(4000).build())
        .addColumn(newLenientVarcharBuilder("external_identity").setLimit(255).build())
        .addColumn(newLenientVarcharBuilder("external_identity_provider").setLimit(100).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("user_local").build())
        .build());
    addIndex(context, "users", "users_login", true, loginCol);
    addIndex(context, "users", "users_updated_at", false, updatedAtCol);
  }

  private void createActiveRuleParameters(Context context) throws SQLException {
    context.execute(
      newTableBuilder("active_rule_parameters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("active_rule_id").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rules_parameter_id").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("value").setLimit(4000).build())
        .addColumn(newLenientVarcharBuilder("rules_parameter_key").setLimit(128).build())
        .build());
  }

  private void createActiveRules(Context context) throws SQLException {
    IntegerColumnDef profileIdCol = newIntegerColumnDefBuilder().setColumnName("profile_id").setIsNullable(false).build();
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder("active_rules")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(profileIdCol)
        .addColumn(ruleIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("failure_level").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("inheritance").setLimit(10).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .build());
    addIndex(context, "active_rules", "uniq_profile_rule_ids", true, profileIdCol, ruleIdCol);
  }

  private void createUserRoles(Context context) throws SQLException {
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").build();
    context.execute(
      newTableBuilder("user_roles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(userIdCol)
        .addColumn(resourceIdCol)
        .addColumn(newLenientVarcharBuilder("role").setLimit(64).setIsNullable(false).build())
        .build());
    addIndex(context, "user_roles", "user_roles_resource", false, resourceIdCol);
    addIndex(context, "user_roles", "user_roles_user", false, userIdCol);
  }

  private void createActiveDashboards(Context context) throws SQLException {
    IntegerColumnDef dashboardIdCol = newIntegerColumnDefBuilder().setColumnName("dashboard_id").setIsNullable(false).build();
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").build();
    context.execute(
      newTableBuilder("active_dashboards")
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
      newTableBuilder("notifications")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newBlobColumnDefBuilder().setColumnName("data").build())
        .build());
  }

  private void createSnapshots(Context context) throws SQLException {
    IntegerColumnDef projectIdCol = newIntegerColumnDefBuilder().setColumnName("project_id").setIsNullable(false).build();
    IntegerColumnDef rootProjectIdCol = newIntegerColumnDefBuilder().setColumnName("root_project_id").setIsNullable(true).build();
    IntegerColumnDef parentSnapshotIdCol = newIntegerColumnDefBuilder().setColumnName("parent_snapshot_id").setIsNullable(true).build();
    VarcharColumnDef qualifierCol = newLenientVarcharBuilder("qualifier").setLimit(10).setIsNullable(true).build();
    IntegerColumnDef rootSnapshotIdCol = newIntegerColumnDefBuilder().setColumnName("root_snapshot_id").setIsNullable(true).build();
    context.execute(
      newTableBuilder("snapshots")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectIdCol)
        .addColumn(parentSnapshotIdCol)
        .addColumn(newLenientVarcharBuilder("status").setLimit(4).setIsNullable(false).setDefaultValue("U").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("islast").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newLenientVarcharBuilder("scope").setLimit(3).setIsNullable(true).build())
        .addColumn(qualifierCol)
        .addColumn(rootSnapshotIdCol)
        .addColumn(newLenientVarcharBuilder("version").setLimit(500).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("path").setLimit(500).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("depth").setIsNullable(true).build())
        .addColumn(rootProjectIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("purge_status").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period1_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period1_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period2_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period2_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period3_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period3_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period4_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period4_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period5_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("period5_param").setLimit(100).setIsNullable(true).build())
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
      newTableBuilder("groups")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(500).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(200).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
  }

  private void createWidgets(Context context) throws SQLException {
    IntegerColumnDef dashboardId = newIntegerColumnDefBuilder().setColumnName("dashboard_id").setIsNullable(false).build();
    VarcharColumnDef widgetKey = newLenientVarcharBuilder("widget_key").setLimit(256).setIsNullable(false).build();
    context.execute(
      newTableBuilder("widgets")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(dashboardId)
        .addColumn(widgetKey)
        .addColumn(newLenientVarcharBuilder("name").setLimit(256).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(1000).setIsNullable(true).build())
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
    VarcharColumnDef projectUuid = newLenientVarcharBuilder("project_uuid").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef profileKey = newLenientVarcharBuilder("profile_key").setLimit(50).setIsNullable(false).build();
    context.execute(
      newTableBuilder("project_qprofiles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectUuid)
        .addColumn(profileKey)
        .build());
    addIndex(context, "project_qprofiles", "uniq_project_qprofiles", true, projectUuid, profileKey);
  }

  private void createRulesProfiles(Context context) throws SQLException {
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder("rules_profiles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(keeCol)
        .addColumn(newLenientVarcharBuilder("parent_kee").setLimit(255).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("rules_updated_at").setLimit(100).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_default").setIsNullable(false).build())
        .build());
    addIndex(context, "rules_profiles", "uniq_qprof_key", true, keeCol);
  }

  private void createRulesParameters(Context context) throws SQLException {
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder("rules_parameters")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(ruleIdCol)
        .addColumn(newLenientVarcharBuilder("name").setLimit(128).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("param_type").setLimit(512).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("default_value").setLimit(4000).setIsNullable(true).build())
        .build());
    addIndex(context, "rules_parameters", "rules_parameters_rule_id", false, ruleIdCol);
  }

  private void createGroupsUsers(Context context) throws SQLException {
    BigIntegerColumnDef userIdCol = newBigIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build();
    BigIntegerColumnDef groupIdCol = newBigIntegerColumnDefBuilder().setColumnName("group_id").setIsNullable(true).build();
    context.execute(
      newTableBuilder("groups_users")
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
      newTableBuilder("project_measures")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
        .addColumn(metricIdCol)
        .addColumn(snapshotIdCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rules_category_id").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("text_value").setLimit(4000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("tendency").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("measure_date").build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("project_id").build())
        .addColumn(newLenientVarcharBuilder("alert_status").setLimit(5).build())
        .addColumn(newLenientVarcharBuilder("alert_text").setLimit(4000).build())
        .addColumn(newLenientVarcharBuilder("url").setLimit(2000).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).build())
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
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(50).build();
    context.execute(
      newTableBuilder("manual_measures")
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(false).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
        .addColumn(newLenientVarcharBuilder("text_value").setLimit(4000).build())
        .addColumn(newLenientVarcharBuilder("user_login").setLimit(255).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(componentUuidCol)
        .build());
    addIndex(context, "manual_measures", "manual_measures_component_uuid", false, componentUuidCol);
  }

  private void createProjects(Context context) throws SQLException {
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(400).build();
    VarcharColumnDef moduleUuidCol = newLenientVarcharBuilder("module_uuid").setLimit(50).build();
    VarcharColumnDef projectUuidCol = newLenientVarcharBuilder("project_uuid").setLimit(50).build();
    VarcharColumnDef qualifierCol = newLenientVarcharBuilder("qualifier").setLimit(10).build();
    IntegerColumnDef rootIdCol = newIntegerColumnDefBuilder().setColumnName("root_id").build();
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(50).build();
    context.execute(
      newTableBuilder("projects")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(2000).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(2000).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("enabled").setDefaultValue(true).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("scope").setLimit(3).build())
        .addColumn(qualifierCol)
        .addColumn(keeCol)
        .addColumn(rootIdCol)
        .addColumn(newLenientVarcharBuilder("language").setLimit(20).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("copy_resource_id").build())
        .addColumn(newLenientVarcharBuilder("long_name").setLimit(2000).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("person_id").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newLenientVarcharBuilder("path").setLimit(2000).build())
        .addColumn(newLenientVarcharBuilder("deprecated_kee").setLimit(400).build())
        .addColumn(uuidCol)
        .addColumn(projectUuidCol)
        .addColumn(moduleUuidCol)
        .addColumn(newLenientVarcharBuilder("module_uuid_path").setLimit(4000).build())
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
    VarcharColumnDef roleCol = newLenientVarcharBuilder("role").setLimit(64).setIsNullable(false).build();
    context.execute(
      newTableBuilder("group_roles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(groupIdCol)
        .addColumn(resourceIdCol)
        .addColumn(roleCol)
        .build());
    addIndex(context, "group_roles", "group_roles_resource", false, resourceIdCol);
    addIndex(context, "group_roles", "uniq_group_roles", true, groupIdCol, resourceIdCol, roleCol);
  }

  private void createRules(Context context) throws SQLException {
    VarcharColumnDef pluginRuleKeyCol = newLenientVarcharBuilder("plugin_rule_key").setLimit(200).setIsNullable(false).build();
    VarcharColumnDef pluginNameCol = newLenientVarcharBuilder("plugin_name").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder("rules")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginRuleKeyCol)
        .addColumn(newLenientVarcharBuilder("plugin_config_key").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginNameCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("description").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("priority").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("status").setLimit(40).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("note_created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("note_updated_at").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("note_user_login").setLimit(255).setIsNullable(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("note_data").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("remediation_function").setLimit(200).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("def_remediation_function").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("def_remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("def_remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("gap_description").setLimit(4000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("tags").setLimit(4000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("system_tags").setLimit(4000).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_template").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newLenientVarcharBuilder("description_format").setLimit(20).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("rule_type").setIsNullable(true).build())
        .build());
    addIndex(context, "rules", "rules_repo_key", true, pluginRuleKeyCol, pluginNameCol);
  }

  private void createWidgetProperties(Context context) throws SQLException {
    IntegerColumnDef widgetIdCol = newIntegerColumnDefBuilder().setColumnName("widget_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder("widget_properties")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(widgetIdCol)
        .addColumn(newLenientVarcharBuilder("kee").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("text_value").setLimit(4000).setIsNullable(true).build())
        .build());
    addIndex(context, "widget_properties", "widget_properties_widgets", false, widgetIdCol);
  }

  private void createEvents(Context context) throws SQLException {
    VarcharColumnDef componentUuid = newLenientVarcharBuilder("component_uuid").setLimit(50).setIsNullable(true).build();
    IntegerColumnDef snapshotId = newIntegerColumnDefBuilder().setColumnName("snapshot_id").setIsNullable(true).build();
    context.execute(
      newTableBuilder("events")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(400).setIsNullable(true).build())
        .addColumn(snapshotId)
        .addColumn(newLenientVarcharBuilder("category").setLimit(50).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(4000).build())
        .addColumn(newLenientVarcharBuilder("event_data").setLimit(4000).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("event_date").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(componentUuid)
        .build());
    addIndex(context, "events", "events_component_uuid", false, componentUuid);
    addIndex(context, "events", "events_snapshot_id", false, snapshotId);
  }

  private void createQualityGates(Context context) throws SQLException {
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build();
    context.execute(
      newTableBuilder("quality_gates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(nameCol)
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
    addIndex(context, "quality_gates", "uniq_quality_gates", true, nameCol);
  }

  private void createQualityGateConditions(Context context) throws SQLException {
    context.execute(
      newTableBuilder("quality_gate_conditions")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("qgate_id").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("period").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("operator").setLimit(3).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("value_error").setLimit(64).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("value_warning").setLimit(64).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
  }

  private void createProperties(Context context) throws SQLException {
    VarcharColumnDef propKey = newLenientVarcharBuilder("prop_key").setLimit(512).setIsNullable(true).build();
    context.execute(
      newTableBuilder("properties")
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
      newTableBuilder("project_links")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("link_type").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("name").setLimit(128).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("href").setLimit(2048).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("component_uuid").setLimit(2048).setIsNullable(true).build())
        .build());
  }

  private void createDuplicationsIndex(Context context) throws SQLException {
    VarcharColumnDef hashCol = newLenientVarcharBuilder("hash").setLimit(50).setIsNullable(false).build();
    IntegerColumnDef snapshotIdCol = newIntegerColumnDefBuilder().setColumnName("snapshot_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder("duplications_index")
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

  private static VarcharColumnDef.Builder newLenientVarcharBuilder(String column) {
    return new VarcharColumnDef.Builder().setColumnName(column).setIgnoreOracleUnit(true);
  }

  private CreateTableBuilder newTableBuilder(String tableName) {
    return new CreateTableBuilder(getDialect(), tableName);
  }
}
