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
package org.sonar.server.platform.db.migration.version.v91;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion91 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6001, "Drop 'manual_measures_component_uuid' index", DropManualMeasuresComponentUuidIndex.class)
      .add(6002, "Drop 'manual_measures' table", DropManualMeasuresTable.class)
      .add(6003, "Drop custom metrics data from 'live_measures' table", DropCustomMetricsLiveMeasuresData.class)
      .add(6004, "Drop custom metrics data from 'project_measures' table", DropCustomMetricsProjectMeasuresData.class)
      .add(6005, "Drop custom metrics data from 'metrics' table", DropUserManagedMetricsData.class)
      .add(6006, "Drop 'user_managed' column from 'metrics' table", DropUserManagedColumnFromMetricsTable.class)
      .add(6007, "Create Audit table", CreateAuditTable.class)
      .add(6008, "Add column 'removed' to 'plugins' table", AddColumnRemovedToPlugins.class)
      .add(6009, "Alter column 'client_secret' of 'alm_settings' table to length 160", AlterClientSecretColumnLengthOfAlmSettingsTable.class)
      .add(6010, "Alter column 'private_key' of 'alm_settings' table to length 2500", AlterPrivateKeyColumnLengthOfAlmSettingsTable.class);
  }
}
