/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v00;

import java.sql.SQLException;
import org.sonar.db.Database;
import org.sonar.server.platform.db.migration.def.BigIntegerColumnDef;
import org.sonar.server.platform.db.migration.def.BooleanColumnDef;
import org.sonar.server.platform.db.migration.def.ColumnDef;
import org.sonar.server.platform.db.migration.def.IntegerColumnDef;
import org.sonar.server.platform.db.migration.def.TinyIntColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TinyIntColumnDef.newTinyIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_VARCHAR_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;
import static org.sonar.server.platform.db.migration.sql.CreateTableBuilder.ColumnFlag.AUTO_INCREMENT;

public class CreateInitialSchema extends DdlChange {

  public CreateInitialSchema(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    createActiveRuleParameters(context);
    createActiveRules(context);
    createAlmAppInstalls(context);
    createAnalysisProperties(context);
    createCeActivity(context);
    createCeQueue(context);
    createCeScannerContext(context);
    createCeTaskCharacteristics(context);
    createCeTaskInput(context);
    createCeTaskMessage(context);
    createDefaultQProfiles(context);
    createDeprecatedRuleKeys(context);
    createDuplicationsIndex(context);
    createEsQueue(context);
    createEventComponentChanges(context);
    createEvents(context);
    createFileSources(context);
    createGroupRoles(context);
    createGroups(context);
    createGroupsUsers(context);
    createInternalComponentProps(context);
    createInternalProperties(context);
    createIssueChanges(context);
    createIssues(context);
    createLiveMeasures(context);
    createManualMeasures(context);
    createMetrics(context);
    createNotifications(context);
    createOrgQProfiles(context);
    createOrgQualityGates(context);
    createOrganizationAlmBindings(context);
    createOrganizationMembers(context);
    createOrganizations(context);
    createPermTemplatesGroups(context);
    createPermTemplatesUsers(context);
    createPermTemplatesCharacteristics(context);
    createPermissionTemplates(context);
    createPlugins(context);
    createProjectAlmBindings(context);
    createProjectBranches(context);
    createProjectLinks(context);
    createProjectMappings(context);
    createProjectMeasures(context);
    createProjectQprofiles(context);
    createProjects(context);
    createProperties(context);
    createQProfileChanges(context);
    createQProfileEditGroups(context);
    createQProfileEditUsers(context);
    createQualityGateConditions(context);
    createQualityGates(context);
    createRulesRepository(context);
    createRules(context);
    createRulesMetadata(context);
    createRulesParameters(context);
    createRulesProfiles(context);
    createSnapshots(context);
    createUserProperties(context);
    createUserRoles(context);
    createUserTokens(context);
    createUsers(context);
    createWebhookDeliveries(context);
    createWebhooks(context);
  }

  private void createActiveRuleParameters(Context context) {
    String tableName = "active_rule_parameters";
    IntegerColumnDef activeRuleIdColumnDef = newIntegerColumnDefBuilder().setColumnName("active_rule_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(activeRuleIdColumnDef)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("rules_parameter_id").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("value").setLimit(MAX_SIZE).build())
        .addColumn(newLenientVarcharBuilder("rules_parameter_key").setLimit(128).build())
        .build());
    addIndex(context, tableName, "ix_arp_on_active_rule_id", false, activeRuleIdColumnDef);
  }

