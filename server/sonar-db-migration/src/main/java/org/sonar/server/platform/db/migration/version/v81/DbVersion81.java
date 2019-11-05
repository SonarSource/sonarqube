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
package org.sonar.server.platform.db.migration.version.v81;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion81 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(3100, "Create ALM_SETTINGS table", CreateAlmSettingsTable.class)
      .add(3101, "Create PROJECT_ALM_SETTINGS table", CreateProjectAlmSettingsTable.class)
      .add(3102, "Migrate property 'sonar.alm.github.app.privateKey.secured' to 'sonar.alm.github.app.privateKeyContent.secured'",
        MigrateDeprecatedGithubPrivateKeyToNewKey.class)
      .add(3103, "Migrate GitHub ALM settings from PROPERTIES to ALM_SETTINGS tables", MigrateGithubAlmSettings.class)
      .add(3104, "Migrate Bitbucket ALM settings from PROPERTIES to ALM_SETTINGS tables", MigrateBitbucketAlmSettings.class)
      .add(3105, "Migrate Azure ALM settings from PROPERTIES to ALM_SETTINGS tables", MigrateAzureAlmSettings.class)
      .add(3106, "Delete 'sonar.pullrequest.provider' property", DeleteSonarPullRequestProviderProperty.class)
      .add(3107, "Migrate default branches to keep global setting", MigrateDefaultBranchesToKeepSetting.class)
      .add(3108, "Add EXCLUDE_FROM_PURGE column", AddExcludeBranchFromPurgeColumn.class)
      .add(3109, "Populate EXCLUDE_FROM_PURGE column", PopulateExcludeBranchFromPurgeColumn.class)
      .add(3110, "Remove 'sonar.branch.longLivedBranches.regex'", RemoveLLBRegexSetting.class)
      .add(3111, "Rename 'sonar.dbcleaner.daysBeforeDeletingInactiveShortLivingBranches' setting",
        RenameDaysBeforeDeletingInactiveSLBSetting.class)
      .add(3112, "Migrate short and long living branches types to common BRANCH type", MigrateSlbsAndLlbsToCommonType.class)
      .add(3113, "Migrate short and long living branches types to common BRANCH type in ce tasks table",
        MigrateSlbsAndLlbsToCommonTypeInCeTasks.class);
  }
}
