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
package org.sonar.server.platform.db.migration.version.v94;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion94 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(6301, "Drop unused Issues Column REPORTER", DropReporterIssueColumn.class)
      .add(6302, "Drop unused Issues Column ACTION_PLAN_KEY", DropActionPlanKeyIssueColumn.class)
      .add(6303, "Drop unused Issues Column ISSUE_ATTRIBUTES", DropIssuesAttributesIssueColumn.class)
      .add(6304, "Create table 'SCANNER_ANALYSIS_CACHE", CreateScannerAnalysisCacheTable.class)
      .add(6305, "Issue warning for users using SHA1 hash method", SelectUsersWithSha1HashMethod.class)
      .add(6306, "Drop table 'user_properties'", DropUserPropertiesTable.class)
    ;
  }
}
