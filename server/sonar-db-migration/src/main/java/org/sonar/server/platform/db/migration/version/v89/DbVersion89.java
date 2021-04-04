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
package org.sonar.server.platform.db.migration.version.v89;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion89 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry.add(4400, "Add column 'minimum_effective_lines' to 'change_set_condition", AddMinimumEffectiveLinesColumnToQualityGateConditionsTable.class)
      .add(4401, "Set default values in 'minimum_effective_lines' to 'change_set_condition", PopulateQualityGateConditionsMinimumEffectiveLinesDefaultValue.class)
      .add(4402, "Make column 'minimum_effective_lines' in 'change_set_condition not nullable", MakeMinimumEffectiveLinesColumnInQualityGateConditionsTableNonNullable.class);
  }
}
