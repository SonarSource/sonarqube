/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202506;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202506 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2025_06_000, "Create 'jira_project_bindings' table", CreateJiraProjectBindingsTable.class)
      .add(2025_06_001, "Create 'atlassian_auth_details' table", CreateAtlassianAuthenticationDetailsTable.class)
      .add(2025_06_002, "Create 'xsrf_tokens' table", CreateXsrfTokensTable.class)
      .add(2025_06_003, "Create 'jira_org_bindings' table", CreateJiraOrganizationBindingsTable.class)
      .add(2025_06_004, "Create 'jira_org_bindings_pending' table", CreateJiraOrganizationBindingsPendingTable.class)
      .add(2025_06_005, "Create 'jira_selected_work_types' table", CreateJiraSelectedWorkTypesTable.class)
      .add(2025_06_006, "Create 'jira_work_items' table", CreateJiraWorkItemsTable.class)
      .add(2025_06_007, "Create 'jira_work_items_linked_resources' table", CreateJiraWorkItemsResourcesTable.class)
      .add(2025_06_008, "Create 'integration_configs' table", CreateIntegrationConfigurationsTable.class)
      .add(2025_06_009, "Create 'slack_subscriptions' table", CreateSlackSubscriptionsTable.class)
      .add(2025_06_010, "Create 'slack_workspaces' table", CreateSlackWorkspacesTable.class)
      .add(2025_06_011, "Create 'user_bindings' table", CreateUserBindingsTable.class)
      .add(2025_06_012, "Create 'user_bindings_slack' table", CreateUserBindingsSlackTable.class)
      .add(2025_06_015, "Add 'created_by' column to 'jira_work_items' table", AddCreatedByColumnToJiraWorkItemsTable.class)
      .add(2025_06_016, "Add index based on 'resource_id' and 'resource_type' to 'jira_work_items_resources' table", AddResourceIndexForJiraWorkItemsResourcesTable.class)
      .add(2025_06_017, "Remove unique index from 'user_bindings' table", RemoveUniqueIndexFromUserBindingsTable.class)
      .add(2025_06_018, "Create index on 'user_uuid' column in 'user_bindings' table", CreateIndexOnUserBindingsUserUuid.class)
      .add(2025_06_019, "Add 'is_token_shared' column to 'jira_org_bindings' table", AddIsTokenSharedToJiraOrgBindingsTable.class)
      .add(2025_06_021, "Add index based on 'jira_organization_binding_id' and 'sonar_project_id' to 'jira_project_bindings' table", AddUniqueIndexForJiraProjectBindingsTable.class)
      .add(2025_06_022, "Create index on 'components' for case-insensitive key queries", CreateIndexOnComponentsLowerKee.class)
      .add(2025_06_023, "Drop table 'issue_stats_by_rule_key'", DropIssueStatsByRuleKeyTable.class)
      .add(2025_06_024, "Create 'issue_stats_by_rule_key' table", CreateIssueStatsByRuleKeyTable.class)
      .add(2025_06_025, "Populate 'issue_stats_by_rule_key'", PopulateIssueStatsByRuleKey.class)
      .add(2025_06_026, "Populate 'issue_stats_by_rule_key' for portfolios and apps", PopulateIssueStatsByRuleKeyForPortfoliosAndApps.class)
      .add(2025_06_027, "Add 'part_number' column to 'ce_task_input'", AddPartNumberColumnToCeTaskInputTable.class)
      .add(2025_06_028, "Add 'part_count' column to 'ce_queue'", AddPartCountColumnToCeQueueTable.class)
    ;
  }

}
