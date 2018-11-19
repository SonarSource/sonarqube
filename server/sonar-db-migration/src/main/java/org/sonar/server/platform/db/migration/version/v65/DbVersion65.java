/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v65;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion65 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1700, "Drop table AUTHORS", DropTableAuthors.class)
      .add(1701, "Clean orphans from USER_ROLES", CleanOrphanRowsInUserRoles.class)
      .add(1702, "Clean orphans from GROUP_ROLES", CleanOrphanRowsInGroupRoles.class)
      .add(1703, "Populate EVENTS.COMPONENT_UUID", PopulateEventsComponentUuid.class)
      .add(1704, "Drop index EVENTS_COMPONENT_UUID", DropIndexEventsComponentUuid.class)
      .add(1705, "Make EVENTS.COMPONENT_UUID not nullable", MakeEventsComponentUuidNotNullable.class)
      .add(1706, "Recreate index EVENTS_COMPONENT_UUID", RecreateIndexEventsComponentUuid.class)
      .add(1707, "Ensure ISSUE.PROJECT_UUID is consistent", EnsureIssueProjectUuidConsistencyOnIssues.class)
      .add(1708, "Clean orphans from PROJECT_LINKS", CleanOrphanRowsInProjectLinks.class)
      .add(1709, "Clean orphans from SETTINGS", CleanOrphanRowsInProperties.class)
      .add(1710, "Clean orphans from MANUAL_MEASURES", CleanOrphanRowsInManualMeasures.class)
      .add(1711, "Drop index MANUAL_MEASURES.COMPONENT_UUID", DropIndexManualMeasuresComponentUuid.class)
      .add(1712, "Make MANUAL_MEASURES.COMPONENT_UUID not nullable", MakeManualMeasuresComponentUuidNotNullable.class)
      .add(1713, "Recreate index MANUAL_MEASURES.COMPONENT_UUID", RecreateIndexManualMeasuresComponentUuid.class)
      .add(1714, "Purge developer data", PurgeDeveloperData.class)
      .add(1715, "Add rules_profiles.is_built_in", AddBuiltInFlagToRulesProfiles.class)
      .add(1716, "Set rules_profiles.is_built_in to false", SetRulesProfilesIsBuiltInToFalse.class)
      .add(1717, "Make rules_profiles.is_built_in not null", MakeRulesProfilesIsBuiltInNotNullable.class)
      .add(1718, "Delete unused loaded_templates on quality profiles", DeleteLoadedTemplatesOnQProfiles.class)
      .add(1719, "Create table default_qprofiles", CreateTableDefaultQProfiles.class)
      .add(1720, "Populate table default_qprofiles", PopulateTableDefaultQProfiles.class)
      .add(1721, "Drop rules_profiles.is_default", DropIsDefaultColumnFromRulesProfiles.class)
      .add(1722, "Create table qprofiles", CreateTableOrgQProfiles.class)
      .add(1723, "Populate table qprofiles", PopulateOrgQProfiles.class)
      .add(1724, "Drop columns organization_uuid and parent_kee from rules_profiles", DropOrgColumnsFromRulesProfiles.class)
      .add(1725, "Mark rules_profiles.is_built_in to true for default organization", SetRulesProfilesIsBuiltInToTrueForDefaultOrganization.class)
      .add(1726, "Update org_qprofiles to reference built-in profiles", UpdateOrgQProfilesToPointToBuiltInProfiles.class)
      .add(1727, "Delete rules_profiles orphans", DeleteOrphansFromRulesProfiles.class)
      .add(1728, "Rename column qprofile_changes.qprofile_key to qprofile_changes.rules_profile_uuid", RenameQProfileKeyToRulesProfileUuidOnQProfileChanges.class)
      .add(1729, "Add index on qprofile_changes.rules_profile_uuid", AddIndexRulesProfileUuidOnQProfileChanges.class)
      .add(1730, "Add USERS.ONBOARDED", AddUsersOnboarded.class)
      .add(1731, "Populate USERS.ONBOARDED", PopulateUsersOnboarded.class)
      .add(1732, "Make USERS.ONBOARDED not nullable", MakeUsersOnboardedNotNullable.class)
      .add(1733, "Create table es_queue", CreateEsQueueTable.class)
      .add(1734, "Add index on es_queue.created_at", AddIndexOnEsQueueCreatedAt.class)
      .add(1735, "Delete sonar.ce.workerCount setting", DeleteCeWorkerCountSetting.class)
    ;
  }
}
