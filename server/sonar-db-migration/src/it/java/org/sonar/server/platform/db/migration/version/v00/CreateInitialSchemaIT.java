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
package org.sonar.server.platform.db.migration.version.v00;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInitialSchemaIT {

  private static final Set<String> SCHEMAS_TO_IGNORE = Set.of("INFORMATION_SCHEMA", "sys", "SYS", "SYSTEM", "CTXSYS", "MDSYS", "XDB");

  @RegisterExtension
  public final MigrationDbTester dbTester = MigrationDbTester.createForMigrationStep(CreateInitialSchema.class);

  private final CreateInitialSchema underTest = new CreateInitialSchema(dbTester.database());

  @Test
  void creates_tables_on_empty_db() throws Exception {
    underTest.execute();

    List<String> tables = new ArrayList<>();
    try (Connection connection = dbTester.openConnection();
         ResultSet rs = connection.getMetaData().getTables(null, null, null, new String[]{"TABLE"})) {

      while (rs.next()) {
        String schema = rs.getString("TABLE_SCHEM");
        if (!SCHEMAS_TO_IGNORE.contains(schema)) {
          tables.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ENGLISH));
        }
      }
    }

    assertThat(tables)
      .containsOnly(
        "active_rules",
        "active_rule_parameters",
        "alm_pats",
        "alm_settings",
        "analysis_properties",
        "anticipated_transitions",
        "app_branch_project_branch",
        "app_projects",
        "architecture_graphs",
        "atlassian_auth_details",
        "audits",
        "ce_activity",
        "ce_queue",
        "ce_scanner_context",
        "ce_task_characteristics",
        "ce_task_input",
        "ce_task_message",
        "components",
        "default_qprofiles",
        "deprecated_rule_keys",
        "devops_perms_mapping",
        "duplications_index",
        "es_queue",
        "event_component_changes",
        "events",
        "external_groups",
        "file_sources",
        "github_orgs_groups",
        "group_roles",
        "groups",
        "groups_users",
        "integration_configs",
        "internal_component_props",
        "internal_properties",
        "issue_changes",
        "issue_stats_by_rule_key",
        "issues",
        "issues_fixed",
        "issues_impacts",
        "jira_org_bindings",
        "jira_org_bindings_pending",
        "jira_project_bindings",
        "jira_selected_work_types",
        "jira_work_items",
        "jira_work_items_resources",
        "measures",
        "metrics",
        "migration_logs",
        "new_code_periods",
        "new_code_reference_issues",
        "notifications",
        "org_qprofiles",
        "perm_templates_groups",
        "perm_templates_users",
        "perm_tpl_characteristics",
        "permission_templates",
        "plugins",
        "portfolio_proj_branches",
        "portfolio_projects",
        "portfolio_references",
        "portfolios",
        "project_alm_settings",
        "project_badge_token",
        "project_branches",
        "project_dependencies",
        "project_links",
        "project_measures",
        "project_qgates",
        "project_qprofiles",
        "projects",
        "properties",
        "push_events",
        "qgate_group_permissions",
        "qgate_user_permissions",
        "qprofile_changes",
        "qprofile_edit_groups",
        "qprofile_edit_users",
        "quality_gate_conditions",
        "quality_gates",
        "report_schedules",
        "report_subscriptions",
        "rule_changes",
        "rule_desc_sections",
        "rule_impact_changes",
        "rule_repositories",
        "rule_tags",
        "rules",
        "rules_default_impacts",
        "rules_parameters",
        "rules_profiles",
        "saml_message_ids",
        "scanner_analysis_cache",
        "sca_analyses",
        "sca_dependencies",
        "sca_encountered_licenses",
        "sca_issue_rels_changes",
        "sca_issues",
        "sca_issues_releases",
        "sca_lic_prof_categories",
        "sca_lic_prof_customs",
        "sca_lic_prof_projects",
        "sca_license_profiles",
        "sca_releases",
        "sca_vulnerability_issues",
        "schema_migrations",
        "scim_groups",
        "scim_users",
        "scm_accounts",
        "session_tokens",
        "slack_subscriptions",
        "slack_workspaces",
        "snapshots",
        "telemetry_metrics_sent",
        "user_ai_tool_usages",
        "user_bindings",
        "user_bindings_slack",
        "user_dismissed_messages",
        "user_roles",
        "user_tokens",
        "users",
        "webhook_deliveries",
        "webhooks",
        "xsrf_tokens");
  }

}
