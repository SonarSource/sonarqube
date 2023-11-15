/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.MigrationDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateInitialSchemaTest {

  private static final Set<String> SCHEMAS_TO_IGNORE = Set.of("INFORMATION_SCHEMA", "sys", "SYS", "SYSTEM", "CTXSYS", "MDSYS", "XDB");

  @Rule
  public final MigrationDbTester dbTester = MigrationDbTester.createForMigrationStep(CreateInitialSchema.class);

  private final CreateInitialSchema underTest = new CreateInitialSchema(dbTester.database());

  @Test
  public void creates_tables_on_empty_db() throws Exception {
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
        "app_branch_project_branch",
        "alm_pats",
        "app_projects",
        "alm_settings",
        "audits",
        "project_alm_settings",
        "analysis_properties",
        "ce_activity",
        "ce_queue",
        "ce_scanner_context",
        "ce_task_characteristics",
        "ce_task_input",
        "ce_task_message",
        "components",
        "default_qprofiles",
        "deprecated_rule_keys",
        "duplications_index",
        "es_queue",
        "events",
        "event_component_changes",
        "file_sources",
        "groups",
        "groups_users",
        "group_roles",
        "internal_component_props",
        "internal_properties",
        "issues",
        "issue_changes",
        "live_measures",
        "metrics",
        "new_code_periods",
        "new_code_reference_issues",
        "notifications",
        "org_qprofiles",
        "permission_templates",
        "perm_templates_groups",
        "perm_templates_users",
        "perm_tpl_characteristics",
        "plugins",
        "portfolios",
        "portfolio_projects",
        "portfolio_proj_branches",
        "portfolio_references",
        "projects",
        "project_badge_token",
        "project_branches",
        "project_links",
        "project_mappings",
        "project_measures",
        "project_qprofiles",
        "project_qgates",
        "properties",
        "push_events",
        "qgate_group_permissions",
        "qgate_user_permissions",
        "qprofile_changes",
        "qprofile_edit_groups",
        "qprofile_edit_users",
        "quality_gates",
        "quality_gate_conditions",
        "rules",
        "rules_parameters",
        "rules_profiles",
        "rule_repositories",
        "rule_desc_sections",
        "saml_message_ids",
        "scanner_analysis_cache",
        "schema_migrations",
        "scim_users",
        "session_tokens",
        "snapshots",
        "users",
        "user_dismissed_messages",
        "user_roles",
        "user_tokens",
        "webhooks",
        "webhook_deliveries");
  }

}
