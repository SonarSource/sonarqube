/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v202501;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion202501 implements DbVersion {

  /**
   * We use the start of the 10.X cycle as an opportunity to align migration numbers with the SQ version number.
   * Please follow this pattern:
   * 2025_01_000
   * 2025_01_001
   * 2025_01_002
   */
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2025_01_000, "Rename 'sonar.portfolios.confidential.header' property to 'sonar.pdf.confidential.header.enabled'",
        MigrateConfidentialHeaderProperty.class)
      .add(2025_01_001, "Delete removed complexity measures from 'project_measures' table", DeleteRemovedComplexityMeasuresFromProjectMeasures.class)
      .add(2025_01_002, "Delete removed complexity metrics from 'metrics' table", DeleteRemovedComplexityMetrics.class)
      .add(2025_01_003, "Create index on 'rule_tags' table", CreateIndexOnRuleTagsTable.class)
      .add(2025_01_004, "Rename 'Sonar Way' without 0 new issues condition to 'Sonar Way(legacy)'", RenameOldSonarWayToLegacy.class)
      .add(2025_01_005, "Create 'user_ai_tool_usages' table", CreateUserAIToolUsagesTable.class)
      .add(2025_01_006, "Add 'detected_ai_code' column to 'projects' table", AddDetectedAICodeColumnToProjectsTable.class)
      .add(2025_01_007, "Create table 'migration_logs'", CreateMigrationLogsTable.class)
      .add(2025_01_008, "Log message if SAML configuration is not valid", LogMessageIfInvalidSamlSetup.class)
      .add(2025_01_009, "Add 'inline_annotations_enabled' column to 'project_alm_settings' table", AddInlineAnnotationsEnabledColumnToProjectAlmSettingsTable.class)
      .add(2025_01_010, "Populate 'inline_annotations_enabled' column for Azure", PopulateInlineAnnotationsEnabledColumnForAzure.class)
      .add(2025_01_011, "Log message if users are still relying on BCRYPT hashed passwords", LogMessageIfInvalidHashMechanismUsed.class)
    ;
  }
}
