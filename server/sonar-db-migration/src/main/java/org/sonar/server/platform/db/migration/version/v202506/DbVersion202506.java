/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
    ;
  }
}
