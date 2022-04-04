/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v86;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion86 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(4100, "Drop QPROFILES_ORG_UUID index from 'quality_profile' table", DropOrganizationUuidIndexFromQualityProfileTable.class)
      .add(4101, "Drop organization_uuid from 'quality_profile' table", DropOrganizationFromQualityProfileTable.class)
      .add(4102, "Drop primary key of table 'default_qprofiles'", DropDefaultQProfilesPk.class)
      .add(4103, "Drop organization_uuid from 'default_qprofiles' table", DropOrganizationFromDefaultQProfiles.class)
      .add(4104, "Add primary key to the table 'default_qprofiles", AddPrimaryKeyToDefaultQProfiles.class)
      .add(4105, "Drop 'organization_uuid' in 'rules_metadata'", DropOrganizationInRulesMetadata.class)
      .add(4106, "Update 'change_data' column of 'qprofile_changes' table to use ruleUuid", UpdateChangeDataOfQProfileChanges.class)
      .add(4107, "Drop 'organization_uuid' in 'users'", DropOrganizationInUsers.class)
      .add(4108, "Drop 'organization_uuid' in 'user_roles'", DropOrganizationInUserRoles.class)
      .add(4109, "Drop 'organization_uuid' in 'group_roles'", DropOrganizationInGroupRoles.class)
      .add(4110, "Drop 'organization_uuid' in 'permission_templates'", DropOrganizationInPermissionTemplates.class)
      .add(4111, "Drop 'organization_uuid' in 'groups'", DropOrganizationInGroups.class)
      .add(4112, "Make 'name' column in 'groups' table not nullable", MakeNameColumnInGroupsTableNotNullable.class)
      .add(4113, "Make 'name' column in 'groups' table unique", AddUniqueIndexOnNameColumnOfGroupsTable.class)
      .add(4114, "Move default permission templates to internal properties", MoveDefaultTemplatesToInternalProperties.class)
      .add(4115, "Set 'sonar.forceAuthentication' to false for upgraded instances", SetForceAuthenticationSettings.class)

      .add(4116, "Create table 'app_projects'", CreateApplicationProjectsTable.class)
      .add(4117, "Create primary key for 'app_projects'", AddPkToApplicationProjects.class)
      .add(4118, "Create index for 'app_projects'", AddIndexToApplicationProjects.class)

      .add(4119, "Create table 'app_branch_project_branch'", CreateApplicationBranchProjs.class)
      .add(4120, "Create primary key for 'app_branch_project_branch'", AddPkToApplicationBranchProjs.class)
      .add(4121, "Create index for 'app_branch_project_branch'", AddIndexToApplicationBranchProjs.class)

      .add(4122, "Migrate view definitions from xml to db", MigrateApplicationDefinitionsFromXmlToDb.class)

      .add(4123, "Add 'reset_password' column to 'users' table", AddResetPasswordColumnToUsers.class)
      .add(4124, "Populate 'reset_password' column with default value", PopulateResetPasswordDefaultValue.class)
      .add(4125, "Make 'reset_password' column in 'users' table not nullable", MakeResetPasswordColumnNotNull.class)

      .add(4126, "Secure gitlab secret parameters", SecureGitlabSecretParameters.class)
    ;
  }
}
