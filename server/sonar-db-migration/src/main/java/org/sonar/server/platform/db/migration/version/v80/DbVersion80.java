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
package org.sonar.server.platform.db.migration.version.v80;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion80 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(3000, "Set Organizations#guarded column nullable", MakeOrganizationsGuardedNullable.class)
      .add(3001, "Create ProjectQualityGates table", CreateProjectQualityGatesTable.class)
      .add(3002, "Make index on DEPRECATED_RULE_KEYS.RULE_ID non unique", MakeDeprecatedRuleKeysRuleIdIndexNonUnique.class)
      .add(3003, "Populate ProjectQualityGate table from Properties table", PopulateProjectQualityGatesTable.class)
      .add(3004, "Rename ANALYSIS_PROPERTIES.SNAPSHOT_UUID to ANALYSIS_UUID", RenameAnalysisPropertiesSnapshotUuid.class)
      .add(3005, "Remove default quality gate property from Properties table", RemoveDefaultQualityGateFromPropertiesTable.class)
    ;
  }
}
