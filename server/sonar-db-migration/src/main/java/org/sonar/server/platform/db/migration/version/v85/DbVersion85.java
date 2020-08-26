/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v85;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion85 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(4000, "Delete 'project_alm_settings' orphans", DeleteProjectAlmSettingsOrphans.class)
      .add(4001, "Drop 'period', 'value_warning' columns from 'quality_gates_conditions' table", DropPeriodAndValueWarningColumnsFromQualityGateConditionsTable.class)
      .add(4002, "Drop 'project_alm_bindings' table", DropProjectAlmBindings.class)
      .add(4003, "Drop unused variation values columns in 'project_measures' table", DropUnusedVariationsInProjectMeasures.class)
      .add(4004, "Drop unused periods in 'snapshots' table", DropUnusedPeriodsInSnapshots.class)
      .add(4005, "Drop orphan favorites from 'properties' table", DropOrphanFavoritesFromProperties.class)

    ;
  }
}
