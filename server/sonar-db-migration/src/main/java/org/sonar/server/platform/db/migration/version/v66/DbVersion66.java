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
package org.sonar.server.platform.db.migration.version.v66;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion66 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1801, "Create table CE task characteristics", CreateTableCeTaskCharacteristics.class)
      .add(1802, "Delete leak settings on views", DeleteLeakSettingsOnViews.class)
      .add(1803, "Fix empty USERS.EXTERNAL_IDENTITY and USERS.EXTERNAL_IDENTITY_PROVIDER", FixEmptyIdentityProviderInUsers.class)
      .add(1804, "Add rules.plugin_key", AddPluginKeyToRules.class)
      .add(1805, "Create table plugins", CreateTablePlugins.class)
      .add(1806, "Create table project_branches", CreateTableProjectBranches.class)
      .add(1807, "Add on project_branches key", AddIndexOnProjectBranchesKey.class)
      .add(1808, "Add branch column to projects table", AddBranchColumnToProjectsTable.class)
      .add(1809, "Populate project_branches with existing main branches", PopulateMainProjectBranches.class)
      .add(1810, "Add ce_activity.error_type", AddErrorTypeColumnToCeActivityTable.class)
      .add(1811, "Create table qprofile_edit_users", CreateTableQProfileEditUsers.class)
      .add(1812, "Create table qprofile_edit_groups", CreateTableQProfileEditGroups.class)
      .add(1814, "Purge table properties", PurgeTableProperties.class)
    ;
  }
}
