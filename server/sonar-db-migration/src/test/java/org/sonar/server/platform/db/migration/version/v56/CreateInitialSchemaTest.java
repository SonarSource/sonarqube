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

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.db.CoreDbTester;

import static org.assertj.core.api.Assertions.assertThat;

public class CreateInitialSchemaTest {

  @Rule
  public final CoreDbTester dbTester = CoreDbTester.createForSchema(CreateInitialSchemaTest.class, "empty.sql");

  private CreateInitialSchema underTest = new CreateInitialSchema(dbTester.database());

  @Test
  public void creates_tables_on_empty_db() throws Exception {
    underTest.execute();

    List<String> tables = new ArrayList<>();
    try (Connection connection = dbTester.openConnection();
      ResultSet rs = connection.getMetaData().getTables(null, null, null, new String[] {"TABLE"})) {

      while (rs.next()) {
        tables.add(rs.getString("TABLE_NAME").toLowerCase(Locale.ENGLISH));
      }
    }
    assertThat(tables).containsOnly(
      "active_dashboards",
      "active_rules",
      "active_rule_parameters",
      "activities",
      "authors",
      "ce_activity",
      "ce_queue",
      "dashboards",
      "duplications_index",
      "events",
      "file_sources",
      "groups",
      "groups_users",
      "group_roles",
      "issues",
      "issue_changes",
      "issue_filters",
      "issue_filter_favourites",
      "loaded_templates",
      "manual_measures",
      "measure_filters",
      "measure_filter_favourites",
      "metrics",
      "notifications",
      "permission_templates",
      "perm_templates_groups",
      "perm_templates_users",
      "projects",
      "project_links",
      "project_measures",
      "project_qprofiles",
      "properties",
      "quality_gates",
      "quality_gate_conditions",
      "resource_index",
      "rules",
      "rules_parameters",
      "rules_profiles",
      "snapshots",
      "users",
      "user_roles",
      "user_tokens",
      "widgets",
      "widget_properties");
  }

}
