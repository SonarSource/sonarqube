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
package org.sonar.server.platform.db.migration.version.v105;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

// ignoring bad number formatting, as it's indented that we align the migration numbers to SQ versions
@SuppressWarnings("java:S3937")
public class DbVersion105 implements DbVersion {

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
      .add(10_5_000, "Drop constraint on 'uuid' from 'issues_impacts' table", DropPrimaryKeyConstraintInIssuesImpactsTable.class)
      .add(10_5_001, "Drop constraint on 'uuid' from 'rules_default_impacts' table", DropPrimaryKeyConstraintInRulesDefaultImpactsTable.class)
      .add(10_5_002, "Drop column 'uuid' from 'issues_impacts' table", DropUuidColumnInIssuesImpactsTable.class)
      .add(10_5_003, "Drop column 'uuid' from 'rules_default_impacts' table", DropUuidColumnInRulesDefaultImpactsTable.class)
      .add(10_5_004, "Create primary key on 'issues_impacts' table", CreatePrimaryKeyOnIssuesImpactsTable.class)
      .add(10_5_005, "Create primary key on 'rules_default_impacts' table", CreatePrimaryKeyOnRulesDefaultImpactsTable.class)
      .add(10_5_006, "Delete 'languageSpecificParameters' property set from 'properties' table", DeleteLanguageSpecificParametersPropertySet.class);
  }
}
