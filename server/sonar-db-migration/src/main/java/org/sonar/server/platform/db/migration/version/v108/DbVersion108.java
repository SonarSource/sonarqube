/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v108;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion108 implements DbVersion {

  /**
   * We use the start of the 10.X cycle as an opportunity to align migration numbers with the SQ version number.
   * Please follow this pattern:
   * 10_0_000
   * 10_0_001
   * 10_0_002
   * 10_1_000
   * 10_1_001
   * 10_1_002
   * 10_2_000
   */
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(10_8_000, "Create 'measures' table", CreateMeasuresTable.class)
      .add(10_8_001, "Add 'measures_migrated' column on 'project_branches' table", AddMeasuresMigratedColumnToProjectBranchesTable.class)
      .add(10_8_002, "Create index on 'project_branches.measures_migrated'", CreateIndexOnProjectBranchesMeasuresMigrated.class)
      .add(10_8_003, "Migrate the content of 'live_measures' to 'measures' for branches", MigrateBranchesLiveMeasuresToMeasures.class)
      .add(10_8_004, "Add 'measures_migrated' column on 'portfolios' table", AddMeasuresMigratedColumnToPortfoliosTable.class)
      .add(10_8_005, "Create index on 'portfolios.measures_migrated'", CreateIndexOnPortfoliosMeasuresMigrated.class)
      .add(10_8_006, "Migrate the content of 'live_measures' to 'measures' for portfolios", MigratePortfoliosLiveMeasuresToMeasures.class)
      .add(10_8_007, "Create primary key on 'measures' table", CreatePrimaryKeyOnMeasuresTable.class)
      .add(10_8_008, "Create index on column 'branch_uuid' in 'measures' table", CreateIndexOnMeasuresTable.class)
      .add(10_8_009, "Drop column 'from_hotspot' in the 'issues' table", DropColumnFromHotspotInIssues.class)
      .add(10_8_010, "Drop 'live_measures' table", DropLiveMeasuresTable.class)
      .add(10_8_011, "Drop index on 'portfolios.measures_migrated'", DropIndexOnPortfoliosMeasuresMigrated.class)
      .add(10_8_012, "Drop 'measures_migrated' column on 'portfolios' table", DropMeasuresMigratedColumnInPortfoliosTable.class)
      .add(10_8_013, "Drop index on 'project_branches.measures_migrated'", DropIndexOnProjectBranchesMeasuresMigrated.class)
      .add(10_8_014, "Drop 'measures_migrated' column on 'project_branches' table", DropMeasuresMigratedColumnInProjectBranchesTable.class)
      .add(10_8_015, "Add column 'impacts' in 'active_rules' table", AddImpactsColumnInActiveRulesTable.class)
    ;
  }

}
