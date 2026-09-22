/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.platform.db.migration.version.v202606;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion202606 implements DbVersion {

  @Override
  @SuppressWarnings("java:S3937")
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2026_06_000, "Remove duplicate permission templates", RemoveDuplicatePermissionTemplates.class)
      .add(2026_06_001, "Enforce unique permission template names", EnforceUniquePermissionTemplateNames.class)
      .add(2026_06_002, "Add 'type' to 'sca_analyses'", AddTypeToScaAnalyses.class)
      .add(2026_06_003, "Create table 'sca_container_analyses'", CreateScaContainerAnalysesTable.class);
  }
}
