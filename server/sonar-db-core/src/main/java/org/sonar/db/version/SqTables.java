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
package org.sonar.db.version;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

public final class SqTables {
  /**
   * These tables are still involved in DB migrations, so potentially
   * incorrect collation must be fixed so that joins with other
   * tables are possible.
   */
  public static final Set<String> OLD_DROPPED_TABLES = unmodifiableSet(new HashSet<>(asList(
    "active_dashboards",
    "activities",
    "dashboards",
    "issue_filters",
    "issue_filter_favourites",
    "loaded_templates",
    "measure_filters",
    "measure_filter_favourites",
    "resource_index",
    "widgets",
    "widget_properties")));

  /**
   * List of all the tables.
   * This list is hardcoded because we didn't succeed in using java.sql.DatabaseMetaData#getTables() in the same way
   * for all the supported databases, particularly due to Oracle results.
   */
  public static final Set<String> TABLES = unmodifiableSet(new HashSet<>(asList(
    "active_rules",
    "active_rule_parameters",
    "analysis_properties",
    "ce_activity",
    "ce_queue",
    "ce_task_characteristics",
    "ce_task_input",
    "ce_scanner_context",
    "default_qprofiles",
    "deprecated_rule_keys",
    "duplications_index",
    "es_queue",
    "events",
    "file_sources",
    "groups",
    "groups_users",
    "group_roles",
    "internal_properties",
    "issues",
    "issue_changes",
    "live_measures",
    "manual_measures",
    "metrics",
    "notifications",
    "organizations",
    "organization_members",
    "org_qprofiles",
    "org_quality_gates",
    "permission_templates",
    "perm_templates_users",
    "perm_templates_groups",
    "perm_tpl_characteristics",
    "plugins",
    "projects",
    "project_branches",
    "project_links",
    "project_measures",
    "project_qprofiles",
    "properties",
    "qprofile_changes",
    "qprofile_edit_groups",
    "qprofile_edit_users",
    "quality_gates",
    "quality_gate_conditions",
    "rules",
    "rules_metadata",
    "rules_parameters",
    "rules_profiles",
    "rule_repositories",
    "schema_migrations",
    "snapshots",
    "users",
    "user_roles",
    "user_tokens",
    "webhook_deliveries")));

  private SqTables() {
    // prevents instantiation
  }
}
