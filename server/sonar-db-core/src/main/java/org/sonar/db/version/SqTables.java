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
package org.sonar.db.version;

import java.util.Set;

public final class SqTables {

  /**
   * List of all the tables.
   * This list is hardcoded because we didn't succeed in using java.sql.DatabaseMetaData#getTables() in the same way
   * for all the supported databases, particularly due to Oracle results.
   */
  public static final Set<String> TABLES = Set.of(
    "active_rules",
    "active_rule_parameters",
    "alm_settings",
    "alm_pats",
    "analysis_properties",
    "app_branch_project_branch",
    "app_projects",
    "audits",
    "ce_activity",
    "ce_queue",
    "ce_task_characteristics",
    "ce_task_input",
    "ce_task_message",
    "ce_scanner_context",
    "cves",
    "cve_cwe",
    "components",
    "default_qprofiles",
    "deprecated_rule_keys",
    "devops_perms_mapping",
    "duplications_index",
    "es_queue",
    "events",
    "event_component_changes",
    "external_groups",
    "file_sources",
    "github_orgs_groups",
    "groups",
    "groups_users",
    "group_roles",
    "internal_component_props",
    "internal_properties",
    "issues",
    "issues_dependency",
    "issues_fixed",
    "issues_impacts",
    "issue_changes",
    "live_measures",
    "metrics",
    "new_code_periods",
    "new_code_reference_issues",
    "notifications",
    "org_qprofiles",
    "permission_templates",
    "perm_templates_users",
    "perm_templates_groups",
    "perm_tpl_characteristics",
    "plugins",
    "portfolios",
    "portfolio_projects",
    "portfolio_proj_branches",
    "portfolio_references",
    "projects",
    "project_alm_settings",
    "project_badge_token",
    "project_branches",
    "project_links",
    "project_measures",
    "project_qprofiles",
    "project_qgates",
    "properties",
    "push_events",
    "qprofile_changes",
    "qprofile_edit_groups",
    "qprofile_edit_users",
    "quality_gates",
    "qgate_user_permissions",
    "qgate_group_permissions",
    "quality_gate_conditions",
    "saml_message_ids",
    "report_schedules",
    "report_subscriptions",
    "rules",
    "rules_metadata",
    "rule_desc_sections",
    "rule_tags",
    "rules_default_impacts",
    "rules_parameters",
    "rules_profiles",
    "rule_repositories",
    "scanner_analysis_cache",
    "schema_migrations",
    "scim_groups",
    "scim_users",
    "scm_accounts",
    "session_tokens",
    "snapshots",
    "telemetry_metrics_sent",
    "users",
    "user_dismissed_messages",
    "user_roles",
    "user_tokens",
    "webhooks",
    "webhook_deliveries");

  private SqTables() {
    // prevents instantiation
  }
}
