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
package org.sonar.server.platform.db.migration.version.v71;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion71 implements DbVersion {

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(2000, "Delete settings defined in sonar.properties from PROPERTIES table", DeleteSettingsDefinedInSonarDotProperties.class)
      .add(2001, "Add scope to rules", AddRuleScope.class)
      .add(2002, "Set rules scope to MAIN", SetRuleScopeToMain.class)
      .add(2003, "Make scope not nullable in rules", MakeScopeNotNullableInRules.class)
      .add(2004, "Use rule id in QPROFILE_CHANGES", UseRuleIdInQPChangesData.class)
      .add(2005, "Create table DEPRECATED_RULE_KEYS", CreateDeprecatedRuleKeysTable.class)
      .add(2006, "Clean orphans in Compute Engine child tables", CleanCeChildTablesOrphans.class)
      .add(2007, "Update PERMISSION_TEMPLATES.KEYS ", UpdatePermissionTooLongTemplateKeys.class)
      .add(2008, "Make scope not nullable in rules", MakeScopeNotNullableInRules.class)
      .add(2009, "Create table PROJECT_LINKS2", CreateTableProjectLinks2.class)
      .add(2010, "Populate table PROJECT_LINKS2", PopulateTableProjectLinks2.class)
      .add(2011, "Drop table PROJECT_LINKS", DropTableProjectLinks.class)
      .add(2012, "Rename table PROJECT_LINKS2 to PROJECT_LINKS", RenameTableProjectLinks2ToProjectLinks.class)
      .add(2013, "Create WEBHOOKS Table", CreateWebhooksTable.class)
      .add(2014, "Migrate webhooks from SETTINGS table to WEBHOOKS table", MigrateWebhooksToWebhooksTable.class)
      .add(2015, "Add webhook key to WEBHOOK_DELIVERIES table", AddWebhookKeyToWebhookDeliveriesTable.class)
      .add(2016, "Increase branch type size in PROJECT_BRANCHES", IncreaseBranchTypeSizeForPullRequest.class)
      .add(2017, "Add key_type column in PROJECT_BRANCHES", AddKeyTypeInProjectBranches.class)
      .add(2018, "Fill key_type column in PROJECT_BRANCHES", SetKeyTypeToBranchInProjectBranches.class)
      .add(2019, "Make key_type not nullable in PROJECT_BRANCHES", MakeKeyTypeNotNullableInProjectBranches.class)
      .add(2020, "Replace index in PROJECT_BRANCHES", ReplaceIndexInProjectBranches.class)
      .add(2021, "Add pull_request_data in PROJECT_BRANCHES", AddPullRequestBinaryInProjectBranches.class)
      .add(2022, "Clean broken project to QG references", CleanBrokenProjectToQGReferences.class)
      .add(2023, "Delete measures of project copies", DeleteMeasuresOfProjectCopies.class)
    ;
  }
}
