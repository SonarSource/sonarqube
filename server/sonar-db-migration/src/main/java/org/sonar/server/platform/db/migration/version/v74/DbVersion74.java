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
package org.sonar.server.platform.db.migration.version.v74;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion74 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2300, "Populate null values of IS_EXTERNAL in RULES", PopulateNullValuesOfIsExternalOnRules.class)
      .add(2301, "Add IS_ADHOC column to RULES table", AddIsAdHocToRules.class)
      .add(2302, "Populate IS_AD_HOC in RULES", PopulateIsAdHocOnRules.class)
      .add(2303, "Set IS_EXTERNAL and IS_AD_HOC not nullable in RULES", SetIsExternalAndIsAdHocNotNullableInRules.class)
    ;
  }
}
