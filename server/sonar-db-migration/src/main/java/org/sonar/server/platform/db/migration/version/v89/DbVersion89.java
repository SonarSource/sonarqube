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
package org.sonar.server.platform.db.migration.version.v89;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion89 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(4400, "Add indices on columns 'type' and 'value' to 'new_code_periods' table", AddIndicesToNewCodePeriodTable.class)
      .add(4402, "Add Index on column 'main_branch_project_uuid' to 'components' table", AddMainBranchProjectUuidIndexToComponentTable.class)
      .add(4403, "Drop Github endpoint on project level setting", DropGithubEndpointOnProjectLevelSetting.class)
      .add(4404, "Increase size of 'value' column in 'new_code_periods' table ", IncreaseSizeOfValueColumnInNewCodePeriodsTable.class)
      .add(4405, "Add index on column 'webhook_uuid' to 'webhook_deliveries' table", AddIndexOnWebhookUuidInWebhookDeliveriesTable.class);
  }
}
