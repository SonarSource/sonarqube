/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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

package org.sonar.server.platform.db.migration.version.v64;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion64 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1600, "Add PROJECTS.TAGS", AddTagsToProjects.class)
      .add(1601, "Set PROJECTS.COPY_COMPONENT_UUID on local views", SetCopyComponentUuidOnLocalViews.class)
      .add(1602, "Add RULES_PROFILES.ORGANIZATION_UUID", AddQualityProfileOrganizationUuid.class)
      .add(1603, "Set RULES_PROFILES.ORGANIZATION_UUID to default", SetQualityProfileOrganizationUuidToDefault.class)
      .add(1604, "Make RULES_PROFILES.ORGANIZATION_UUID not nullable", MakeQualityProfileOrganizationUuidNotNullable.class)
      .add(1605, "Drop unique index on RULES_PROFILES.KEE", DropUniqueIndexOnQualityProfileKey.class)
      .add(1606, "Make RULES_PROFILES.ORGANIZATION_UUID and KEE unique", MakeQualityProfileOrganizationUuidAndKeyUnique.class)
      .add(1607, "Create ORGANIZATION_MEMBERS table", CreateOrganizationMembersTable.class)
      .add(1608, "Populate ORGANIZATION_MEMBERS table", PopulateOrganizationMembersTable.class)
      .add(1609, "Drop unique index on RULES_PROFILES.ORGANIZATION_UUID and KEE", DropUniqueIndexOnQualityProfileOrganizationUuidAndKey.class)
      .add(1610, "Make RULES_PROFILES.KEE unique", MakeQualityProfileKeyUnique.class)
      .add(1611, "Clean LOADED_TEMPLATES rows without type", CleanLoadedTemplateOrphans.class)
      .add(1612, "Extend size of column LOADED_TEMPLATES.TEMPLATE_TYPE", ExtendLoadedTemplateTypeColumn.class)
      .add(1613, "Add index LOADED_TEMPLATES_TYPE", AddIndexLoadedTemplatesType.class)
      .add(1614, "Upgrade loaded template entries for quality profiles", UpgradeQualityTemplateLoadedTemplates.class)
      .add(1615, "Create table RULES_METADATA", CreateRulesMetadata.class)
      .add(1616, "Populate table RULES_METADATA", PopulateRulesMetadata.class)
      .add(1617, "Drop metadata columns from RULES", DropMetadataColumnsFromRules.class)
      .add(1618, "Restore 'sonar-users' group", RestoreSonarUsersGroups.class)
      .add(1619, "Delete 'sonar.defaultGroup' setting", DeleteDefaultGroupSetting.class)
      .add(1620, "Set all users into 'sonar-users' group", SetAllUsersIntoSonarUsersGroup.class)
      .add(1621, "Create 'members' group in each organization", CreateMembersGroupsInEachOrganization.class)
      .add(1622, "Set organization members into 'members' group", SetOrganizationMembersIntoMembersGroup.class)
      .add(1623, "Add ORGANIZATIONS.DEFAULT_GROUP_ID", AddDefaultGroupIdToOrganizations.class)
      .add(1624, "Populate column ORGANIZATIONS.DEFAULT_GROUP_ID", PopulateColumnDefaultGroupIdOfOrganizations.class)
    ;
  }
}
