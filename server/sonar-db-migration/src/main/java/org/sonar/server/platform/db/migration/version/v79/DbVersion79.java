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
package org.sonar.server.platform.db.migration.version.v79;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion79 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2800, "Truncate environment variables and system properties from existing scanner reports",
        TruncateEnvAndSystemVarsFromScannerContext.class)
      .add(2801, "populate install version and install date internal properties", PopulateInstallDateAndVersion.class)
      .add(2802, "Migrate property 'sonar.pullrequest.provider' value from VSTS to Azure DevOps", MigrateVstsProviderToAzureDevOps.class)
      .add(2803, "Remove quality gate conditions on Security Review Rating", RemoveQGConditionsOnSecurityReviewRating.class)
      .add(2804, "Reindex issues and rules to take into account latest categories definition", ReindexIssuesAndRules.class);
  }
}
