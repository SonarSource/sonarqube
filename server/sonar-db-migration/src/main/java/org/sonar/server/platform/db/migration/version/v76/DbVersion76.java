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
package org.sonar.server.platform.db.migration.version.v76;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion76 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2500, "Create table USER_PROPERTIES", CreateUserPropertiesTable.class)
      .add(2501, "Add index in table USER_PROPERTIES", AddUniqueIndexInUserPropertiesTable.class)
      .add(2502, "Archive module properties in a new project level property", MigrateModuleProperties.class)
      .add(2503, "Delete useless 'sonar.dbcleaner.cleanDirectory' property", DeleteUselessProperty.class)
      .add(2504, "Delete useless module and folder level measures", DeleteModuleAndFolderMeasures.class)
      .add(2505, "Fix the direction values of certain metrics (prepare for migration of conditions)", FixDirectionOfMetrics.class)
      .add(2506, "Migrate quality gate conditions using warning, period and no more supported operations", MigrateNoMoreUsedQualityGateConditions.class)
      .add(2507, "Delete sonar.onboardingTutorial.showToNewUsers from settings", DeleteUselessOnboardingSetting.class)
      .add(2508, "Add unique index on EXTERNAL_LOGIN and EXTERNAL_IDENTITY_PROVIDER columns in table USERS", AddUniqueIndexOnExternalLoginInUsersTable.class)
    ;
  }
}
