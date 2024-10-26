/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
import org.sonar.server.platform.db.migration.def.TimestampColumnDef;
import org.sonar.server.platform.db.migration.def.TinyIntColumnDef;
import org.sonar.server.platform.db.migration.def.VarcharColumnDef;
import org.sonar.server.platform.db.migration.sql.CreateIndexBuilder;
import org.sonar.server.platform.db.migration.sql.CreateTableBuilder;
import org.sonar.server.platform.db.migration.step.DdlChange;

import static java.util.Arrays.stream;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.sonar.server.platform.db.migration.def.BigIntegerColumnDef.newBigIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BlobColumnDef.newBlobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.BooleanColumnDef.newBooleanColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.ClobColumnDef.newClobColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.DecimalColumnDef.newDecimalColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.IntegerColumnDef.newIntegerColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TimestampColumnDef.newTimestampColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.TinyIntColumnDef.newTinyIntColumnDefBuilder;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.DESCRIPTION_SECTION_KEY_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.MAX_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.USER_UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.UUID_SIZE;
import static org.sonar.server.platform.db.migration.def.VarcharColumnDef.newVarcharColumnDefBuilder;

public class CreateInitialSchema extends DdlChange {

  /**
   * Initially, UUID columns were created with size 50 when only 40 is needed. {@link VarcharColumnDef#UUID_SIZE}
   * should be used instead of this constant whenever reducing the column size is possible.
   */
  private static final int OLD_UUID_VARCHAR_SIZE = 50;

  // keep column name constants in alphabetic order
  private static final String ANALYSIS_UUID_COL_NAME = "analysis_uuid";
  private static final String COMPONENT_UUID_COL_NAME = "component_uuid";
  private static final String CREATED_AT_COL_NAME = "created_at";
  private static final String DESCRIPTION_COL_NAME = "description";
  private static final String GROUP_UUID_COL_NAME = "group_uuid";
  private static final String LANGUAGE_COL_NAME = "language";
  private static final String METRIC_UUID_COL_NAME = "metric_uuid";
  private static final String ORGANIZATION_UUID_COL_NAME = "organization_uuid";
  private static final String PROJECT_UUID_COL_NAME = "project_uuid";
  private static final String BRANCH_UUID_COL_NAME = "branch_uuid";
  public static final String RULE_UUID_COL_NAME = "rule_uuid";
  private static final String STATUS_COL_NAME = "status";
  private static final String TASK_UUID_COL_NAME = "task_uuid";
  private static final String TEMPLATE_UUID_COL_NAME = "template_uuid";
  private static final String TEXT_VALUE_COL_NAME = "text_value";
  private static final String UPDATED_AT_COL_NAME = "updated_at";
  private static final String USER_UUID_COL_NAME = "user_uuid";
  private static final String VALUE_COL_NAME = "value";
  private static final String PRIVATE_COL_NAME = "private";
  private static final String QPROFILE_UUID_COL_NAME = "qprofile_uuid";
  private static final String EXPIRATION_DATE_COL_NAME = "expiration_date";
  public static final String QUALITY_GATE_UUID_COL_NAME = "quality_gate_uuid";
  public static final String CLOB_VALUE_COL_NAME = "clob_value";
  public static final String IS_EMPTY_COL_NAME = "is_empty";
  private static final String UNIQUE_INDEX_SUFFIX = "_unique";

  // usual technical columns
  private static final VarcharColumnDef UUID_COL = newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(UUID_SIZE).build();

  private static final BigIntegerColumnDef TECHNICAL_CREATED_AT_COL = newBigIntegerColumnDefBuilder().setColumnName(CREATED_AT_COL_NAME).setIsNullable(false).build();
  private static final BigIntegerColumnDef NULLABLE_TECHNICAL_CREATED_AT_COL = newBigIntegerColumnDefBuilder().setColumnName(CREATED_AT_COL_NAME).setIsNullable(true).build();
  private static final BigIntegerColumnDef TECHNICAL_UPDATED_AT_COL = newBigIntegerColumnDefBuilder().setColumnName(UPDATED_AT_COL_NAME).setIsNullable(false).build();
  private static final BigIntegerColumnDef NULLABLE_TECHNICAL_UPDATED_AT_COL = newBigIntegerColumnDefBuilder().setColumnName(UPDATED_AT_COL_NAME).setIsNullable(true).build();
  private static final TimestampColumnDef DEPRECATED_TECHNICAL_CREATED_AT_COL = newTimestampColumnDefBuilder().setColumnName(CREATED_AT_COL_NAME).setIsNullable(true).build();
  private static final TimestampColumnDef DEPRECATED_TECHNICAL_UPDATED_AT_COL = newTimestampColumnDefBuilder().setColumnName(UPDATED_AT_COL_NAME).setIsNullable(true).build();

  public CreateInitialSchema(Database db) {
    super(db);
  }

  @Override
  public void execute(Context context) throws SQLException {
    createActiveRuleParameters(context);
    createActiveRules(context);
    createAlmPats(context);
    createAlmSettings(context);
    createProjectAlmSettings(context);
    createAnalysisProperties(context);
    createAppBranchProjectBranch(context);
    createAppProjects(context);
    createAudits(context);
    createCeActivity(context);
    createCeQueue(context);
    createCeScannerContext(context);
    createCeTaskCharacteristics(context);
    createCeTaskInput(context);
    createCeTaskMessage(context);
    createComponents(context);
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
    createMetrics(context);
    createNewCodePeriods(context);
    createNewCodeReferenceIssues(context);
    createNotifications(context);
    createOrgQProfiles(context);
    createOrgQualityGates(context);
    createOrganizationMembers(context);
    createOrganizations(context);
    createPermTemplatesGroups(context);
    createPermTemplatesUsers(context);
    createPermTemplatesCharacteristics(context);
    createPermissionTemplates(context);
    createPlugins(context);
    createPortfolioProjBranches(context);
    createPortfolioProjects(context);
    createPortfolioReferences(context);
    createPortfolios(context);
    createProjectBadgeToken(context);
    createProjectBranches(context);
    createProjectLinks(context);
    createProjectMappings(context);
    createProjectMeasures(context);
    createProjectQprofiles(context);
    createProjects(context);
    createProjectQGates(context);
    createProperties(context);
    createPushEvents(context);
    createQGateGroupPermissions(context);
    createQGateUserPermissions(context);
    createQProfileChanges(context);
    createQProfileEditGroups(context);
    createQProfileEditUsers(context);
    createQualityGateConditions(context);
    createQualityGates(context);
    createScimUsers(context);
    createSessionTokens(context);
    createRulesRepository(context);
    createRuleDescSections(context);
    createRules(context);
    createRulesMetadata(context);
    createRulesParameters(context);
    createRulesProfiles(context);
    createSamlMessageIds(context);
    createScannerAnalysisCache(context);
    createSnapshots(context);
    createUserRoles(context);
    createUserDismissedMessage(context);
    createUserTokens(context);
    createUsers(context);
    createWebhookDeliveries(context);
    createWebhooks(context);
  }

