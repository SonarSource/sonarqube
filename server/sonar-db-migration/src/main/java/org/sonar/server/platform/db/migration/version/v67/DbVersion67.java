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
package org.sonar.server.platform.db.migration.version.v67;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion67 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1830, "Copy deprecated server ID", CopyDeprecatedServerId.class)
      .add(1831, "Add webhook_deliveries.analysis_uuid", AddAnalysisUuidToWebhookDeliveries.class)
      .add(1832, "Create table ANALYSIS_PROPERTIES", CreateTableAnalysisProperties.class)
      .add(1833, "Cleanup disabled users", CleanupDisabledUsers.class)
      .add(1834, "Set WEBHOOK_DELIVERIES.CE_TASK_UUID as nullable", UpdateCeTaskUuidColumnToNullableOnWebhookDeliveries.class)
      .add(1835, "Populate WEBHOOK_DELIVERIES.ANALYSIS_UUID", PopulateAnalysisUuidColumnOnWebhookDeliveries.class)
      .add(1836, "Migrate 'previous_analysis' leak periods to 'previous_version'", MigratePreviousAnalysisToPreviousVersion.class)
      .add(1837, "Drop old licenses", DropOldLicenses.class)
    ;
  }
}
