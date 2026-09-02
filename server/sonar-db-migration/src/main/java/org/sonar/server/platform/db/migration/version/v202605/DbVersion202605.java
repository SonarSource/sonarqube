/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202605;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202605 implements DbVersion {

  @Override
  @SuppressWarnings("java:S3937")
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2026_05_000, "Create table 'a3s_contexts'", CreateA3SContextsTable.class)
      .add(2026_05_001, "Create table 'a3s_context_items'", CreateA3SContextItemsTable.class)
      .add(2026_05_002, "Create table 'a3s_analysis_usages'", CreateA3SAnalysisUsagesTable.class)
      .add(2026_05_003, "Create table 'agent_schedules'", CreateAgentSchedulesTable.class)
      .add(2026_05_004, "Create table 'agent_sched_proc_issues'", CreateAgentScheduleProcessedIssuesTable.class)
      .add(2026_05_005, "Add 'reachability_analyzed' to 'sca_issues_releases'", AddReachabilityAnalyzedToScaIssuesReleases.class)
      .add(2026_05_006, "Seed global '*' row in 'agent_schedules'", SeedGlobalAgentSchedule.class)
      .add(2026_05_007, "Create 'issue_count_dimensions' table", CreateIssueCountDimensionsTable.class)
      .add(2026_05_008, "Create 'issue_count_history' table", CreateIssueCountHistoryTable.class)
      .add(2026_05_009, "Create 'measure_key_mapping' table", CreateMeasureKeyMappingTable.class)
      .add(2026_05_010, "Create 'measure_history' table", CreateMeasureHistoryTable.class)
      .add(2026_05_011, "Increase issue_stats_by_rule_key.rule_key column size", IncreaseIssueStatsRuleKeyColumnSize.class)
      .add(2026_05_012, "Create table 'sca_issue_dimensions'", CreateScaIssueDimensionsTable.class)
      .add(2026_05_013, "Create table 'sca_ttr_history'", CreateScaTtrHistoryTable.class)
      .add(2026_05_014, "Make 'branch_name' nullable in 'a3s_contexts'", MakeA3sContextsBranchNameNullable.class)
      .add(2026_05_015, "Add 'producer' column to 'issues' table", AddProducerColumnToIssuesTable.class)
      .add(2026_05_016, "Create table 'agentic_job'", CreateAgenticJobTable.class)
      .add(2026_05_017, "Create table 'llm_providers'", CreateLlmProvidersTable.class)
      .add(2026_05_018, "Create table 'llm_provider_mappings'", CreateLlmProviderMappingsTable.class)
      .add(2026_05_019, "Create table 'arch_boundary_descriptors'", CreateArchitectureBoundaryDescriptorsTable.class)
      .add(2026_05_020, "Rename table 'arch_intended' to 'arch_models'", RenameArchIntendedToArchModels.class)
      .add(2026_05_021, "Rename index 'arch_intended_uuid' to 'arch_models_uuid'", RenameArchModelsUuidIndex.class)
      .add(2026_05_022, "Create table 'arch_patterns'", CreateArchPatternsTable.class)
      .add(2026_05_023, "Create table 'arch_model_patterns'", CreateArchModelPatternsTable.class)
      .add(2026_05_024, "Rename constraint 'pk_arch_intended' to 'pk_arch_models'", RenameArchModelsPrimaryKeyConstraint.class)
      .add(2026_05_025, "Create table 'arch_org_placeholders'", CreateArchOrganizationPlaceholdersTable.class)
      .add(2026_05_026, "Create table 'arch_proj_relations'", CreateArchProjectRelationshipsTable.class)
      .add(2026_05_027, "Create table 'arch_proj_org_compo'", CreateArchProjectOrgComponentTable.class)
      .add(2026_05_028, "Create 'dashboards' table", CreateDashboardsTable.class)
      .add(2026_05_029, "Create table 'issue_ttr_history'", CreateIssueTtrHistoryTable.class)
      .add(2026_05_030, "Drop index 'sca_ttr_history_uq_idx'", DropScaTtrHistoryUniqueIndex.class)
      .add(2026_05_031, "Drop index 'sca_ttr_history_ent_type_epoch'", DropScaTtrHistoryEntityEpochIndex.class)
      .add(2026_05_032, "Resize entity_id and entity_type columns in 'sca_ttr_history'", ResizeScaTtrEntityIdAndEntityType.class)
      .add(2026_05_033, "Add 'url' and 'repo_id' columns to 'project_alm_settings' table", AddUrlAndRepoIdColumnsToProjectAlmSettingsTable.class)
      .add(2026_05_034, "Create index 'sca_dependencies_updated_at' on 'sca_dependencies.updated_at'", CreateIndexOnScaDependenciesUpdatedAt.class)
      .add(2026_05_035, "Create index 'sca_ir_cve_loc_updated_at' on 'sca_ir_cve_locations.updated_at'", CreateIndexOnScaIrCveLocationsUpdatedAt.class)
      .add(2026_05_036, "Create 'agent_jobs' table", CreateAgentJobsTable.class)
      .add(2026_05_037, "Create 'remediation_agent_jobs' table", CreateRemediationAgentJobsTable.class)
      .add(2026_05_038, "Drop 'agentic_job' table", DropAgenticJobTable.class)
      .add(2026_05_039, "Create 'findings' table", CreateFindingsTable.class)
      .add(2026_05_040, "Create 'finding_locations' table", CreateFindingLocationsTable.class)
      .add(2026_05_041, "Create 'organization_configs' table", CreateOrganizationConfigsTable.class)
      .add(2026_05_042, "Create 'project_configs' table", CreateProjectConfigsTable.class)
      .add(2026_05_043, "Add 'selected_project_ids' column to 'agent_schedules' table", AddSelectedProjectIdsToAgentSchedulesTable.class)
      .add(2026_05_044, "Add 'dop_user_id' column to 'remediation_agent_jobs' table", AddDopUserIdToRemediationAgentJobsTable.class)
      .add(2026_05_045, "Add 'issues_selection_strategy' column to 'remediation_agent_jobs' table", AddIssuesSelectionStrategyToRemediationAgentJobsTable.class)
      .add(2026_05_046, "Create 'hunter_scheduled_tasks' table", CreateHunterScheduledTasksTable.class)
      .add(2026_05_047, "Make legacy-id columns nullable in 'project_configs'", MakeProjectConfigsLegacyIdsNullable.class)
      .add(2026_05_048, "Create 'agent_orch_tasks' table", CreateAgentOrchTasksTable.class)
      .add(2026_05_049, "Create 'remediation_sched_tasks' table", CreateRemediationSchedTasksTable.class)
      .add(2026_05_050, "Create table 'cag_usage'", CreateCagUsageTable.class)
      .add(2026_05_051, "Create 'hunter_agent_jobs' table", CreateHunterAgentJobsTable.class)
      .add(2026_05_052, "Create 'dop_pr_snapshots' table", CreateDopPrSnapshotsTable.class)
      .add(2026_05_053, "Create 'hunter_agent_runs' table", CreateHunterAgentRunsTable.class)
      .add(2026_05_054, "Create index 'group_roles_ent_role_grp' on 'group_roles'", CreateIndexOnGroupRolesEntityRoleGroup.class)
      .add(2026_05_055, "Add the rule body to 'findings'", AddRuleBodyToFindingsTable.class)
      .add(2026_05_056, "Add flow metadata to 'finding_locations'", AddFlowMetadataToFindingLocationsTable.class)
      .add(2026_05_057, "Add 'expected_credits' to 'agent_jobs'", AddExpectedCreditsToAgentJobsTable.class)
      .add(2026_05_058, "Add 'analyzed_at' to 'hunter_agent_jobs'", AddAnalyzedAtToHunterAgentJobsTable.class)
      .add(2026_05_059, "Create table 'vortex_cag_events'", CreateVortexCagEventsTable.class)
      .add(2026_05_060, "Create table 'vortex_sqaa_events'", CreateVortexSqaaEventsTable.class);
  }
}
