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
      .add(2304, "Add ad hoc related columns in RULES_METADATA", AddAdHocColumnsInInRulesMetadata.class)
      .add(2305, "Add CE_QUEUE.MAIN_COMPONENT_UUID 1/5", AddTmpColumnsToCeQueue.class)
      .add(2306, "Add CE_ACTIVITY.MAIN_COMPONENT_UUID 1/5", AddTmpColumnsToCeActivity.class)
      .add(2307, "Populate CE_QUEUE.MAIN_COMPONENT_UUID 2/5", PopulateTmpColumnsToCeQueue.class)
      .add(2308, "Populate CE_ACTIVITY.MAIN_COMPONENT_UUID 2/5", PopulateTmpColumnsToCeActivity.class)
      .add(2309, "Add CE_ACTIVITY.MAIN_LAST_KEY 1/6", AddTmpLastKeyColumnsToCeActivity.class)
      .add(2310, "Populate CE_ACTIVITY.MAIN_LAST_KEY 2/6", PopulateTmpLastKeyColumnsToCeActivity.class)
      .add(2311, "Populate CE_ACTIVITY.MAIN_LAST_KEY 3/6", MakeCeActivityLastKeyColumnsNullable.class)
      .add(2312, "Add CE_QUEUE.MAIN_COMPONENT_UUID 3/5", AddMainComponentUuidColumnsToCeQueue.class)
      .add(2313, "Add CE_ACTIVITY.MAIN_COMPONENT_UUID 3/5", AddMainComponentUuidColumnsToCeActivity.class)
      .add(2314, "Add CE_ACTIVITY.MAIN_LAST_KEY 3/6", AddLastKeyColumnsToCeActivity.class)
      .add(2315, "Populate CE_QUEUE.MAIN_COMPONENT_UUID 4/5", PopulateMainComponentUuidColumnsToCeQueue.class)
      .add(2316, "Populate CE_ACTIVITY.MAIN_COMPONENT_UUID 4/5", PopulateMainComponentUuidColumnsToCeActivity.class)
      .add(2317, "Populate CE_ACTIVITY.MAIN_LAST_KEY 4/6", PopulateLastKeyColumnsToCeActivity.class)
    ;
  }
}