  private void createActiveRuleParameters(Context context) {
    String tableName = "active_rule_parameters";
    VarcharColumnDef activeRuleUuidColumnDef = newVarcharColumnDefBuilder("active_rule_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef rulesParameterUuidColumnDef = newVarcharColumnDefBuilder("rules_parameter_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder(VALUE_COL_NAME).setLimit(MAX_SIZE).build())
        .addColumn(newVarcharColumnDefBuilder("rules_parameter_key").setLimit(128).build())
        .addColumn(activeRuleUuidColumnDef)
        .addColumn(rulesParameterUuidColumnDef)
        .build());
    addIndex(context, tableName, "arp_active_rule_uuid", false, activeRuleUuidColumnDef);
  }

  private void createActiveRules(Context context) {
    VarcharColumnDef profileUuidCol = newVarcharColumnDefBuilder("profile_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef ruleUuidCol = newVarcharColumnDefBuilder(RULE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder("active_rules")
        .addPkColumn(UUID_COL)
        .addColumn(newIntegerColumnDefBuilder().setColumnName("failure_level").setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("inheritance").setLimit(10).build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .addColumn(NULLABLE_TECHNICAL_UPDATED_AT_COL)
        .addColumn(profileUuidCol)
        .addColumn(ruleUuidCol)
        .build());
    addIndex(context, "active_rules", "uniq_profile_rule_uuids", true, profileUuidCol, ruleUuidCol);
  }

  private void createAlmPats(Context context) {
    String tableName = "alm_pats";
    VarcharColumnDef patCol = newVarcharColumnDefBuilder("pat").setIsNullable(false).setLimit(2000).build();
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setIsNullable(false).setLimit(256).build();
    VarcharColumnDef almSettingUuidCol = newVarcharColumnDefBuilder("alm_setting_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();

    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(patCol)
      .addColumn(userUuidCol)
      .addColumn(almSettingUuidCol)
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "uniq_alm_pats", true, userUuidCol, almSettingUuidCol);
  }

  private void createAlmSettings(Context context) {
    String tableName = "alm_settings";
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setIsNullable(false).setLimit(200).build();

    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(newVarcharColumnDefBuilder("alm_id").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(keeCol)
      .addColumn(newVarcharColumnDefBuilder("url").setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder("app_id").setIsNullable(true).setLimit(80).build())
      .addColumn(newVarcharColumnDefBuilder("private_key").setIsNullable(true).setLimit(2500).build())
      .addColumn(newVarcharColumnDefBuilder("pat").setIsNullable(true).setLimit(2000).build())
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(newVarcharColumnDefBuilder("client_id").setIsNullable(true).setLimit(80).build())
      .addColumn(newVarcharColumnDefBuilder("client_secret").setIsNullable(true).setLimit(160).build())
      .addColumn(newVarcharColumnDefBuilder("webhook_secret").setIsNullable(true).setLimit(160).build())
      .build());
    addIndex(context, tableName, "uniq_alm_settings", true, keeCol);
  }

  private void createProjectAlmSettings(Context context) {
    String tableName = "project_alm_settings";
    VarcharColumnDef almSettingUuidCol = newVarcharColumnDefBuilder("alm_setting_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef almRepoCol = newVarcharColumnDefBuilder("alm_repo").setIsNullable(true).setLimit(256).build();
    VarcharColumnDef almSlugCol = newVarcharColumnDefBuilder("alm_slug").setIsNullable(true).setLimit(256).build();
    BooleanColumnDef summaryCommentEnabledCol = newBooleanColumnDefBuilder("summary_comment_enabled").setIsNullable(true).build();
    BooleanColumnDef monorepoCol = newBooleanColumnDefBuilder("monorepo").setIsNullable(false).build();

    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(almSettingUuidCol)
      .addColumn(projectUuidCol)
      .addColumn(almRepoCol)
      .addColumn(almSlugCol)
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(summaryCommentEnabledCol)
      .addColumn(monorepoCol)
      .build());
    addIndex(context, tableName, "uniq_project_alm_settings", true, projectUuidCol);
    addIndex(context, tableName, "project_alm_settings_alm", false, almSettingUuidCol);
    addIndex(context, tableName, "project_alm_settings_slug", false, almSlugCol);
  }

  private void createAnalysisProperties(Context context) {
    String tableName = "analysis_properties";
    VarcharColumnDef snapshotUuidColumn = newVarcharColumnDefBuilder(ANALYSIS_UUID_COL_NAME)
      .setIsNullable(false)
      .setLimit(UUID_SIZE)
      .build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(snapshotUuidColumn)
        .addColumn(newVarcharColumnDefBuilder("kee").setIsNullable(false).setLimit(512).build())
        .addColumn(newVarcharColumnDefBuilder(TEXT_VALUE_COL_NAME).setIsNullable(true).setLimit(MAX_SIZE).build())
        .addColumn(newClobColumnDefBuilder().setColumnName(CLOB_VALUE_COL_NAME).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName(IS_EMPTY_COL_NAME).setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());
    addIndex(context, tableName, "analysis_properties_analysis", false, snapshotUuidColumn);
  }

  private void createAppBranchProjectBranch(Context context) {
    String tableName = "app_branch_project_branch";
    VarcharColumnDef applicationBranchUuid = newVarcharColumnDefBuilder("application_branch_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef projectBranchUuid = newVarcharColumnDefBuilder("project_branch_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef applicationUuid = newVarcharColumnDefBuilder("application_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef projectUuid = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(applicationUuid)
        .addColumn(applicationBranchUuid)
        .addColumn(projectUuid)
        .addColumn(projectBranchUuid)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());
    addIndex(context, tableName, "uniq_app_branch_proj", true, applicationBranchUuid, projectBranchUuid);
    addIndex(context, tableName, "idx_abpb_app_uuid", false, applicationUuid);
    addIndex(context, tableName, "idx_abpb_app_branch_uuid", false, applicationBranchUuid);
    addIndex(context, tableName, "idx_abpb_proj_uuid", false, projectUuid);
    addIndex(context, tableName, "idx_abpb_proj_branch_uuid", false, projectBranchUuid);
  }

  private void createAppProjects(Context context) {
    String tableName = "app_projects";
    VarcharColumnDef applicationUuid = newVarcharColumnDefBuilder("application_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef projectUuid = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(applicationUuid)
        .addColumn(projectUuid)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());

    addIndex(context, tableName, "uniq_app_projects", true, applicationUuid, projectUuid);
    addIndex(context, tableName, "idx_app_proj_application_uuid", false, applicationUuid);
    addIndex(context, tableName, "idx_app_proj_project_uuid", false, projectUuid);
  }

  private void createAudits(Context context) {
    String tableName = "audits";

    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setIsNullable(false).setLimit(USER_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder("user_login").setIsNullable(false).setLimit(USER_UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder("category").setIsNullable(false).setLimit(25).build())
      .addColumn(newVarcharColumnDefBuilder("operation").setIsNullable(false).setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder("new_value").setIsNullable(true).setLimit(4000).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(newBooleanColumnDefBuilder("user_triggered").setIsNullable(false).setDefaultValue(true).build())
      .build());
    addIndex(context, tableName, "audits_created_at", false, TECHNICAL_CREATED_AT_COL);
  }

  private void createCeActivity(Context context) {
    String tableName = "ce_activity";
    VarcharColumnDef uuidCol = UUID_COL;
    VarcharColumnDef mainComponentUuidCol = newVarcharColumnDefBuilder("main_component_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef statusCol = newVarcharColumnDefBuilder(STATUS_COL_NAME).setLimit(15).setIsNullable(false).build();
    BooleanColumnDef mainIsLastCol = newBooleanColumnDefBuilder().setColumnName("main_is_last").setIsNullable(false).build();
    VarcharColumnDef mainIsLastKeyCol = newVarcharColumnDefBuilder("main_is_last_key").setLimit(55).setIsNullable(false).build();
    BooleanColumnDef isLastCol = newBooleanColumnDefBuilder().setColumnName("is_last").setIsNullable(false).build();
    VarcharColumnDef isLastKeyCol = newVarcharColumnDefBuilder("is_last_key").setLimit(55).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(uuidCol)
        .addColumn(newVarcharColumnDefBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(mainComponentUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(statusCol)
        .addColumn(mainIsLastCol)
        .addColumn(mainIsLastKeyCol)
        .addColumn(isLastCol)
        .addColumn(isLastKeyCol)
        .addColumn(newVarcharColumnDefBuilder("submitter_uuid").setLimit(USER_UUID_SIZE).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("submitted_at").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("executed_at").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("execution_count").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("execution_time_ms").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder(ANALYSIS_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("error_message").setLimit(1_000).setIsNullable(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName("error_stacktrace").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("error_type").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("worker_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder("node_name").setLimit(100).setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "ce_activity_component", false, componentUuidCol);
    addIndex(context, tableName, "ce_activity_islast", false, isLastCol, statusCol);
    addIndex(context, tableName, "ce_activity_islast_key", false, isLastKeyCol);
    addIndex(context, tableName, "ce_activity_main_component", false, mainComponentUuidCol);
    addIndex(context, tableName, "ce_activity_main_islast", false, mainIsLastCol, statusCol);
    addIndex(context, tableName, "ce_activity_main_islast_key", false, mainIsLastKeyCol);
  }

  private void createCeQueue(Context context) {
    String tableName = "ce_queue";
    VarcharColumnDef uuidCol = UUID_COL;
    VarcharColumnDef mainComponentUuidCol = newVarcharColumnDefBuilder("main_component_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(uuidCol)
        .addColumn(newVarcharColumnDefBuilder("task_type").setLimit(15).setIsNullable(false).build())
        .addColumn(mainComponentUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(newVarcharColumnDefBuilder(STATUS_COL_NAME).setLimit(15).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("submitter_uuid").setLimit(USER_UUID_SIZE).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("started_at").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("worker_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("execution_count").setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .build());
    addIndex(context, tableName, "ce_queue_main_component", false, mainComponentUuidCol);
    addIndex(context, tableName, "ce_queue_component", false, componentUuidCol);
  }

  private void createCeScannerContext(Context context) {
    context.execute(
      newTableBuilder("ce_scanner_context")
        .addPkColumn(newVarcharColumnDefBuilder(TASK_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("context_data").setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .build());
  }

  private void createCeTaskCharacteristics(Context context) {
    String tableName = "ce_task_characteristics";
    VarcharColumnDef ceTaskUuidColumn = newVarcharColumnDefBuilder(TASK_UUID_COL_NAME)
      .setLimit(UUID_SIZE)
      .setIsNullable(false)
      .build();

    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(ceTaskUuidColumn)
        .addColumn(newVarcharColumnDefBuilder("kee").setLimit(512).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder(TEXT_VALUE_COL_NAME).setLimit(512).setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "ce_characteristics_" + ceTaskUuidColumn.getName(), false, ceTaskUuidColumn);
  }

  private void createCeTaskInput(Context context) {
    context.execute(
      newTableBuilder("ce_task_input")
        .addPkColumn(newVarcharColumnDefBuilder(TASK_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("input_data").setIsNullable(true).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .build());
  }

  private void createCeTaskMessage(Context context) {
    String tableName = "ce_task_message";
    VarcharColumnDef taskUuidCol = newVarcharColumnDefBuilder(TASK_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef messageTypeCol = newVarcharColumnDefBuilder("message_type").setIsNullable(false).setLimit(255).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(taskUuidCol)
      .addColumn(newVarcharColumnDefBuilder("message").setIsNullable(false).setLimit(MAX_SIZE).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(messageTypeCol)
      .build());
    addIndex(context, tableName, tableName + "_task", false, taskUuidCol);
    addIndex(context, tableName, "ctm_message_type", false, messageTypeCol);
  }

  private void createComponents(Context context) {
    String tableName = "components";
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setIsNullable(true).setLimit(1000).build();
    VarcharColumnDef moduleUuidCol = newVarcharColumnDefBuilder("module_uuid").setIsNullable(true).setLimit(50).build();
    VarcharColumnDef branchUuidCol = newVarcharColumnDefBuilder(BRANCH_UUID_COL_NAME).setIsNullable(false).setLimit(50).build();
    VarcharColumnDef qualifierCol = newVarcharColumnDefBuilder("qualifier").setIsNullable(true).setLimit(10).build();
    VarcharColumnDef rootUuidCol = newVarcharColumnDefBuilder("root_uuid").setIsNullable(false).setLimit(50).build();
    VarcharColumnDef uuidCol = newVarcharColumnDefBuilder("uuid").setIsNullable(false).setLimit(50).build();
    VarcharColumnDef mainBranchProjectUuidCol = newVarcharColumnDefBuilder("main_branch_project_uuid").setIsNullable(true).setLimit(50).build();

    context.execute(newTableBuilder(tableName)
      .addColumn(uuidCol)
      .addColumn(organizationUuidCol)
      .addColumn(keeCol)
      .addColumn(newVarcharColumnDefBuilder("deprecated_kee").setIsNullable(true).setLimit(400).build())
      .addColumn(newVarcharColumnDefBuilder("name").setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder("long_name").setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setIsNullable(true).setLimit(2000).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("enabled").setIsNullable(false).setDefaultValue(true).build())
      .addColumn(newVarcharColumnDefBuilder("scope").setIsNullable(true).setLimit(3).build())
      .addColumn(qualifierCol)
      .addColumn(newBooleanColumnDefBuilder().setColumnName(PRIVATE_COL_NAME).setIsNullable(false).build())
      .addColumn(rootUuidCol)
      .addColumn(newVarcharColumnDefBuilder(LANGUAGE_COL_NAME).setIsNullable(true).setLimit(20).build())
      .addColumn(newVarcharColumnDefBuilder("copy_component_uuid").setIsNullable(true).setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder("path").setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder("uuid_path").setIsNullable(false).setLimit(1500).build())
      .addColumn(branchUuidCol)
      .addColumn(moduleUuidCol)
      .addColumn(newVarcharColumnDefBuilder("module_uuid_path").setIsNullable(true).setLimit(1500).build())
      .addColumn(mainBranchProjectUuidCol)
      .addColumn(newBooleanColumnDefBuilder().setColumnName("b_changed").setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("b_name").setIsNullable(true).setLimit(500).build())
      .addColumn(newVarcharColumnDefBuilder("b_long_name").setIsNullable(true).setLimit(500).build())
      .addColumn(newVarcharColumnDefBuilder("b_description").setIsNullable(true).setLimit(2000).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("b_enabled").setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("b_qualifier").setIsNullable(true).setLimit(10).build())
      .addColumn(newVarcharColumnDefBuilder("b_language").setIsNullable(true).setLimit(20).build())
      .addColumn(newVarcharColumnDefBuilder("b_copy_component_uuid").setIsNullable(true).setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder("b_path").setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder("b_uuid_path").setIsNullable(true).setLimit(1500).build())
      .addColumn(newVarcharColumnDefBuilder("b_module_uuid").setIsNullable(true).setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder("b_module_uuid_path").setIsNullable(true).setLimit(1500).build())
      .addColumn(newTimestampColumnDefBuilder().setColumnName(CREATED_AT_COL_NAME).setIsNullable(true).build())
      .build());

    addIndex(context, tableName, "components_organization", false, organizationUuidCol);
    addIndex(context, tableName, "projects_module_uuid", false, moduleUuidCol);
    addIndex(context, tableName, "projects_qualifier", false, qualifierCol);
    addIndex(context, tableName, "projects_root_uuid", false, rootUuidCol);
    addIndex(context, tableName, "idx_main_branch_prj_uuid", false, mainBranchProjectUuidCol);
    addIndex(context, tableName, "components_uuid", true, uuidCol);
    addIndex(context, tableName, "components_branch_uuid", false, branchUuidCol);
    addIndex(context, tableName, "components_kee_branch_uuid", true, keeCol, branchUuidCol);
  }

  private void createDefaultQProfiles(Context context) {
    String tableName = "default_qprofiles";
    VarcharColumnDef profileUuidColumn = newVarcharColumnDefBuilder(QPROFILE_UUID_COL_NAME)
      .setLimit(255)
      .setIsNullable(false)
      .build();

    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addPkColumn(newVarcharColumnDefBuilder(LANGUAGE_COL_NAME).setLimit(20).setIsNullable(false).build())
        .addColumn(profileUuidColumn)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .build());
    addIndex(context, tableName, "uniq_default_qprofiles_uuid", true, profileUuidColumn);
  }

  private void createDeprecatedRuleKeys(Context context) {
    String tableName = "deprecated_rule_keys";
    VarcharColumnDef ruleUuidCol = newVarcharColumnDefBuilder(RULE_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef oldRepositoryKeyCol = newVarcharColumnDefBuilder("old_repository_key").setIsNullable(false).setLimit(255).build();
    VarcharColumnDef oldRuleKeyCol = newVarcharColumnDefBuilder("old_rule_key").setIsNullable(false).setLimit(200).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(oldRepositoryKeyCol)
      .addColumn(oldRuleKeyCol)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(ruleUuidCol)
      .build());
    addIndex(context, tableName, "uniq_deprecated_rule_keys", true, oldRepositoryKeyCol, oldRuleKeyCol);
    addIndex(context, tableName, "rule_uuid_deprecated_rule_keys", false, ruleUuidCol);
  }

  private void createDuplicationsIndex(Context context) {
    String tableName = "duplications_index";
    VarcharColumnDef hashCol = newVarcharColumnDefBuilder("hash").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef analysisUuidCol = newVarcharColumnDefBuilder(ANALYSIS_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
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
    BigIntegerColumnDef createdAtCol = TECHNICAL_CREATED_AT_COL;
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("doc_type").setIsNullable(false).setLimit(40).build())
        .addColumn(newVarcharColumnDefBuilder("doc_id").setIsNullable(false).setLimit(MAX_SIZE).build())
        .addColumn(newVarcharColumnDefBuilder("doc_id_type").setIsNullable(true).setLimit(20).build())
        .addColumn(newVarcharColumnDefBuilder("doc_routing").setIsNullable(true).setLimit(MAX_SIZE).build())
        .addColumn(createdAtCol)
        .build());
    addIndex(context, tableName, "es_queue_created_at", false, createdAtCol);
  }

  private void createEventComponentChanges(Context context) {
    String tableName = "event_component_changes";
    VarcharColumnDef eventUuidCol = newVarcharColumnDefBuilder("event_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef eventComponentUuidCol = newVarcharColumnDefBuilder("event_component_uuid").setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef eventAnalysisUuidCol = newVarcharColumnDefBuilder("event_analysis_uuid").setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef changeCategoryCol = newVarcharColumnDefBuilder("change_category").setIsNullable(false).setLimit(12).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(eventUuidCol)
      .addColumn(eventComponentUuidCol)
      .addColumn(eventAnalysisUuidCol)
      .addColumn(changeCategoryCol)
      .addColumn(componentUuidCol)
      .addColumn(newVarcharColumnDefBuilder("component_key").setIsNullable(false).setLimit(400).build())
      .addColumn(newVarcharColumnDefBuilder("component_name").setIsNullable(false).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder("component_branch_key").setIsNullable(true).setLimit(255).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, tableName + UNIQUE_INDEX_SUFFIX, true, eventUuidCol, changeCategoryCol, componentUuidCol);
    addIndex(context, tableName, "event_cpnt_changes_cpnt", false, eventComponentUuidCol);
    addIndex(context, tableName, "event_cpnt_changes_analysis", false, eventAnalysisUuidCol);
  }

  private void createEvents(Context context) {
    String tableName = "events";
    VarcharColumnDef uuidCol = UUID_COL;
    VarcharColumnDef analysisUuidCol = newVarcharColumnDefBuilder(ANALYSIS_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuid = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(uuidCol)
        .addColumn(analysisUuidCol)
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(400).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("category").setLimit(50).build())
        .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(MAX_SIZE).build())
        .addColumn(newVarcharColumnDefBuilder("event_data").setLimit(MAX_SIZE).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("event_date").setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(componentUuid)
        .build());
    addIndex(context, tableName, "events_analysis", false, analysisUuidCol);
    addIndex(context, tableName, "events_component_uuid", false, componentUuid);
  }

  private void createFileSources(Context context) {
    String tableName = "file_sources";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    BigIntegerColumnDef updatedAtCol = TECHNICAL_UPDATED_AT_COL;
    VarcharColumnDef fileUuidCol = newVarcharColumnDefBuilder("file_uuid").setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(projectUuidCol)
        .addColumn(fileUuidCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("line_hashes").setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line_hashes_version").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("data_hash").setLimit(50).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("src_hash").setLimit(50).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("revision").setLimit(100).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line_count").setIsNullable(false).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("binary_data").setIsNullable(true).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(updatedAtCol)
        .build());
    addIndex(context, tableName, "file_sources_file_uuid", true, fileUuidCol);
    addIndex(context, tableName, "file_sources_project_uuid", false, projectUuidCol);
    addIndex(context, tableName, "file_sources_updated_at", false, updatedAtCol);
  }

  private void createGroupRoles(Context context) {
    String tableName = "group_roles";
    VarcharColumnDef roleCol = newVarcharColumnDefBuilder("role").setLimit(64).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setIsNullable(true).setLimit(UUID_SIZE).build();
    VarcharColumnDef groupUuidCol = newVarcharColumnDefBuilder(GROUP_UUID_COL_NAME).setIsNullable(true).setLimit(UUID_SIZE).build();
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(organizationUuidCol)
        .addColumn(roleCol)
        .addColumn(componentUuidCol)
        .addColumn(groupUuidCol)
        .build());
    addIndex(context, tableName, "group_roles_component_uuid", false, componentUuidCol);
    addIndex(context, tableName, "uniq_group_roles", true, organizationUuidCol, groupUuidCol, componentUuidCol, roleCol);
  }

  private void createGroups(Context context) {
    String tableName = "groups";
    VarcharColumnDef nameCol = newVarcharColumnDefBuilder("name").setLimit(500).setIsNullable(false).build();
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(organizationUuidCol)
        .addColumn(nameCol)
        .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(200).setIsNullable(true).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .build());
    addIndex(context, tableName, "uniq_groups_name", true, organizationUuidCol, nameCol);
  }

  private void createGroupsUsers(Context context) {
    String tableName = "groups_users";
    VarcharColumnDef groupUuidCol = newVarcharColumnDefBuilder(GROUP_UUID_COL_NAME).setLimit(40).setIsNullable(false).build();
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addColumn(groupUuidCol)
        .addColumn(userUuidCol)
        .build());
    addIndex(context, tableName, "index_groups_users_group_uuid", false, groupUuidCol);
    addIndex(context, tableName, "index_groups_users_user_uuid", false, userUuidCol);
    addIndex(context, tableName, "groups_users_unique", true, userUuidCol, groupUuidCol);
  }

  private void createInternalComponentProps(Context context) {
    String tableName = "internal_component_props";
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setIsNullable(false).setLimit(512).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(componentUuidCol)
      .addColumn(keeCol)
      .addColumn(newVarcharColumnDefBuilder(VALUE_COL_NAME).setIsNullable(true).setLimit(MAX_SIZE).build())
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "unique_component_uuid_kee", true, componentUuidCol, keeCol);
  }

  private void createInternalProperties(Context context) {
    context.execute(
      newTableBuilder("internal_properties")
        .addPkColumn(newVarcharColumnDefBuilder("kee").setLimit(20).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName(IS_EMPTY_COL_NAME).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder().setColumnName(TEXT_VALUE_COL_NAME).setLimit(MAX_SIZE).setIgnoreOracleUnit(true).build())
        .addColumn(newClobColumnDefBuilder().setColumnName(CLOB_VALUE_COL_NAME).setIsNullable(true).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());
  }

  private void createIssueChanges(Context context) {
    String tableName = "issue_changes";
    VarcharColumnDef issueKeyCol = newVarcharColumnDefBuilder("issue_key").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setLimit(50).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(50).setIsNullable(false).build();
    VarcharColumnDef changeTypeCol = newVarcharColumnDefBuilder("change_type").setLimit(20).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(keeCol)
        .addColumn(issueKeyCol)
        .addColumn(newVarcharColumnDefBuilder("user_login").setLimit(USER_UUID_SIZE).build())
        .addColumn(changeTypeCol)
        .addColumn(newClobColumnDefBuilder().setColumnName("change_data").build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .addColumn(NULLABLE_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_change_creation_date").build())
        .addColumn(projectUuidCol)
        .build());
    addIndex(context, tableName, "issue_changes_issue_key", false, issueKeyCol);
    addIndex(context, tableName, "issue_changes_kee", false, keeCol);
    addIndex(context, tableName, "issue_changes_project_uuid", false, projectUuidCol);
    addIndex(context, tableName, "issue_changes_issue_key_type", false, issueKeyCol, changeTypeCol);
  }

  private void createIssues(Context context) {
    var tableName = "issues";
    VarcharColumnDef assigneeCol = newVarcharColumnDefBuilder("assignee").setLimit(USER_UUID_SIZE).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(50).build();
    BigIntegerColumnDef issueCreationDateCol = newBigIntegerColumnDefBuilder().setColumnName("issue_creation_date").build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setLimit(50).setIsNullable(false).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(50).build();
    VarcharColumnDef resolutionCol = newVarcharColumnDefBuilder("resolution").setLimit(20).build();
    VarcharColumnDef ruleUuidCol = newVarcharColumnDefBuilder(RULE_UUID_COL_NAME).setLimit(40).setIsNullable(true).build();
    BigIntegerColumnDef updatedAtCol = NULLABLE_TECHNICAL_UPDATED_AT_COL;
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(keeCol)
        .addColumn(ruleUuidCol)
        .addColumn(newVarcharColumnDefBuilder("severity").setLimit(10).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("manual_severity").setIsNullable(false).build())
        // unit has been fixed in SonarQube 5.6 (see migration 1151, SONAR-7493)
        .addColumn(newVarcharColumnDefBuilder("message").setLimit(MAX_SIZE).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("line").build())
        .addColumn(newDecimalColumnDefBuilder().setColumnName("gap").setPrecision(30).setScale(20).build())
        .addColumn(newVarcharColumnDefBuilder(STATUS_COL_NAME).setLimit(20).build())
        .addColumn(resolutionCol)
        .addColumn(newVarcharColumnDefBuilder("checksum").setLimit(1000).build())
        .addColumn(assigneeCol)
        .addColumn(newVarcharColumnDefBuilder("author_login").setLimit(255).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("effort").build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .addColumn(updatedAtCol)
        .addColumn(issueCreationDateCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_update_date").build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("issue_close_date").build())
        .addColumn(newVarcharColumnDefBuilder("tags").setLimit(MAX_SIZE).build())
        .addColumn(componentUuidCol)
        .addColumn(projectUuidCol)
        .addColumn(newBlobColumnDefBuilder().setColumnName("locations").build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("issue_type").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("from_hotspot").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("quick_fix_available").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("rule_description_context_key").setLimit(50).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("message_formattings").build())
        .build());

    addIndex(context, tableName, "issues_assignee", false, assigneeCol);
    addIndex(context, tableName, "issues_component_uuid", false, componentUuidCol);
    addIndex(context, tableName, "issues_creation_date", false, issueCreationDateCol);
    addIndex(context, tableName, "issues_project_uuid", false, projectUuidCol);
    addIndex(context, tableName, "issues_resolution", false, resolutionCol);
    addIndex(context, tableName, "issues_updated_at", false, updatedAtCol);
    addIndex(context, tableName, "issues_rule_uuid", false, ruleUuidCol);
  }

  private void createLiveMeasures(Context context) {
    String tableName = "live_measures";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef metricUuidCol = newVarcharColumnDefBuilder(METRIC_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(projectUuidCol)
      .addColumn(componentUuidCol)
      .addColumn(metricUuidCol)
      .addColumn(newDecimalColumnDefBuilder().setColumnName(VALUE_COL_NAME).setPrecision(38).setScale(20).build())
      .addColumn(newVarcharColumnDefBuilder(TEXT_VALUE_COL_NAME).setIsNullable(true).setLimit(MAX_SIZE).build())
      .addColumn(newBlobColumnDefBuilder().setColumnName("measure_data").setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("update_marker").setIsNullable(true).setLimit(UUID_SIZE).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .build());
    addIndex(context, tableName, "live_measures_project", false, projectUuidCol);
    addIndex(context, tableName, "live_measures_component", true, componentUuidCol, metricUuidCol);
  }

  private void createMetrics(Context context) {
    String tableName = "metrics";
    VarcharColumnDef nameCol = newVarcharColumnDefBuilder("name").setLimit(64).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(nameCol)
        .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(255).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("direction").setIsNullable(false).setDefaultValue(0).build())
        .addColumn(newVarcharColumnDefBuilder("domain").setLimit(64).build())
        .addColumn(newVarcharColumnDefBuilder("short_name").setLimit(64).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("qualitative").setDefaultValue(false).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("val_type").setLimit(8).build())
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

  private void createNewCodePeriods(Context context) {
    String tableName = "new_code_periods";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef branchUuidCol = newVarcharColumnDefBuilder(BRANCH_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef typeCol = newVarcharColumnDefBuilder("type").setLimit(30).setIsNullable(false).build();
    VarcharColumnDef valueCol = newVarcharColumnDefBuilder(VALUE_COL_NAME).setLimit(255).setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(projectUuidCol)
        .addColumn(branchUuidCol)
        .addColumn(typeCol)
        .addColumn(valueCol)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());

    addIndex(context, tableName, "uniq_new_code_periods", true, projectUuidCol, branchUuidCol);
    addIndex(context, tableName, "idx_ncp_type", false, typeCol);
    addIndex(context, tableName, "idx_ncp_value", false, valueCol);
  }

  private void createNewCodeReferenceIssues(Context context) {
    String tableName = "new_code_reference_issues";
    VarcharColumnDef issueKeyCol = newVarcharColumnDefBuilder("issue_key").setLimit(50).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(issueKeyCol)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());

    addIndex(context, tableName, "uniq_new_code_reference_issues", true, issueKeyCol);
  }

  private void createNotifications(Context context) {
    context.execute(
      newTableBuilder("notifications")
        .addPkColumn(UUID_COL)
        .addColumn(newBlobColumnDefBuilder().setColumnName("data").build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());
  }

  private void createOrgQProfiles(Context context) {
    String tableName = "org_qprofiles";
    int profileUuidSize = 255;
    VarcharColumnDef organizationCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef rulesProfileUuidCol = newVarcharColumnDefBuilder("rules_profile_uuid").setLimit(profileUuidSize).setIsNullable(false).build();
    VarcharColumnDef parentUuidCol = newVarcharColumnDefBuilder("parent_uuid").setLimit(profileUuidSize).setIsNullable(true).build();
    context.execute(
            newTableBuilder(tableName)
                    .addPkColumn(newVarcharColumnDefBuilder("uuid").setLimit(profileUuidSize).setIsNullable(false).build())
                    .addColumn(organizationCol)
                    .addColumn(rulesProfileUuidCol)
                    .addColumn(parentUuidCol)
                    .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_used").setIsNullable(true).build())
                    .addColumn(newBigIntegerColumnDefBuilder().setColumnName("user_updated_at").setIsNullable(true).build())
                    .addColumn(TECHNICAL_CREATED_AT_COL)
                    .addColumn(TECHNICAL_UPDATED_AT_COL)
                    .build());
    addIndex(context, tableName, "qprofiles_org_uuid", false, organizationCol);
    addIndex(context, tableName, "qprofiles_rp_uuid", false, rulesProfileUuidCol);
    addIndex(context, tableName, "org_qprofiles_parent_uuid", false, parentUuidCol);
  }

  private void createOrgQualityGates(Context context) {
    String tableName = "org_quality_gates";
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef qualityGateUuidCol = newVarcharColumnDefBuilder("quality_gate_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(newTableBuilder(tableName)
            .addPkColumn(newVarcharColumnDefBuilder("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
            .addColumn(organizationUuidCol)
            .addColumn(qualityGateUuidCol)
            .build());
    addIndex(context, tableName, "uniq_org_quality_gates", true, organizationUuidCol, qualityGateUuidCol);
  }

  private void createOrganizationMembers(Context context) {
    String tableName = "organization_members";
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(255).setIsNullable(false).build();
    context.execute(
            newTableBuilder(tableName)
                    .addPkColumn(newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
                    .addPkColumn(userUuidCol)
                    .build());
    addIndex(context, tableName, "idx_org_members_on_user_uuid", false, userUuidCol);
  }

  private void createOrganizations(Context context) {
    String tableName = "organizations";
    VarcharColumnDef keeColumn = newVarcharColumnDefBuilder("kee").setLimit(255).setIsNullable(false).build();
    context.execute(
            newTableBuilder(tableName)
                    .addPkColumn(newVarcharColumnDefBuilder("uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
                    .addColumn(keeColumn)
                    .addColumn(newVarcharColumnDefBuilder("name").setLimit(255).setIsNullable(false).build())
                    .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(256).setIsNullable(true).build())
                    .addColumn(newVarcharColumnDefBuilder("url").setLimit(256).setIsNullable(true).build())
                    .addColumn(newVarcharColumnDefBuilder("avatar_url").setLimit(256).setIsNullable(true).build())
                    .addColumn(newVarcharColumnDefBuilder().setColumnName("default_group_uuid").setLimit(UUID_SIZE).setIsNullable(true).build())
                    .addColumn(newVarcharColumnDefBuilder("default_quality_gate_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
                    .addColumn(newVarcharColumnDefBuilder("default_perm_template_project").setLimit(UUID_SIZE).setIsNullable(true).build())
                    .addColumn(newVarcharColumnDefBuilder("default_perm_template_app").setLimit(UUID_SIZE).setIsNullable(true).build())
                    .addColumn(newVarcharColumnDefBuilder("default_perm_template_port").setLimit(UUID_SIZE).setIsNullable(true).build())
                    .addColumn(newBooleanColumnDefBuilder().setColumnName("new_project_private").setIsNullable(false).build())
                    .addColumn(newVarcharColumnDefBuilder("subscription").setLimit(UUID_SIZE).setIsNullable(false).build())
                    .addColumn(TECHNICAL_CREATED_AT_COL)
                    .addColumn(TECHNICAL_UPDATED_AT_COL)
                    .build());
    addIndex(context, tableName, "organization_key", true, keeColumn);
  }

  private void createPermTemplatesGroups(Context context) {
    context.execute(
      newTableBuilder("perm_templates_groups")
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("permission_reference").setLimit(64).setIsNullable(false).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder(TEMPLATE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder(GROUP_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build())
        .build());
  }

  private void createPermTemplatesUsers(Context context) {
    context.execute(
      newTableBuilder("perm_templates_users")
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("permission_reference").setLimit(64).setIsNullable(false).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder(TEMPLATE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(USER_UUID_SIZE).setIsNullable(false).build())
        .build());
  }

  private void createPermTemplatesCharacteristics(Context context) {
    String tableName = "perm_tpl_characteristics";
    VarcharColumnDef permissionKeyColumn = newVarcharColumnDefBuilder("permission_key").setLimit(64).setIsNullable(false).build();
    VarcharColumnDef templateUuidColumn = newVarcharColumnDefBuilder(TEMPLATE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(permissionKeyColumn)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("with_project_creator").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .addColumn(templateUuidColumn)
        .build());

    addIndex(context, tableName, "uniq_perm_tpl_charac", true, templateUuidColumn, permissionKeyColumn);
  }

  private void createPermissionTemplates(Context context) {
    context.execute(
      newTableBuilder("permission_templates")
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(MAX_SIZE).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder("key_pattern").setLimit(500).build())
        .build());
  }

  private void createPlugins(Context context) {
    int pluginKeyMaxSize = 200;
    String tableName = "plugins";
    VarcharColumnDef keyColumn = newVarcharColumnDefBuilder("kee").setLimit(pluginKeyMaxSize).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(keyColumn)
        .addColumn(newVarcharColumnDefBuilder("base_plugin_key").setLimit(pluginKeyMaxSize).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("file_hash").setLimit(200).setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder("type").setLimit(10).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder("removed").setDefaultValue(false).setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "plugins_key", true, keyColumn);
  }

  private void createPortfolioProjBranches(Context context) {
    String tableName = "portfolio_proj_branches";
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(UUID_COL)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("portfolio_project_uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(BRANCH_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
  }

  private void createPortfolioProjects(Context context) {
    String tableName = "portfolio_projects";
    VarcharColumnDef portfolioUuidColumn = newVarcharColumnDefBuilder().setColumnName("portfolio_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef projectUuidColumn = newVarcharColumnDefBuilder().setColumnName(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(UUID_COL)
      .addColumn(portfolioUuidColumn)
      .addColumn(projectUuidColumn)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "uniq_portfolio_projects", true, portfolioUuidColumn, projectUuidColumn);
  }

  private void createPortfolioReferences(Context context) {
    String tableName = "portfolio_references";
    VarcharColumnDef portfolioUuidColumn = newVarcharColumnDefBuilder().setColumnName("portfolio_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef referenceUuidColumn = newVarcharColumnDefBuilder().setColumnName("reference_uuid").setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef branchUuidColumn = newVarcharColumnDefBuilder().setColumnName(BRANCH_UUID_COL_NAME).setIsNullable(true).setLimit(255).build();
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(UUID_COL)
      .addColumn(portfolioUuidColumn)
      .addColumn(referenceUuidColumn)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(branchUuidColumn)
      .build());
    addIndex(context, tableName, "uniq_portfolio_references", true, portfolioUuidColumn, referenceUuidColumn, branchUuidColumn);
  }

  private void createPortfolios(Context context) {
    String tableName = "portfolios";
    VarcharColumnDef keeColumn = newVarcharColumnDefBuilder().setColumnName("kee").setIsNullable(false).setLimit(400).build();
    context.execute(new CreateTableBuilder(getDialect(), tableName)
      .addPkColumn(UUID_COL)
      .addColumn(keeColumn)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("name").setIsNullable(false).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(DESCRIPTION_COL_NAME).setIsNullable(true).setLimit(2000).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("root_uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("parent_uuid").setIsNullable(true).setLimit(UUID_SIZE).build())
      .addColumn(newBooleanColumnDefBuilder(PRIVATE_COL_NAME).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("selection_mode").setIsNullable(false).setLimit(50).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName("selection_expression").setIsNullable(true).setLimit(4000).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("branch_key").setIsNullable(true).setLimit(255).build())
      .build());
    addIndex(context, tableName, "uniq_portfolios_kee", true, keeColumn);
  }

  private void createProjectBadgeToken(Context context) {
    String tableName = "project_badge_token";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("token").setIsNullable(false).setLimit(255).build())
        .addColumn(projectUuidCol)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .build());
    addIndex(context, tableName, "uniq_project_badge_token", true, projectUuidCol);
  }

  private void createProjectBranches(Context context) {
    String tableName = "project_branches";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setIsNullable(false).setLimit(255).build();
    VarcharColumnDef branchTypeCol = newVarcharColumnDefBuilder("branch_type").setIsNullable(false).setLimit(12).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnDefBuilder("uuid").setIsNullable(false).setLimit(OLD_UUID_VARCHAR_SIZE).build())
        .addColumn(projectUuidCol)
        .addColumn(keeCol)
        .addColumn(branchTypeCol)
        .addColumn(newVarcharColumnDefBuilder("merge_branch_uuid").setIsNullable(true).setLimit(OLD_UUID_VARCHAR_SIZE).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("pull_request_binary").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("manual_baseline_analysis_uuid").setIsNullable(true).setLimit(UUID_SIZE).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .addColumn(newBooleanColumnDefBuilder("exclude_from_purge").setDefaultValue(false).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder("need_issue_sync").setIsNullable(false).build())
        .build());
    addIndex(context, tableName, "uniq_project_branches", true, branchTypeCol, projectUuidCol, keeCol);
  }

  private void createProjectLinks(Context context) {
    String tableName = "project_links";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(projectUuidCol)
      .addColumn(newVarcharColumnDefBuilder("link_type").setLimit(20).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder("name").setLimit(128).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("href").setLimit(2048).setIsNullable(false).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .build());
    addIndex(context, tableName, "project_links_project", false, projectUuidCol);
  }

  private void createProjectMappings(Context context) {
    String tableName = "project_mappings";
    VarcharColumnDef keyTypeCol = newVarcharColumnDefBuilder("key_type").setIsNullable(false).setLimit(200).build();
    VarcharColumnDef keyCol = newVarcharColumnDefBuilder("kee").setIsNullable(false).setLimit(MAX_SIZE).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(keyTypeCol)
      .addColumn(keyCol)
      .addColumn(projectUuidCol)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "key_type_kee", true, keyTypeCol, keyCol);
    addIndex(context, tableName, PROJECT_UUID_COL_NAME, false, projectUuidCol);
  }

  private void createProjectMeasures(Context context) {
    String tableName = "project_measures";
    IntegerColumnDef personIdCol = newIntegerColumnDefBuilder().setColumnName("person_id").build();
    VarcharColumnDef metricUuidCol = newVarcharColumnDefBuilder(METRIC_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef analysisUuidCol = newVarcharColumnDefBuilder(ANALYSIS_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(newDecimalColumnDefBuilder().setColumnName(VALUE_COL_NAME).setPrecision(38).setScale(20).build())
        .addColumn(analysisUuidCol)
        .addColumn(componentUuidCol)
        .addColumn(newVarcharColumnDefBuilder(TEXT_VALUE_COL_NAME).setLimit(MAX_SIZE).build())
        .addColumn(newVarcharColumnDefBuilder("alert_status").setLimit(5).build())
        .addColumn(newVarcharColumnDefBuilder("alert_text").setLimit(MAX_SIZE).build())
        .addColumn(personIdCol)
        .addColumn(newBlobColumnDefBuilder().setColumnName("measure_data").build())
        .addColumn(metricUuidCol)
        .build());
    addIndex(context, tableName, "measures_component_uuid", false, componentUuidCol);
    addIndex(context, tableName, "measures_analysis_metric", false, analysisUuidCol, metricUuidCol);
    addIndex(context, tableName, "project_measures_metric", false, metricUuidCol);
  }

  private void createProjectQprofiles(Context context) {
    String tableName = "project_qprofiles";
    VarcharColumnDef projectUuid = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(50).setIsNullable(false).build();
    VarcharColumnDef profileKey = newVarcharColumnDefBuilder("profile_key").setLimit(50).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(projectUuid)
        .addColumn(profileKey)
        .build());
    addIndex(context, tableName, "uniq_project_qprofiles", true, projectUuid, profileKey);
  }

  private void createProjects(Context context) {
    String tableName = "projects";
    VarcharColumnDef uuidCol = UUID_COL;
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder("kee").setLimit(400).setIsNullable(false).build();
    VarcharColumnDef qualifierCol = newVarcharColumnDefBuilder("qualifier").setLimit(10).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(uuidCol)
        .addColumn(organizationUuidCol)
        .addColumn(keeCol)
        .addColumn(qualifierCol)
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(2_000).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(2_000).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName(PRIVATE_COL_NAME).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("tags").setLimit(500).setIsNullable(true).build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .withPkConstraintName("pk_new_projects")
        .build());
    addIndex(context, tableName, "uniq_projects_kee", true, keeCol);
    addIndex(context, tableName, "idx_qualifier", false, qualifierCol);
    addIndex(context, tableName, "projects_organization", false, organizationUuidCol);
  }

  private void createProjectQGates(Context context) {
    String tableName = "project_qgates";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef qualityGateUuidCol = newVarcharColumnDefBuilder(QUALITY_GATE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(projectUuidCol)
        .addColumn(qualityGateUuidCol)
        .build());
    addIndex(context, tableName, "uniq_project_qgates", true, projectUuidCol, qualityGateUuidCol);
  }

  private void createProperties(Context context) {
    String tableName = "properties";
    VarcharColumnDef propKey = newVarcharColumnDefBuilder("prop_key").setLimit(512).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(propKey)
      .addColumn(newBooleanColumnDefBuilder().setColumnName(IS_EMPTY_COL_NAME).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder(TEXT_VALUE_COL_NAME).setLimit(MAX_SIZE).build())
      .addColumn(newClobColumnDefBuilder().setColumnName(CLOB_VALUE_COL_NAME).setIsNullable(true).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(newVarcharColumnDefBuilder().setColumnName(COMPONENT_UUID_COL_NAME).setIsNullable(true).setLimit(UUID_SIZE).build())
      .addColumn(newVarcharColumnDefBuilder().setColumnName(USER_UUID_COL_NAME).setIsNullable(true).setLimit(USER_UUID_SIZE).build())
      // table with be renamed to properties in following migration, use final constraint name right away
      .withPkConstraintName("pk_properties")
      .build());
    addIndex(context, tableName, "properties_key", false, propKey);
  }

  private void createPushEvents(Context context) {
    String tableName = "push_events";
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(newVarcharColumnDefBuilder("name").setLimit(40).setIsNullable(false).build())
      .addColumn(projectUuidCol)
      .addColumn(newBlobColumnDefBuilder().setColumnName("payload").setIsNullable(false).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(newVarcharColumnDefBuilder(LANGUAGE_COL_NAME).setLimit(20).build())
      .build());
    addIndex(context, tableName, "idx_push_even_crea_uuid_proj", false, TECHNICAL_CREATED_AT_COL, UUID_COL, projectUuidCol);
  }

  private void createQGateGroupPermissions(Context context) {
    String tableName = "qgate_group_permissions";
    VarcharColumnDef qualityGateUuidColumn = newVarcharColumnDefBuilder(QUALITY_GATE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(qualityGateUuidColumn)
      .addColumn(newVarcharColumnDefBuilder(GROUP_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "qg_groups_uuid_idx", false, qualityGateUuidColumn);
  }

  private void createQGateUserPermissions(Context context) {
    String tableName = "qgate_user_permissions";
    VarcharColumnDef qualityGateUuidColumn = newVarcharColumnDefBuilder(QUALITY_GATE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(qualityGateUuidColumn)
      .addColumn(newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "quality_gate_uuid_idx", false, qualityGateUuidColumn);
  }

  private void createQProfileChanges(Context context) {
    String tableName = "qprofile_changes";
    VarcharColumnDef rulesProfileUuidCol = newVarcharColumnDefBuilder("rules_profile_uuid").setLimit(255).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnDefBuilder("kee").setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(rulesProfileUuidCol)
      .addColumn(newVarcharColumnDefBuilder("change_type").setLimit(20).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(USER_UUID_SIZE).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("change_data").setIsNullable(true).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, "qp_changes_rules_profile_uuid", false, rulesProfileUuidCol);
  }

  private void createQProfileEditGroups(Context context) {
    String tableName = "qprofile_edit_groups";
    VarcharColumnDef qProfileUuidCol = newVarcharColumnDefBuilder(QPROFILE_UUID_COL_NAME).setIsNullable(false).setLimit(255).build();
    VarcharColumnDef groupUuidCol = newVarcharColumnDefBuilder(GROUP_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(qProfileUuidCol)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(groupUuidCol)
      .build());
    addIndex(context, tableName, tableName + "_qprofile", false, qProfileUuidCol);
    addIndex(context, tableName, tableName + UNIQUE_INDEX_SUFFIX, true, groupUuidCol, qProfileUuidCol);
  }

  private void createQProfileEditUsers(Context context) {
    String tableName = "qprofile_edit_users";
    VarcharColumnDef qProfileUuidCol = newVarcharColumnDefBuilder(QPROFILE_UUID_COL_NAME).setLimit(255).setIsNullable(false).build();
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(255).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(qProfileUuidCol)
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(userUuidCol)
      .build());
    addIndex(context, tableName, tableName + "_qprofile", false, qProfileUuidCol);
    addIndex(context, tableName, tableName + UNIQUE_INDEX_SUFFIX, true, userUuidCol, qProfileUuidCol);
  }

  private void createQualityGateConditions(Context context) {
    context.execute(
      newTableBuilder("quality_gate_conditions")
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("operator").setLimit(3).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("value_error").setLimit(64).setIsNullable(true).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder(METRIC_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("qgate_uuid").setLimit(UUID_SIZE).setIsNullable(false).build())
        .build());
  }

  private void createQualityGates(Context context) {
    context.execute(
      newTableBuilder("quality_gates")
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_built_in").setIsNullable(false).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .build());
  }

  private void createScimUsers(Context context) {
    String tableName = "scim_users";
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnDefBuilder().setColumnName("scim_uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
        .addColumn(userUuidCol)
        .build());

    addIndex(context, tableName, "uniq_scim_users_user_uuid", true, userUuidCol);
  }

  private void createSessionTokens(Context context) {
    String tableName = "session_tokens";
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(userUuidCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName(EXPIRATION_DATE_COL_NAME).setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(TECHNICAL_UPDATED_AT_COL)
        .build());

    addIndex(context, tableName, "session_tokens_user_uuid", false, userUuidCol);
  }

  private void createRulesRepository(Context context) {
    String tableName = "rule_repositories";
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnDefBuilder("kee").setLimit(200).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder(LANGUAGE_COL_NAME).setLimit(20).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder("name").setLimit(4_000).setIsNullable(false).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
  }

  private void createRuleDescSections(Context context) {
    String tableName = "rule_desc_sections";
    VarcharColumnDef ruleUuidCol = newVarcharColumnDefBuilder().setColumnName(RULE_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build();
    VarcharColumnDef keeCol = newVarcharColumnDefBuilder().setColumnName("kee").setIsNullable(false).setLimit(DESCRIPTION_SECTION_KEY_SIZE).build();
    VarcharColumnDef contextKeyCol = newVarcharColumnDefBuilder().setColumnName("context_key").setIsNullable(true).setLimit(DESCRIPTION_SECTION_KEY_SIZE).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnDefBuilder().setColumnName("uuid").setIsNullable(false).setLimit(UUID_SIZE).build())
      .addColumn(ruleUuidCol)
      .addColumn(keeCol)
      .addColumn(newClobColumnDefBuilder().setColumnName("content").setIsNullable(false).build())
      .addColumn(contextKeyCol)
      .addColumn(newVarcharColumnDefBuilder().setColumnName("context_display_name").setIsNullable(true).setLimit(50).build())
      .build());

    addIndex(context, tableName, "uniq_rule_desc_sections", true, ruleUuidCol, keeCol, contextKeyCol);
  }

  private void createRules(Context context) {
    VarcharColumnDef pluginRuleKeyCol = newVarcharColumnDefBuilder("plugin_rule_key").setLimit(200).setIsNullable(false).build();
    VarcharColumnDef pluginNameCol = newVarcharColumnDefBuilder("plugin_name").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    context.execute(
      newTableBuilder("rules")
        .addPkColumn(UUID_COL)
        .addColumn(organizationUuidCol)
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginRuleKeyCol)
        .addColumn(newVarcharColumnDefBuilder("plugin_key").setLimit(200).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("plugin_config_key").setLimit(200).setIsNullable(true).build())
        .addColumn(pluginNameCol)
        .addColumn(newVarcharColumnDefBuilder("scope").setLimit(20).setIsNullable(false).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("priority").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder(STATUS_COL_NAME).setLimit(40).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder(LANGUAGE_COL_NAME).setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("def_remediation_function").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("def_remediation_gap_mult").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("def_remediation_base_effort").setLimit(20).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("gap_description").setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("system_tags").setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_template").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newVarcharColumnDefBuilder("description_format").setLimit(20).setIsNullable(true).build())
        .addColumn(new TinyIntColumnDef.Builder().setColumnName("rule_type").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("security_standards").setIsNullable(true).setLimit(4_000).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_ad_hoc").setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_external").setIsNullable(false).build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .addColumn(NULLABLE_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newVarcharColumnDefBuilder(TEMPLATE_UUID_COL_NAME).setIsNullable(true).setLimit(UUID_SIZE).build())
        .addColumn(newVarcharColumnDefBuilder("education_principles").setLimit(255).setIsNullable(true).build())
        .build());
    addIndex(context, "rules", "rules_repo_key", true, pluginRuleKeyCol, pluginNameCol);
    addIndex(context, "rules", "rules_organization_uuid", false, organizationUuidCol, pluginNameCol);
  }

  public void createRulesMetadata(Context context) {
    String tableName = "rules_metadata";
    context.execute(newTableBuilder(tableName)
      .addPkColumn(newVarcharColumnDefBuilder(RULE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
      .addPkColumn(newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("note_data").setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("note_user_uuid").setLimit(USER_UUID_SIZE).setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("note_created_at").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("note_updated_at").setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("remediation_function").setLimit(20).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("remediation_gap_mult").setLimit(20).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("remediation_base_effort").setLimit(20).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("tags").setLimit(4_000).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("ad_hoc_name").setLimit(200).setIsNullable(true).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("ad_hoc_description").setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("ad_hoc_severity").setLimit(10).setIsNullable(true).build())
      .addColumn(newTinyIntColumnDefBuilder().setColumnName("ad_hoc_type").setIsNullable(true).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(TECHNICAL_UPDATED_AT_COL)
      .withPkConstraintName("pk_rules_metadata")
      .build());
  }

  private void createRulesParameters(Context context) {
    String tableName = "rules_parameters";
    VarcharColumnDef ruleUuidCol = newVarcharColumnDefBuilder(RULE_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef nameCol = newVarcharColumnDefBuilder("name").setLimit(128).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(nameCol)
        .addColumn(newVarcharColumnDefBuilder(DESCRIPTION_COL_NAME).setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("param_type").setLimit(512).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("default_value").setLimit(MAX_SIZE).setIsNullable(true).build())
        .addColumn(ruleUuidCol)
        .build());
    addIndex(context, tableName, "rules_parameters_rule_uuid", false, ruleUuidCol);
    addIndex(context, tableName, "rules_parameters_unique", true, ruleUuidCol, nameCol);
  }

  private void createRulesProfiles(Context context) {
    String tableName = "rules_profiles";
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(100).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder(LANGUAGE_COL_NAME).setLimit(20).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_built_in").setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("rules_updated_at").setLimit(100).setIsNullable(true).build())
        .addColumn(DEPRECATED_TECHNICAL_CREATED_AT_COL)
        .addColumn(DEPRECATED_TECHNICAL_UPDATED_AT_COL)
        .build());
  }

  private void createSamlMessageIds(Context context) {
    String tableName = "saml_message_ids";
    VarcharColumnDef messageIdCol = newVarcharColumnDefBuilder("message_id").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(messageIdCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName(EXPIRATION_DATE_COL_NAME).setIsNullable(false).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());
    addIndex(context, tableName, "saml_message_ids_unique", true, messageIdCol);
  }

  private void createScannerAnalysisCache(Context context) {
    String tableName = "scanner_analysis_cache";
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnDefBuilder().setColumnName(BRANCH_UUID_COL_NAME).setIsNullable(false).setLimit(UUID_SIZE).build())
        .addColumn(newBlobColumnDefBuilder().setColumnName("data").setIsNullable(false).build())
        .build());
  }

  private void createSnapshots(Context context) {
    String tableName = "snapshots";
    VarcharColumnDef uuidCol = newVarcharColumnDefBuilder("uuid").setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(OLD_UUID_VARCHAR_SIZE).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(uuidCol)
        .addColumn(componentUuidCol)
        .addColumn(newVarcharColumnDefBuilder(STATUS_COL_NAME).setLimit(4).setIsNullable(false).setDefaultValue("U").build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("islast").setIsNullable(false).setDefaultValue(false).build())
        .addColumn(newVarcharColumnDefBuilder("version").setLimit(500).setIsNullable(true).build())
        .addColumn(newIntegerColumnDefBuilder().setColumnName("purge_status").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("build_string").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("revision").setLimit(100).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("build_date").setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("period1_mode").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("period1_param").setLimit(100).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("period1_date").setIsNullable(true).build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .build());
    addIndex(context, tableName, "snapshot_component", false, componentUuidCol);
  }

  private void createUserRoles(Context context) {
    String tableName = "user_roles";
    VarcharColumnDef componentUuidCol = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(USER_UUID_SIZE).setIsNullable(true).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("role").setLimit(64).setIsNullable(false).build())
        .addColumn(componentUuidCol)
        .addColumn(userUuidCol)
        .build());
    addIndex(context, tableName, "user_roles_component_uuid", false, componentUuidCol);
    addIndex(context, tableName, "user_roles_user", false, userUuidCol);
  }

  private void createUserDismissedMessage(Context context) {
    String tableName = "user_dismissed_messages";
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(USER_UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef messageTypeCol = newVarcharColumnDefBuilder("message_type").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(userUuidCol)
        .addColumn(projectUuidCol)
        .addColumn(messageTypeCol)
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .build());
    addIndex(context, tableName, "uniq_user_dismissed_messages", true, userUuidCol, projectUuidCol, messageTypeCol);
    addIndex(context, tableName, "udm_project_uuid", false, projectUuidCol);
    addIndex(context, tableName, "udm_message_type", false, messageTypeCol);
  }

  private void createUserTokens(Context context) {
    String tableName = "user_tokens";
    VarcharColumnDef userUuidCol = newVarcharColumnDefBuilder(USER_UUID_COL_NAME).setLimit(USER_UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef nameCol = newVarcharColumnDefBuilder("name").setLimit(100).setIsNullable(false).build();
    VarcharColumnDef tokenHashCol = newVarcharColumnDefBuilder("token_hash").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef projectKeyCol = newVarcharColumnDefBuilder("project_key").setLimit(255).setIsNullable(true).build();
    VarcharColumnDef typeCol = newVarcharColumnDefBuilder("type").setLimit(100).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(UUID_COL)
        .addColumn(userUuidCol)
        .addColumn(nameCol)
        .addColumn(tokenHashCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_connection_date").setIsNullable(true).build())
        .addColumn(TECHNICAL_CREATED_AT_COL)
        .addColumn(projectKeyCol)
        .addColumn(typeCol)
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName(EXPIRATION_DATE_COL_NAME).setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "user_tokens_user_uuid_name", true, userUuidCol, nameCol);
    addIndex(context, tableName, "user_tokens_token_hash", true, tokenHashCol);
  }

  private void createUsers(Context context) {
    String tableName = "users";
    VarcharColumnDef loginCol = newVarcharColumnDefBuilder("login").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef externalLoginCol = newVarcharColumnDefBuilder("external_login").setLimit(255).setIsNullable(false).build();
    VarcharColumnDef externalIdentityProviderCol = newVarcharColumnDefBuilder("external_identity_provider").setLimit(100).setIsNullable(false).build();
    VarcharColumnDef externalIdCol = newVarcharColumnDefBuilder("external_id").setLimit(255).setIsNullable(false).build();
    context.execute(
      newTableBuilder(tableName)
        .addPkColumn(newVarcharColumnDefBuilder("uuid").setLimit(USER_UUID_SIZE).setIsNullable(false).build())
        .addColumn(loginCol)
        .addColumn(newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("name").setLimit(200).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("email").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("crypted_password").setLimit(100).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("salt").setLimit(40).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("hash_method").setLimit(10).setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("active").setDefaultValue(true).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("scm_accounts").setLimit(MAX_SIZE).build())
        .addColumn(externalLoginCol)
        .addColumn(externalIdentityProviderCol)
        .addColumn(externalIdCol)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("is_root").setIsNullable(false).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("user_local").setIsNullable(true).build())
        .addColumn(newBooleanColumnDefBuilder().setColumnName("onboarded").setIsNullable(false).build())
        .addColumn(newVarcharColumnDefBuilder("homepage_type").setLimit(40).setIsNullable(true).build())
        .addColumn(newVarcharColumnDefBuilder("homepage_parameter").setLimit(40).setIsNullable(true).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_connection_date").setIsNullable(true).build())
        .addColumn(NULLABLE_TECHNICAL_CREATED_AT_COL)
        .addColumn(NULLABLE_TECHNICAL_UPDATED_AT_COL)
        .addColumn(newBooleanColumnDefBuilder().setColumnName("reset_password").setIsNullable(false).build())
        .addColumn(newBigIntegerColumnDefBuilder().setColumnName("last_sonarlint_connection").setIsNullable(true).build())
        .build());
    addIndex(context, tableName, "users_login", true, loginCol);
    addIndex(context, tableName, "users_updated_at", false, NULLABLE_TECHNICAL_UPDATED_AT_COL);
    addIndex(context, tableName, "uniq_external_id", true, externalIdentityProviderCol, externalIdCol);
    addIndex(context, tableName, "uniq_external_login", true, externalIdentityProviderCol, externalLoginCol);
  }

  private void createWebhookDeliveries(Context context) {
    String tableName = "webhook_deliveries";
    VarcharColumnDef componentUuidColumn = newVarcharColumnDefBuilder(COMPONENT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(false).build();
    VarcharColumnDef ceTaskUuidColumn = newVarcharColumnDefBuilder("ce_task_uuid").setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef webhookUuidColumn = newVarcharColumnDefBuilder("webhook_uuid").setLimit(UUID_SIZE).setIsNullable(false).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(webhookUuidColumn)
      .addColumn(componentUuidColumn)
      .addColumn(ceTaskUuidColumn)
      .addColumn(newVarcharColumnDefBuilder(ANALYSIS_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build())
      .addColumn(newVarcharColumnDefBuilder("name").setLimit(100).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder("url").setLimit(2_000).setIsNullable(false).build())
      .addColumn(newBooleanColumnDefBuilder().setColumnName("success").setIsNullable(false).build())
      .addColumn(newIntegerColumnDefBuilder().setColumnName("http_status").setIsNullable(true).build())
      .addColumn(newBigIntegerColumnDefBuilder().setColumnName("duration_ms").setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("payload").setIsNullable(false).build())
      .addColumn(newClobColumnDefBuilder().setColumnName("error_stacktrace").setIsNullable(true).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .build());
    addIndex(context, tableName, COMPONENT_UUID_COL_NAME, false, componentUuidColumn);
    addIndex(context, tableName, "ce_task_uuid", false, ceTaskUuidColumn);
    addIndex(context, tableName, "idx_wbhk_dlvrs_wbhk_uuid", false, webhookUuidColumn);
  }

  private void createWebhooks(Context context) {
    String tableName = "webhooks";
    VarcharColumnDef organizationUuidCol = newVarcharColumnDefBuilder(ORGANIZATION_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    VarcharColumnDef projectUuidCol = newVarcharColumnDefBuilder(PROJECT_UUID_COL_NAME).setLimit(UUID_SIZE).setIsNullable(true).build();
    context.execute(newTableBuilder(tableName)
      .addPkColumn(UUID_COL)
      .addColumn(organizationUuidCol)
      .addColumn(projectUuidCol)
      .addColumn(newVarcharColumnDefBuilder("name").setLimit(100).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder("url").setLimit(2_000).setIsNullable(false).build())
      .addColumn(newVarcharColumnDefBuilder("secret").setLimit(200).setIsNullable(true).build())
      .addColumn(TECHNICAL_CREATED_AT_COL)
      .addColumn(NULLABLE_TECHNICAL_UPDATED_AT_COL)
      .build());
    addIndex(context, tableName, "organization_webhook", false, organizationUuidCol);
  }

  private void addIndex(Context context, String table, String index, boolean unique, ColumnDef firstColumn, ColumnDef... otherColumns) {
    CreateIndexBuilder builder = new CreateIndexBuilder(getDialect())
      .setTable(table)
      .setName(index)
      .setUnique(unique);
    concat(of(firstColumn), stream(otherColumns)).forEach(builder::addColumn);
    context.execute(builder.build());
  }

  private CreateTableBuilder newTableBuilder(String tableName) {
    return new CreateTableBuilder(getDialect(), tableName);
  }
}