  private void createActiveRules(Context context) {
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

  private void createAlmAppInstalls(Context context) {
    String tableName = "alm_app_installs";
    VarcharColumnDef almIdCol = newVarcharColumnBuilder("alm_id").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef ownerCol = newVarcharColumnBuilder("owner_id").setIsNullable(false).setLimit(MAX_SIZE).build();
    VarcharColumnDef installCol = newVarcharColumnBuilder("install_id").setIsNullable(false).setLimit(MAX_SIZE).build();
    VarcharColumnDef userExternalIdCol = newVarcharColumnBuilder("user_external_id").setLimit(255).setIsNullable(true).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(almIdCol)
      .addColumn(ownerCol)
      .addColumn(installCol)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("is_owner_user").setIsNullable(false).build())
      .addColumn(userExternalIdCol)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "alm_app_installs_owner", true, almIdCol, ownerCol);
    addIndex(context, tableName, "alm_app_installs_install", true, almIdCol, installCol);
    addIndex(context, tableName, "alm_app_installs_external_id", false, userExternalIdCol);
  }

  private void createAnalysisProperties(Context context) {
    String tableName = "analysis_properties";
    VarcharColumnDef snapshotUuidColumn = newVarcharColumnBuilder("snapshot_uuid")
      .setIsNullable(false)
      .setLimit(UUID_SIZE)
      .build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
        .addColumn(snapshotUuidColumn)
        .addColumn(newVarcharColumnBuilder("kee").setIsNullable(false).setLimit(512).build())
        .addColumn(newVarcharColumnBuilder("text_value").setIsNullable(true).setLimit(MAX_SIZE).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("clob_value").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_empty").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "ix_snapshot_uuid", false, snapshotUuidColumn);
  }

  private void createCeActivity(Context context) {
    String tableName = "ce_activity";
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef mainComponentUuidCol = newVarcharColumnBuilder("main_component_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnBuilder("component_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef statusCol = newLenientVarcharBuilder("status").setLimit(15).setIsNullable(false).build();
    BooleanColumnDef isLastCol = newBooleanColumnDefBuilder().setColumnName("is_last").setIsNullable(false).build();
    VarcharColumnDef isLastKeyCol = newLenientVarcharBuilder("is_last_key").setLimit(55).setIsNullable(false).build();
    BooleanColumnDef mainIsLastCol = newBooleanColumnDefBuilder().setColumnName("main_is_last").setIsNullable(false).build();
    VarcharColumnDef mainIsLastKeyCol = newLenientVarcharBuilder("main_is_last_key").setLimit(55).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newLenientVarcharBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(mainComponentUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(statusCol)
        .addColumn(mainIsLastCol)
        .addColumn(mainIsLastKeyCol)
        .addColumn(isLastCol)
        .addColumn(isLastKeyCol)
        .addColumn(newLenientVarcharBuilder("submitter_uuid").setLimit(255).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("submitted_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("executed_at").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("execution_count").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("execution_time_ms").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("analysis_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("error_message").setLimit(1_000).setIsNullable(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("error_stacktrace").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("error_type").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("worker_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "ce_activity_component", false, componentUuidCol);
    addIndex(context, tableName, "ce_activity_islast", false, isLastCol, statusCol);
    addIndex(context, tableName, "ce_activity_islast_key", false, isLastKeyCol);
    addIndex(context, tableName, "ce_activity_main_component", false, mainComponentUuidCol);
    addIndex(context, tableName, "ce_activity_main_islast", false, mainIsLastCol, statusCol);
    addIndex(context, tableName, "ce_activity_main_islast_key", false, mainIsLastKeyCol);
    addIndex(context, tableName, "ce_activity_uuid", true, uuidCol);
  }

  private void createCeQueue(Context context) {
    String tableName = "ce_queue";
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef mainComponentUuidCol = newLenientVarcharBuilder("main_component_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newLenientVarcharBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(mainComponentUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(newLenientVarcharBuilder("status").setLimit(15).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("submitter_uuid").setLimit(255).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("worker_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("execution_count").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "ce_queue_main_component", false, mainComponentUuidCol);
    addIndex(context, tableName, "ce_queue_component", false, componentUuidCol);
    addIndex(context, tableName, "ce_queue_uuid", true, uuidCol);
  }

  private void createCeScannerContext(Context context) {
    context.execute(
      newTableBuilder("ce_scanner_context")
        .addPkColumn(newLenientVarcharBuilder("task_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("context_data").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
  }

  private void createCeTaskCharacteristics(Context context) {
    String tableName = "ce_task_characteristics";
    VarcharColumnDef ceTaskUuidColumn = newLenientVarcharBuilder("task_uuid")
      .setLimit(UUID_SIZE)
      .setIsNullable(false)
      .build();

    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newLenientVarcharBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(ceTaskUuidColumn)
        .addColumn(newLenientVarcharBuilder("kee").setLimit(512).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("text_value").setLimit(512).setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "ce_characteristics_" + ceTaskUuidColumn.getName(), false, ceTaskUuidColumn);
  }

  private void createCeTaskInput(Context context) {
    context.execute(
      newTableBuilder("ce_task_input")
        .addPkColumn(newLenientVarcharBuilder("task_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("input_data").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
  }

  private void createCeTaskMessage(Context context) {
    String tableName = "ce_task_message";
    VarcharColumnDef taskUuidCol = newVarcharColumnBuilder("task_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(taskUuidCol)
      .addColumn(newVarcharColumnBuilder("message").setIsNullable(false).setLimit(MAX_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, tableName + "_task", false, taskUuidCol);
  }

  private void createDefaultQProfiles(Context context) {
    String tableName = "default_qprofiles";
    VarcharColumnDef profileUuidColumn = newLenientVarcharBuilder("qprofile_uuid")
      .setLimit(255)
      .setIsNullable(false)
      .build();

    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newLenientVarcharBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addPkColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(false).build())
        .addColumn(profileUuidColumn)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "uniq_default_qprofiles_uuid", true, profileUuidColumn);
  }

  private void createDeprecatedRuleKeys(Context context) {
    String tableName = "deprecated_rule_keys";
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build();
    VarcharColumnDef oldRepositoryKeyCol = newVarcharColumnBuilder("old_repository_key").setIsNullable(false).setLimit(255).build();
    VarcharColumnDef oldRuleKeyCol = newVarcharColumnBuilder("old_rule_key").setIsNullable(false).setLimit(200).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(ruleIdCol)
      .addColumn(oldRepositoryKeyCol)
      .addColumn(oldRuleKeyCol)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "uniq_deprecated_rule_keys", true, oldRepositoryKeyCol, oldRuleKeyCol);
    addIndex(context, tableName, "rule_id_deprecated_rule_keys", true, ruleIdCol);
  }

  private void createDuplicationsIndex(Context context) {
    String tableName = "duplications_index";
    VarcharColumnDef hashCol = newLenientVarcharBuilder("hash").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef analysisUuidCol = newLenientVarcharBuilder("analysis_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(analysisUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(hashCol)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("index_in_file").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("start_line").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("end_line").setIsNullable(false).build())
        .build());

    addIndex(context, tableName, "duplications_index_hash", false, hashCol);
    addIndex(context, tableName, "duplication_analysis_component", false, analysisUuidCol, componentUuidCol);
  }

  private void createEsQueue(Context context) {
    String tableName = "es_queue";
    BigIntegerColumnDef createdAtCol = newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
        .addColumn(newVarcharColumnBuilder("doc_type").setIsNullable(false).setLimit(40).build())
        .addColumn(newVarcharColumnBuilder("doc_id").setIsNullable(false).setLimit(MAX_SIZE).build())
        .addColumn(newVarcharColumnBuilder("doc_id_type").setIsNullable(true).setLimit(20).build())
        .addColumn(newVarcharColumnBuilder("doc_routing").setIsNullable(true).setLimit(MAX_SIZE).build())
        .addColumn(createdAtCol)
        .build());
    addIndex(context, tableName, "es_queue_created_at", false, createdAtCol);
  }

  private void createEventComponentChanges(Context context) {
    String tableName = "event_component_changes";
    VarcharColumnDef eventUuidCol = newVarcharColumnBuilder("event_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef eventComponentUuidCol = newVarcharColumnBuilder("event_component_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    VarcharColumnDef eventAnalysisUuidCol = newVarcharColumnBuilder("event_analysis_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    VarcharColumnDef changeCategoryCol = newVarcharColumnBuilder("change_category").setIsNullable(false).setLimit(12).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnBuilder("component_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(eventUuidCol)
      .addColumn(eventComponentUuidCol)
      .addColumn(eventAnalysisUuidCol)
      .addColumn(changeCategoryCol)
      .addColumn(componentUuidCol)
      .addColumn(newVarcharColumnBuilder("component_key").setIsNullable(false).setLimit(400).build())
      .addColumn(newVarcharColumnBuilder("component_name").setIsNullable(false).setLimit(2000).build())
      .addColumn(newVarcharColumnBuilder("component_branch_key").setIsNullable(true).setLimit(255).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, tableName + "_unique", true, eventUuidCol, changeCategoryCol, componentUuidCol);
    addIndex(context, tableName, "event_cpnt_changes_cpnt", false, eventComponentUuidCol);
    addIndex(context, tableName, "event_cpnt_changes_analysis", false, eventAnalysisUuidCol);
  }

  private void createEvents(Context context) {
    String tableName = "events";
    VarcharColumnDef uuidCol = newVarcharColumnBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef analysisUuidCol = newLenientVarcharBuilder("analysis_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuid = newLenientVarcharBuilder("component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(analysisUuidCol)
        .addColumn(newLenientVarcharBuilder("name").setLimit(400).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("category").setLimit(50).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(MAX_SIZE).build())
        .addColumn(newLenientVarcharBuilder("event_data").setLimit(MAX_SIZE).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("event_date").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(componentUuid)
        .build());
    addIndex(context, tableName, "events_uuid", true, uuidCol);
    addIndex(context, tableName, "events_analysis", false, analysisUuidCol);
    addIndex(context, tableName, "events_component_uuid", false, componentUuid);
  }

  private void createFileSources(Context context) {
    String tableName = "file_sources";
    VarcharColumnDef projectUuidCol = newLenientVarcharBuilder("project_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build();
    VarcharColumnDef fileUuidCol = newLenientVarcharBuilder("file_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectUuidCol)
        .addColumn(fileUuidCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("line_hashes").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line_hashes_version").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("data_hash").setLimit(50).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("src_hash").setLimit(50).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("revision").setLimit(100).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line_count").setIsNullable(false).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("binary_data").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(updatedAtCol)
        .build());
    addIndex(context, tableName, "file_sources_file_uuid", true, fileUuidCol);
    addIndex(context, tableName, "file_sources_project_uuid", false, projectUuidCol);
    addIndex(context, tableName, "file_sources_updated_at", false, updatedAtCol);
  }

  private void createGroupRoles(Context context) {
    String tableName = "group_roles";
    IntegerColumnDef groupIdCol = newIntegerColumnDefBuilder().setColumnName("group_id").setIsNullable(true).build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build();
    VarcharColumnDef roleCol = newLenientVarcharBuilder("role").setLimit(64).setIsNullable(false).build();
    VarcharColumnDef organizationUuidCol = newLenientVarcharBuilder("organization_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(organizationUuidCol)
        .addColumn(groupIdCol)
        .addColumn(resourceIdCol)
        .addColumn(roleCol)
        .build());
    addIndex(context, tableName, "uniq_group_roles", true, organizationUuidCol, groupIdCol, resourceIdCol, roleCol);
    addIndex(context, tableName, "group_roles_resource", false, resourceIdCol);
  }

  private void createGroups(Context context) {
    context.execute(
      newTableBuilder("groups")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("organization_uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
        .addColumn(newLenientVarcharBuilder("name").setLimit(500).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(200).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
  }

  private void createGroupsUsers(Context context) {
    String tableName = "groups_users";
    BigIntegerColumnDef userIdCol = newBigIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build();
    BigIntegerColumnDef groupIdCol = newBigIntegerColumnDefBuilder().setColumnName("group_id").setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addColumn(userIdCol)
        .addColumn(groupIdCol)
        .build());
    addIndex(context, tableName, "index_groups_users_on_user_id", false, userIdCol);
    addIndex(context, tableName, "index_groups_users_on_group_id", false, groupIdCol);
    addIndex(context, tableName, "groups_users_unique", true, groupIdCol, userIdCol);
  }

  private void createInternalComponentProps(Context context) {
    String tableName = "internal_component_props";
    VarcharColumnDef componentUuidCol = newVarcharColumnBuilder("component_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    VarcharColumnDef keeCol = newVarcharColumnBuilder("kee").setIsNullable(false).setLimit(512).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(componentUuidCol)
      .addColumn(keeCol)
      .addColumn(newVarcharColumnBuilder("value").setIsNullable(true).setLimit(MAX_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "unique_component_uuid_kee", true, componentUuidCol, keeCol);
  }

  private void createInternalProperties(Context context) {
    context.execute(
      newTableBuilder("internal_properties")
        .addPkColumn(newLenientVarcharBuilder("kee").setLimit(20).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_empty").setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName("text_value").setLimit(MAX_SIZE).setIgnoreOracleUnit(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("clob_value").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .build());
  }

  private void createIssueChanges(Context context) {
    String tableName = "issue_changes";
    VarcharColumnDef issueKeyCol = newLenientVarcharBuilder("issue_key").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(50).build();
    context.execute(
      newTableBuilder(tableName)
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
    addIndex(context, tableName, "issue_changes_issue_key", false, issueKeyCol);
    addIndex(context, tableName, "issue_changes_kee", false, keeCol);
  }

  private void createIssues(Context context) {
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
        .addColumn(newVarcharColumnBuilder("message").setLimit(MAX_SIZE).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line").build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("gap").setPrecision(30).setScale(20).build())
        .addColumn(newLenientVarcharBuilder("status").setLimit(20).build())
        .addColumn(resolutionCol)
        .addColumn(newLenientVarcharBuilder("checksum").setLimit(1000).build())
        .addColumn(newLenientVarcharBuilder("reporter").setLimit(255).build())
        .addColumn(assigneeCol)
        .addColumn(newLenientVarcharBuilder("author_login").setLimit(255).build())
        .addColumn(newLenientVarcharBuilder("action_plan_key").setLimit(50).build())
        .addColumn(newLenientVarcharBuilder("issue_attributes").setLimit(MAX_SIZE).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("effort").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(updatedAtCol)
        .addColumn(issueCreationDateCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_update_date").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_close_date").build())
        .addColumn(newLenientVarcharBuilder("tags").setLimit(MAX_SIZE).build())
        .addColumn(componentUuidCol)
        .addColumn(projectUuidCol)
        .addColumn(newBlobColumnDefBuilder().setColumnName("locations").build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("issue_type").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("from_hotspot").setIsNullable(true).build())
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

  private void createLiveMeasures(Context context) {
    String tableName = "live_measures";
    VarcharColumnDef projectUuidCol = newVarcharColumnBuilder("project_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnBuilder("component_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    IntegerColumnDef metricIdCol = newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(projectUuidCol)
      .addColumn(componentUuidCol)
      .addColumn(metricIdCol)
      .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
      .addColumn(newVarcharColumnBuilder("text_value").setIsNullable(true).setLimit(MAX_SIZE).build())
      .addColumn(newDecimalColumnDefBuilder().setColumnName("variation").setPrecision(38).setScale(20).build())
      .addColumn(newBlobColumnDefBuilder().setColumnName("measure_data").setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("update_marker").setIsNullable(true).setLimit(UUID_SIZE).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "live_measures_project", false, projectUuidCol);
    addIndex(context, tableName, "live_measures_component", true, componentUuidCol, metricIdCol);
  }

  private void createManualMeasures(Context context) {
    String tableName = "manual_measures";
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(false).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
        .addColumn(newLenientVarcharBuilder("text_value").setLimit(MAX_SIZE).build())
        .addColumn(newLenientVarcharBuilder("user_uuid").setLimit(255).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(MAX_SIZE).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(componentUuidCol)
        .build());
    addIndex(context, tableName, "manual_measures_component_uuid", false, componentUuidCol);
  }

  private void createMetrics(Context context) {
    String tableName = "metrics";
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(64).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
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
    addIndex(context, tableName, "metrics_unique_name", true, nameCol);
  }

  private void createNotifications(Context context) {
    context.execute(
      newTableBuilder("notifications")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newBlobColumnDefBuilder().setColumnName("data").build())
        .build());
  }

  private void createOrgQProfiles(Context context) {
    String tableName = "org_qprofiles";
    int profileUuidSize = 255;
    VarcharColumnDef organizationCol = newLenientVarcharBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef rulesProfileUuidCol = newLenientVarcharBuilder("rules_profile_uuid").setLimit(profileUuidSize).setIsNullable(false).build();
    VarcharColumnDef parentUuidCol = newLenientVarcharBuilder("parent_uuid").setLimit(profileUuidSize).setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnBuilder("uuid").setLimit(profileUuidSize).setIsNullable(false).build())
        .addColumn(organizationCol)
        .addColumn(rulesProfileUuidCol)
        .addColumn(parentUuidCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_used").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("user_updated_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "qprofiles_org_uuid", false, organizationCol);
    addIndex(context, tableName, "qprofiles_rp_uuid", false, rulesProfileUuidCol);
    addIndex(context, tableName, "org_qprofiles_parent_uuid", false, parentUuidCol);
  }

  private void createOrgQualityGates(Context context) {
    String tableName = "org_quality_gates";
    VarcharColumnDef organizationUuidCol = newVarcharColumnBuilder("organization_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef qualityGateUuidCol = newVarcharColumnBuilder("quality_gate_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(organizationUuidCol)
      .addColumn(qualityGateUuidCol)
      .build());
    addIndex(context, tableName, "uniq_org_quality_gates", true, organizationUuidCol, qualityGateUuidCol);
  }

  private void createOrganizationAlmBindings(Context context) {
    String tableName = "organization_alm_bindings";
    VarcharColumnDef organizationUuidCol = newVarcharColumnBuilder("organization_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef almAppInstallUuidCol = newVarcharColumnBuilder("alm_app_install_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(organizationUuidCol)
      .addColumn(almAppInstallUuidCol)
      .addColumn(newVarcharColumnBuilder("alm_id").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnBuilder("url").setIsNullable(false).setLimit(2000).build())
      .addColumn(newVarcharColumnBuilder("user_uuid").setIsNullable(false).setLimit(255).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("members_sync_enabled").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "org_alm_bindings_org", true, organizationUuidCol);
    addIndex(context, tableName, "org_alm_bindings_install", true, almAppInstallUuidCol);
  }

  private void createOrganizationMembers(Context context) {
    String tableName = "organization_members";
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addPkColumn(userIdCol)
        .build());
    addIndex(context, tableName, "ix_org_members_on_user_id", false, userIdCol);
  }

  private void createOrganizations(Context context) {
    String tableName = "organizations";
    VarcharColumnDef keeColumn = newLenientVarcharBuilder("kee").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newLenientVarcharBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(keeColumn)
        .addColumn(newLenientVarcharBuilder("name").setLimit(255).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(256).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("url").setLimit(256).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("avatar_url").setLimit(256).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("guarded").setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("default_group_id").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("default_quality_gate_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newVarcharColumnBuilder("default_perm_template_project").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("default_perm_template_app").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("default_perm_template_port").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("new_project_private").setIsNullable(false).build())
        .addColumn(newVarcharColumnBuilder("subscription").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "organization_key", true, keeColumn);
  }

  private void createPermTemplatesGroups(Context context) {
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

  private void createPermTemplatesUsers(Context context) {
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

  private void createPermTemplatesCharacteristics(Context context) {
    String tableName = "perm_tpl_characteristics";
    IntegerColumnDef templateIdColumn = newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(false).build();
    VarcharColumnDef permissionKeyColumn = newLenientVarcharBuilder("permission_key").setLimit(64).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(templateIdColumn)
        .addColumn(permissionKeyColumn)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("with_project_creator").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());

    addIndex(context, tableName, "uniq_perm_tpl_charac", true, templateIdColumn, permissionKeyColumn);
  }

  private void createPermissionTemplates(Context context) {
    context.execute(
      newTableBuilder("permission_templates")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("organization_uuid").setIsNullable(false).setLimit(40).build())
        .addColumn(newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("kee").setLimit(100).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(MAX_SIZE).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").build())
        .addColumn(newLenientVarcharBuilder("key_pattern").setLimit(500).build())
        .build());
  }

  private void createPlugins(Context context) {
    int pluginKeyMaxSize = 200;
    String tableName = "plugins";
    VarcharColumnDef keyColumn = newVarcharColumnBuilder("kee").setLimit(pluginKeyMaxSize).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newLenientVarcharBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(keyColumn)
        .addColumn(newVarcharColumnBuilder("base_plugin_key").setLimit(pluginKeyMaxSize).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("file_hash").setLimit(200).setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "plugins_key", true, keyColumn);
  }

  private void createProjectAlmBindings(Context context) {
    String tableName = "project_alm_bindings";
    VarcharColumnDef almIdCol = newVarcharColumnDefBuilder().setColumnName("alm_id").setIsNullable(false).setLimit(40).build();
    VarcharColumnDef repoIdCol = newVarcharColumnDefBuilder().setColumnName("repo_id").setIsNullable(false).setLimit(256).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder().setColumnName("project_uuid").setIsNullable(false).setLimit(40).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(almIdCol)
      .addColumn(repoIdCol)
      .addColumn(projectUuidCol)
      .addColumn(newVarcharColumnBuilder("github_slug").setIsNullable(true).setLimit(256).build())
      .addColumn(newVarcharColumnBuilder("url").setIsNullable(false).setLimit(2000).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, tableName + "_alm_repo", true, almIdCol, repoIdCol);
    addIndex(context, tableName, tableName + "_project", true, projectUuidCol);
  }

  private void createProjectBranches(Context context) {
    String tableName = "project_branches";
    VarcharColumnDef projectUuidCol = newVarcharColumnBuilder("project_uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build();
    VarcharColumnDef keeCol = newVarcharColumnBuilder("kee").setIsNullable(false).setLimit(255).build();
    VarcharColumnDef keyTypeCol = newVarcharColumnBuilder("key_type").setIsNullable(false).setLimit(12).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_VARCHAR_SIZE).build())
        .addColumn(projectUuidCol)
        .addColumn(keeCol)
        .addColumn(newVarcharColumnBuilder("branch_type").setIsNullable(true).setLimit(12).build())
        .addColumn(newVarcharColumnBuilder("merge_branch_uuid").setIsNullable(true).setLimit(UUID_VARCHAR_SIZE).build())
        .addColumn(keyTypeCol)
        .addColumn(newBlobColumnDefBuilder().setColumnName("pull_request_binary").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("manual_baseline_analysis_uuid").setIsNullable(true).setLimit(UUID_SIZE).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "project_branches_kee_key_type", true, projectUuidCol, keeCol, keyTypeCol);
  }

  private void createProjectLinks(Context context) {
    String tableName = "project_links";
    VarcharColumnDef projectUuidCol = newVarcharColumnBuilder("project_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(projectUuidCol)
      .addColumn(newVarcharColumnBuilder("link_type").setLimit(20).setIsNullable(false).build())
      .addColumn(newVarcharColumnBuilder("name").setLimit(128).setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("href").setLimit(2048).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "project_links_project", false, projectUuidCol);
  }

  private void createProjectMappings(Context context) {
    String tableName = "project_mappings";
    VarcharColumnDef keyTypeCol = newVarcharColumnBuilder("key_type").setIsNullable(false).setLimit(200).build();
    VarcharColumnDef keyCol = newVarcharColumnBuilder("kee").setIsNullable(false).setLimit(MAX_SIZE).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnBuilder("project_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(keyTypeCol)
      .addColumn(keyCol)
      .addColumn(projectUuidCol)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "key_type_kee", true, keyTypeCol, keyCol);
    addIndex(context, tableName, "project_uuid", false, projectUuidCol);
  }

  private void createProjectMeasures(Context context) {
    String tableName = "project_measures";
    IntegerColumnDef personIdCol = newIntegerColumnDefBuilder().setColumnName("person_id").build();
    IntegerColumnDef metricIdCol = newIntegerColumnDefBuilder().setColumnName("metric_id").setIsNullable(false).build();
    VarcharColumnDef analysisUuidCol = newLenientVarcharBuilder("analysis_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newBigIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newDecimalColumnDefBuilder().setColumnName("value").setPrecision(38).setScale(20).build())
        .addColumn(metricIdCol)
        .addColumn(analysisUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(newLenientVarcharBuilder("text_value").setLimit(MAX_SIZE).build())
        .addColumn(newLenientVarcharBuilder("alert_status").setLimit(5).build())
        .addColumn(newLenientVarcharBuilder("alert_text").setLimit(MAX_SIZE).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(MAX_SIZE).build())
        .addColumn(personIdCol)
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_1").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_2").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_3").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_4").setPrecision(38).setScale(20).build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("variation_value_5").setPrecision(38).setScale(20).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("measure_data").build())
        .build());
    addIndex(context, tableName, "measures_analysis_metric", false, analysisUuidCol, metricIdCol);
    addIndex(context, tableName, "measures_component_uuid", false, componentUuidCol);
  }

  private void createProjectQprofiles(Context context) {
    String tableName = "project_qprofiles";
    VarcharColumnDef projectUuid = newLenientVarcharBuilder("project_uuid").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef profileKey = newLenientVarcharBuilder("profile_key").setLimit(50).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(projectUuid)
        .addColumn(profileKey)
        .build());
    addIndex(context, tableName, "uniq_project_qprofiles", true, projectUuid, profileKey);
  }

  private void createProjects(Context context) {
    String tableName = "projects";
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef organizationUuidCol = newVarcharColumnBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(400).setIsNullable(true).build();
    VarcharColumnDef qualifierCol = newLenientVarcharBuilder("qualifier").setLimit(10).setIsNullable(true).build();
    VarcharColumnDef rootUuidCol = newVarcharColumnBuilder("root_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef projectUuidCol = newLenientVarcharBuilder("project_uuid").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef moduleUuidCol = newLenientVarcharBuilder("module_uuid").setLimit(50).setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(organizationUuidCol)
        .addColumn(keeCol)
        .addColumn(newLenientVarcharBuilder("deprecated_kee").setLimit(400).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("name").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("long_name").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("description").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("enabled").setDefaultValue(true).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("scope").setLimit(3).setIsNullable(true).build())
        .addColumn(qualifierCol)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("private").setIsNullable(false).build())
        .addColumn(rootUuidCol)
        .addColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("copy_component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("developer_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("path").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("uuid_path").setLimit(1_500).setIsNullable(false).build())
        .addColumn(projectUuidCol)
        .addColumn(moduleUuidCol)
        .addColumn(newLenientVarcharBuilder("module_uuid_path").setLimit(1_500).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("authorization_updated_at").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("tags").setLimit(500).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("main_branch_project_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("b_changed").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_name").setLimit(500).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_long_name").setLimit(500).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_description").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("b_enabled").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_qualifier").setLimit(10).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_language").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_copy_component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_path").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_uuid_path").setLimit(1_500).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_module_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("b_module_uuid_path").setLimit(1_500).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "projects_organization", false, organizationUuidCol);
    addIndex(context, tableName, "projects_kee", true, keeCol);
    addIndex(context, tableName, "projects_module_uuid", false, moduleUuidCol);
    addIndex(context, tableName, "projects_project_uuid", false, projectUuidCol);
    addIndex(context, tableName, "projects_qualifier", false, qualifierCol);
    addIndex(context, tableName, "projects_root_uuid", false, rootUuidCol);
    // see SONAR-12341, index projects_uuid should actually be unique
    addIndex(context, tableName, "projects_uuid", false, uuidCol);
  }

  private void createProperties(Context context) {
    String tableName = "properties";
    VarcharColumnDef propKey = newLenientVarcharBuilder("prop_key").setLimit(512).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
      .addColumn(propKey)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("is_empty").setIsNullable(false).build())
      .addColumn(newLenientVarcharBuilder("text_value").setLimit(MAX_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("clob_value").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      // table with be renamed to properties in following migration, use final constraint name right away
      .withPkConstraintName("pk_properties")
      .build());
    addIndex(context, tableName, "properties_key", false, propKey);
  }

  private void createQProfileChanges(Context context) {
    String tableName = "qprofile_changes";
    VarcharColumnDef rulesProfileUuidCol = newLenientVarcharBuilder("rules_profile_uuid").setLimit(255).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newLenientVarcharBuilder("kee").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(rulesProfileUuidCol)
      .addColumn(newLenientVarcharBuilder("change_type").setLimit(20).setIsNullable(false).build())
      .addColumn(newLenientVarcharBuilder("user_uuid").setLimit(255).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("change_data").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "qp_changes_rules_profile_uuid", false, rulesProfileUuidCol);
  }

  private void createQProfileEditGroups(Context context) {
    String tableName = "qprofile_edit_groups";
    IntegerColumnDef groupCol = newIntegerColumnDefBuilder().setColumnName("group_id").setIsNullable(false).build();
    VarcharColumnDef qProfileUuidCol = newVarcharColumnBuilder("qprofile_uuid").setIsNullable(false).setLimit(255).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(groupCol)
      .addColumn(qProfileUuidCol)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, tableName + "_qprofile", false, qProfileUuidCol);
    addIndex(context, tableName, tableName + "_unique", true, groupCol, qProfileUuidCol);
  }

  private void createQProfileEditUsers(Context context) {
    String tableName = "qprofile_edit_users";
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(false).build();
    VarcharColumnDef qProfileUuidCol = newVarcharColumnBuilder("qprofile_uuid").setIsNullable(false).setLimit(255).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(userIdCol)
      .addColumn(qProfileUuidCol)
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, tableName + "_qprofile", false, qProfileUuidCol);
    addIndex(context, tableName, tableName + "_unique", true, userIdCol, qProfileUuidCol);
  }

  private void createQualityGateConditions(Context context) {
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

  private void createQualityGates(Context context) {
    String tableName = "quality_gates";
    VarcharColumnDef uuidCol = newVarcharColumnBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_built_in").setIsNullable(false).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "uniq_quality_gates_uuid", true, uuidCol);
  }

  private void createRulesRepository(Context context) {
    String tableName = "rule_repositories";
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newLenientVarcharBuilder("kee").setLimit(200).setIsNullable(false).build())
      .addColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(false).build())
      .addColumn(newLenientVarcharBuilder("name").setLimit(4_000).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
  }

  private void createRules(Context context) {
    VarcharColumnDef pluginRuleKeyCol = newLenientVarcharBuilder("plugin_rule_key").setLimit(200).setIsNullable(false).build();
    VarcharColumnDef pluginNameCol = newLenientVarcharBuilder("plugin_name").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder("rules")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginRuleKeyCol)
        .addColumn(newVarcharColumnBuilder("plugin_key").setLimit(200).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("plugin_config_key").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginNameCol)
        .addColumn(newVarcharColumnBuilder("scope").setLimit(20).setIsNullable(false).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("description").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("priority").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("template_id").setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("status").setLimit(40).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("def_remediation_function").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("def_remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("def_remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("gap_description").setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("system_tags").setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_template").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newLenientVarcharBuilder("description_format").setLimit(20).setIsNullable(true).build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("rule_type").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("security_standards").setIsNullable(true).setLimit(4_000).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_ad_hoc").setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_external").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
    addIndex(context, "rules", "rules_repo_key", true, pluginRuleKeyCol, pluginNameCol);
  }

  private void createRulesMetadata(Context context) {
    String tableName = "rules_metadata";
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build())
      .addPkColumn(newVarcharColumnBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("note_data").setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("note_user_uuid").setLimit(255).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("note_created_at").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("note_updated_at").setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("remediation_function").setLimit(20).setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("remediation_gap_mult").setLimit(20).setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("remediation_base_effort").setLimit(20).setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("tags").setLimit(4_000).setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("ad_hoc_name").setLimit(200).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("ad_hoc_description").setIsNullable(true).build())
      .addColumn(newVarcharColumnBuilder("ad_hoc_severity").setLimit(10).setIsNullable(true).build())
      .addColumn(newTinyIntColumnDefBuilder().setColumnName("ad_hoc_type").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .withPkConstraintName("pk_" + tableName)
      .build());
  }

  private void createRulesParameters(Context context) {
    String tableName = "rules_parameters";
    IntegerColumnDef ruleIdCol = newIntegerColumnDefBuilder().setColumnName("rule_id").setIsNullable(false).build();
    VarcharColumnDef nameCol = newLenientVarcharBuilder("name").setLimit(128).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(ruleIdCol)
        .addColumn(nameCol)
        .addColumn(newLenientVarcharBuilder("description").setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("param_type").setLimit(512).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("default_value").setLimit(MAX_SIZE).setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "rules_parameters_rule_id", false, ruleIdCol);
    addIndex(context, tableName, "rules_parameters_unique", true, ruleIdCol, nameCol);
  }

  private void createRulesProfiles(Context context) {
    VarcharColumnDef keeCol = newLenientVarcharBuilder("kee").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder("rules_profiles")
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("language").setLimit(20).setIsNullable(true).build())
        .addColumn(keeCol)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_built_in").setIsNullable(false).build())
        .addColumn(newLenientVarcharBuilder("rules_updated_at").setLimit(100).setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(newTimestampColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
        .build());
    addIndex(context, "rules_profiles", "uniq_qprof_key", true, keeCol);
  }

  private void createSnapshots(Context context) {
    String tableName = "snapshots";
    VarcharColumnDef uuidCol = newLenientVarcharBuilder("uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newLenientVarcharBuilder("component_uuid").setLimit(UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(componentUuidCol)
        .addColumn(newLenientVarcharBuilder("status").setLimit(4).setIsNullable(false).setDefaultValue("U").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("islast").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newLenientVarcharBuilder("version").setLimit(500).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("purge_status").setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("build_string").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("revision").setLimit(100).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("build_date").setIsNullable(true).build())
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
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period1_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period2_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period3_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period4_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period5_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "analyses_uuid", true, uuidCol);
    addIndex(context, tableName, "snapshot_component", false, componentUuidCol);
  }

  private void createUserProperties(Context context) {
    String tableName = "user_properties";
    VarcharColumnDef userUuidCol = newVarcharColumnBuilder("user_uuid").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef keyCol = newVarcharColumnBuilder("kee").setLimit(100).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(userUuidCol)
      .addColumn(keyCol)
      .addColumn(newVarcharColumnBuilder("text_value").setLimit(4_000).setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "user_properties_user_uuid_kee", true, userUuidCol, keyCol);
  }

  private void createUserRoles(Context context) {
    String tableName = "user_roles";
    IntegerColumnDef userIdCol = newIntegerColumnDefBuilder().setColumnName("user_id").setIsNullable(true).build();
    IntegerColumnDef resourceIdCol = newIntegerColumnDefBuilder().setColumnName("resource_id").setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(newLenientVarcharBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(userIdCol)
        .addColumn(resourceIdCol)
        .addColumn(newLenientVarcharBuilder("role").setLimit(64).setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "user_roles_resource", false, resourceIdCol);
    addIndex(context, tableName, "user_roles_user", false, userIdCol);
  }

  private void createUserTokens(Context context) {
    String tableName = "user_tokens";
    VarcharColumnDef userUuidCol = newVarcharColumnBuilder("user_uuid").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef nameCol = newVarcharColumnBuilder("name").setLimit(100).setIsNullable(false).build();
    VarcharColumnDef tokenHashCol = newVarcharColumnBuilder("token_hash").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(userUuidCol)
        .addColumn(nameCol)
        .addColumn(tokenHashCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_connection_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "user_tokens_user_uuid_name", true, userUuidCol, nameCol);
    addIndex(context, tableName, "user_tokens_token_hash", true, tokenHashCol);
  }

  private void createUsers(Context context) {
    String tableName = "users";
    VarcharColumnDef uuidCol = newVarcharColumnBuilder("uuid").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef loginCol = newLenientVarcharBuilder("login").setLimit(255).setIsNullable(false).build();
    BigIntegerColumnDef updatedAtCol = newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build();
    VarcharColumnDef externalLoginCol = newLenientVarcharBuilder("external_login").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef externalIdentityProviderCol = newLenientVarcharBuilder("external_identity_provider").setLimit(100).setIsNullable(false).build();
    VarcharColumnDef externalIdCol = newVarcharColumnBuilder("external_id").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newIntegerColumnDefBuilder().setColumnName("id").setIsNullable(false).build(), AUTO_INCREMENT)
        .addColumn(uuidCol)
        .addColumn(loginCol)
        .addColumn(newVarcharColumnBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("name").setLimit(200).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("email").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("crypted_password").setLimit(100).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("salt").setLimit(40).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("hash_method").setLimit(10).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("active").setDefaultValue(true).setIsNullable(true).build())
        .addColumn(newLenientVarcharBuilder("scm_accounts").setLimit(MAX_SIZE).build())
        .addColumn(externalLoginCol)
        .addColumn(externalIdentityProviderCol)
        .addColumn(externalIdCol)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_root").setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("user_local").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("onboarded").setIsNullable(false).build())
        .addColumn(newVarcharColumnBuilder("homepage_type").setLimit(40).setIsNullable(true).build())
        .addColumn(newVarcharColumnBuilder("homepage_parameter").setLimit(40).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_connection_date").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(true).build())
        .addColumn(updatedAtCol)
        .build());
    addIndex(context, tableName, "users_login", true, loginCol);
    addIndex(context, tableName, "users_updated_at", false, updatedAtCol);
    addIndex(context, tableName, "users_uuid", true, uuidCol);
    addIndex(context, tableName, "uniq_external_id", true, externalIdentityProviderCol, externalIdCol);
    addIndex(context, tableName, "uniq_external_login", true, externalIdentityProviderCol, externalLoginCol);
  }

  private void createWebhookDeliveries(Context context) {
    String tableName = "webhook_deliveries";
    VarcharColumnDef componentUuidColumn = newLenientVarcharBuilder("component_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef ceTaskUuidColumn = newLenientVarcharBuilder("ce_task_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newLenientVarcharBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(newVarcharColumnBuilder("webhook_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(componentUuidColumn)
      .addColumn(ceTaskUuidColumn)
      .addColumn(newVarcharColumnBuilder("analysis_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
      .addColumn(newLenientVarcharBuilder("name").setLimit(100).setIsNullable(false).build())
      .addColumn(newLenientVarcharBuilder("url").setLimit(2_000).setIsNullable(false).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("success").setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName("http_status").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("duration_ms").setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("payload").setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("error_stacktrace").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .build());
    addIndex(context, tableName, "component_uuid", false, componentUuidColumn);
    addIndex(context, tableName, "ce_task_uuid", false, ceTaskUuidColumn);
  }

  private void createWebhooks(Context context) {
    String tableName = "webhooks";
    VarcharColumnDef organizationUuidCol = newVarcharColumnBuilder("organization_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnBuilder("project_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(organizationUuidCol)
      .addColumn(projectUuidCol)
      .addColumn(newVarcharColumnBuilder("name").setLimit(100).setIsNullable(false).build())
      .addColumn(newVarcharColumnBuilder("url").setLimit(2_000).setIsNullable(false).build())
      .addColumn(newVarcharColumnBuilder("secret").setLimit(200).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("created_at").setIsNullable(false).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("updated_at").setIsNullable(true).build())
      .build());
    addIndex(context, tableName, "organization_webhook", false, organizationUuidCol);
    addIndex(context, tableName, "project_webhook", false, projectUuidCol);
  }

  private static void addIndex(Context context, String table, String index, boolean unique, ColumnDef... columns) {
    CreateIndexBuilder builder = new CreateIndexBuilder()
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

  private static VarcharColumnDef.Builder newVarcharColumnBuilder(String column) {
    return newVarcharColumnDefBuilder().setColumnName(column);
  }

  private CreateTableBuilder newTableBuilder(String tableName) {
    return new CreateTableBuilder(getDialect(), tableName);
  }
}
