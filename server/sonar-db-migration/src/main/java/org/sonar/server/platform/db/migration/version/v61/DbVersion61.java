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
package org.sonar.server.platform.db.migration.version.v61;

import java.util.stream.Stream;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion61 implements DbVersion {
  @Override
  public Stream<Object> getSupportComponents() {
    return Stream.of(
      // Migration1304
      ShrinkModuleUuidPathOfProjects.class,
      AddBUuidPathToProjects.class);
  }

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1300, "Delete project dashboards and widgets", DeleteProjectDashboards.class)
      .add(1301, "Drop column DASHBOARDS.IS_GLOBAL", DropIsGlobalFromDashboards.class)
      .add(1302, "Create table CE_TASK_INPUT", CreateTableCeTaskInput.class)
      .add(1303, "Clear CE_QUEUE content", DeleteReportsFromCeQueue.class)
      .add(1304, "Shrink column PROJECTS.MODULE_UUID_PATH", Migration1304.class)
      .add(1307, "Add columns CE_ACTIVITY.ERROR_*", AddErrorColumnsToCeActivity.class)
      .add(1309, "Create table CE_SCANNER_CONTEXT", CreateTableScannerContext.class)
      .add(1310, "Create table INTERNAL_PROPERTIES", CreateTableInternalProperties.class)
      .add(1311, "Move views config from PROPERTIES to INTERNAL_PROPERTIES", RemoveViewsDefinitionFromProperties.class)
      .add(1312, "Create table PROPERTIES2", CreateTableProperties2.class)
      .add(1313, "Populate table PROPERTIES2", PopulateTableProperties2.class)
      .add(1314, "Drop table PROPERTIES", DropTableProperties.class)
      .add(1315, "Rename table PROPERTIES2 to PROPERTIES", RenameTableProperties2ToProperties.class)
      .add(1316, "Create table QPROFILE_CHANGES", CreateTableQprofileChanges.class)
      .add(1317, "Populate table QPROFILE_CHANGES", CopyActivitiesToQprofileChanges.class)
      .add(1318, "Drop table ACTIVITIES", DropTableActivities.class)
      .add(1319, "Create table RULE_REPOSITORIES", CreateTableRuleRepositories.class);
  }
}
