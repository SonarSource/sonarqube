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
package org.sonar.server.platform.db.migration.version.v93;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion93 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6201, "Fix usage of deprecated column in MSSQL", FixUsageOfDeprecatedColumnsMsSQL.class)
      .add(6202, "Drop index 'uniq_portfolio_references'", DropUniqPortfolioReferencesIndex.class)
      .add(6203, "Add column 'branch_uuid' to 'portfolio_references'", AddBranchToPortfolioReferences.class)
      .add(6204, "Create index 'uniq_portfolio_references'", CreateIndexForPortfolioReferences.class)
      .add(6205, "Create table 'new_code_reference_branch_issues'", CreateNewCodeReferenceBranchIssues.class)
      .add(6206, "Create index 'uniq_new_code_reference_issues'", CreateIndexForNewCodeReferenceBranchIssues.class)

    ;
  }
}
