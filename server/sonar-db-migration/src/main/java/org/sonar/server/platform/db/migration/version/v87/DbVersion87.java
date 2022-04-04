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
package org.sonar.server.platform.db.migration.version.v87;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion87 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(4200, "Move default project visibility to global properties", MoveDefaultProjectVisibilityToGlobalProperties.class)
      .add(4201, "Move default quality gate to global properties", MoveDefaultQualityGateToGlobalProperties.class)

      .add(4202, "Drop organization_uuid in table 'components'", DropOrganizationInComponents.class)
      .add(4203, "Drop organization_uuid in table 'projects'", DropOrganizationInProjects.class)
      .add(4204, "Drop organizations in table 'webhooks'", DropOrganizationInWebhooks.class)

      .add(4205, "Drop table 'org_quality_gates'", DropOrgQualityGatesTable.class)
      .add(4206, "Drop table 'organization_alm_bindings'", DropOrganizationAlmBindingsTable.class)
      .add(4207, "Drop table 'alm_app_installs'", DropAlmAppInstallsTable.class)
      .add(4208, "Drop table 'organizations'", DropOrganizationsTable.class)
      .add(4209, "Drop table 'organization_members'", DropOrgMembersTable.class)

      .add(4210, "Add column 'monorepo' to table 'project_alm_settings'", AddMonorepoColumnToProjectAlmSettingsTable.class)
      .add(4211, "Populate column 'monorepo' to false in table 'project_alm_settings'", PopulateMonorepoColumnToProjectAlmSettingsTable.class)
      .add(4212, "Make column 'monorepo' in table 'project_alm_settings' not Nullable", MakeMonorepoColumnInProjectAlmSettingsTableNotNullable.class)

      .add(4213, "Drop column 'description' in table 'project_measures'", DropDescriptionInProjectMeasures.class)

    ;
  }
}
