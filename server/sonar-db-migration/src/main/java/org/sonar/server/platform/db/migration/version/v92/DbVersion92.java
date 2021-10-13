/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v92;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion92 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6101, "Change size of column 'selection_expression' in 'Portfolios'", AlterSelectionExpressionInPortfoliosTable.class)
      .add(6102, "Migrate Bitbucket.org authentication plugin settings to built-in authentication settings", MigrateBibucketOrgPluginSettingsToBuiltInSettings.class)
      .add(6103, "Create column quick_fix_available in 'issues'", AddQuickFixAvailableColumnInIssuesTable.class)
      .add(6104, "Create qgate_user_permissions Table", CreateQGateUserPermissionsTable.class)
      .add(6105, "Create qgate_group_permissions Table", CreateQGateGroupPermissionsTable.class);
  }
}
