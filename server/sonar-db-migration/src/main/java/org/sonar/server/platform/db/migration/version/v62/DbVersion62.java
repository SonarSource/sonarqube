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
package org.sonar.server.platform.db.migration.version.v62;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion62 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1400, "Create table ORGANIZATIONS", CreateTableOrganizations.class)
      .add(1401, "Create default organization", CreateDefaultOrganization.class)
      .add(1402, "Delete permission shareDashboard", DeletePermissionShareDashboard.class)
      .add(1403, "Add column GROUPS.ORGANIZATION_UUID", AddOrganizationUuidToGroups.class)
      .add(1404, "Add column USERS.IS_ROOT", AddIsRootColumnOnTableUsers.class)
      .add(1405, "Populate column USERS.IS_ROOT", PopulateIsRootColumnOnTableUsers.class)
      .add(1406, "Make column USERS.IS_ROOT not nullable", MakeRootColumnNotNullOnTableUsers.class)
      .add(1407, "Populate column GROUPS.ORGANIZATION_UUID", PopulateOrganizationUuidOfGroups.class)
      .add(1408, "Make column GROUPS.ORGANIZATION_UUID not nullable", MakeOrganizationUuidNotNullOnGroups.class)
      .add(1409, "Add column USER_ROLES.ORGANIZATION_UUID", AddOrganizationUuidToUserRoles.class)
      .add(1410, "Populate column USER_ROLES.ORGANIZATION_UUID", PopulateOrganizationUuidOfUserRoles.class)
      .add(1411, "Make column USER_ROLES.ORGANIZATION_UUID not nullable", MakeOrganizationUuidNotNullOnUserRoles.class)
      .add(1412, "Add column PERMISSION_TEMPLATES.ORGANIZATION_UUID", AddOrganizationUuidToPermissionTemplates.class)
      .add(1413, "Populate column PERMISSION_TEMPLATES.ORGANIZATION_UUID", PopulateOrganizationUuidOfPermissionTemplates.class)
      .add(1414, "Make column PERMISSION_TEMPLATES.ORGANIZATION_UUID not nullable", MakeOrganizationUuidNotNullOnPermissionTemplates.class)
      .add(1415, "Add column GROUP_ROLES.ORGANIZATION_UUID", AddOrganizationUuidToGroupRoles.class)
      .add(1416, "Populate column GROUP_ROLES.ORGANIZATION_UUID", PopulateOrganizationUuidOfGroupRoles.class)
      .add(1417, "Make column GROUP_ROLES.ORGANIZATION_UUID not nullable", MakeOrganizationUuidNotNullOnGroupRoles.class)
      .add(1418, "Add ORGANIZATION_UUID to index uniq_group_roles", IncludeOrganizationUuidInUniqueIndexOfGroupRoles.class)
      .add(1419, "Update qualigate conditions on coverage", UpdateQualityGateConditionsOnCoverage.class)
      .add(1420, "Drop tables related to dashboards", DropRelatedDashboardTables.class)
      .add(1421, "Drop tables related to measure filters", DropMeasureFiltersTables.class)
      .add(1422, "Drop tables related to issue filters", DropIssueFiltersTables.class)
      .add(1423, "Create table WEBHOOK_DELIVERIES", CreateTableWebhookDeliveries.class);
  }
}
