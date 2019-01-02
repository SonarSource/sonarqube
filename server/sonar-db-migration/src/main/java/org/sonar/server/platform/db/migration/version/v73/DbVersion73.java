/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v73;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion73 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2200, "Populate PROJECT_BRANCHES with existing main application branches", PopulateMainApplicationBranches.class)
      .add(2201, "Add 'securityhotspotadmin' permission to all permission templates groups already having 'issueadmin'", PopulateHotspotAdminPermissionOnTemplatesGroups.class)
      .add(2202, "Add 'securityhotspotadmin' permission to all permission templates users already having 'issueadmin'", PopulateHotspotAdminPermissionOnTemplatesUsers.class)
      .add(2203, "Add 'securityhotspotadmin' permission to all groups already having 'issueadmin'", PopulateHotspotAdminPermissionOnGroups.class)
      .add(2204, "Add 'securityhotspotadmin' permission to all users already having 'issueadmin'", PopulateHotspotAdminPermissionOnUsers.class)
      .add(2205, "Add 'from hotspot' flag to issues", AddFromHotspotFlagToIssues.class)
      .add(2206, "Add SUBSCRIPTION column to ORGANIZATIONS table", AddSubscriptionToOrganizations.class)
      .add(2207, "Populate SUBSCRIPTION in ORGANIZATIONS", PopulateSubscriptionOnOrganizations.class)
      .add(2208, "Add rules.security_standards", AddSecurityStandardsToRules.class)
      .add(2209, "Fix missing quality profiles on organizations", FixMissingQualityProfilesOnOrganizations.class)
      .add(2210, "Add 'securityhotspotadmin' permission to templates characteristics already having 'issueadmin'", PopulateHotspotAdminPermissionOnTemplatesCharacteristics.class)
      .add(2211, "Set SUBSCRIPTION not nullable in ORGANIZATIONS", SetSubscriptionOnOrganizationsNotNullable.class)
      .add(2212, "Add index on ORGANIZATION_MEMBERS", AddIndexOnOrganizationMembers.class)
      .add(2213, "Create table to store project ALM bindings", CreateProjectAlmBindingsTable.class)
    ;
  }
}
