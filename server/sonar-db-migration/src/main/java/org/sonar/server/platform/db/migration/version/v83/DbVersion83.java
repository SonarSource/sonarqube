/*
 * SonarQube
 * Copyright (C) 2009-2020 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v83;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;
import org.sonar.server.platform.db.migration.version.v83.activeruleparameters.AddPrimaryKeyOnUuidColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activeruleparameters.AddUuidColumnToActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activeruleparameters.DropIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activeruleparameters.DropPrimaryKeyOnIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activeruleparameters.MakeActiveRuleParametersUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.activeruleparameters.PopulateActiveRuleParametersUuid;
import org.sonar.server.platform.db.migration.version.v83.activerules.AddActiveRuleUuidColumnToActiveRuleParameters;
import org.sonar.server.platform.db.migration.version.v83.activerules.AddIndexOnActiveRuleUuidOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.AddPrimaryKeyOnUuidColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.AddUuidColumnToActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.DropActiveRuleIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.DropIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.DropIndexOnActiveRuleIdOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.DropPrimaryKeyOnIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v83.activerules.MakeActiveRuleParametersActiveRuleUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v83.activerules.MakeActiveRulesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.activerules.PopulateActiveRuleParametersActiveRuleUuid;
import org.sonar.server.platform.db.migration.version.v83.activerules.PopulateActiveRulesUuid;
import org.sonar.server.platform.db.migration.version.v83.ceactivity.AddPrimaryKeyOnUuidColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v83.ceactivity.DropIdColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v83.ceactivity.DropPrimaryKeyOnIdColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v83.cequeue.AddPrimaryKeyOnUuidColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v83.cequeue.DropIdColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v83.cequeue.DropPrimaryKeyOnIdColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v83.cequeue.DropUniqueIndexOnUuidColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v83.duplicationsindex.AddPrimaryKeyOnUuidColumnOfDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v83.duplicationsindex.AddUuidToDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v83.duplicationsindex.DropIdColumnOfDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v83.duplicationsindex.DropPrimaryKeyOnIdColumnOfDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v83.duplicationsindex.MakeDuplicationsIndexUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.duplicationsindex.PopulateDuplicationsIndexUuid;
import org.sonar.server.platform.db.migration.version.v83.events.AddPrimaryKeyOnUuidColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v83.events.DropIdColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v83.events.DropPrimaryKeyOnIdColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v83.filesources.AddPrimaryKeyOnUuidColumnOfFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v83.filesources.AddUuidColumnToFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v83.filesources.DropIdColumnOfFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v83.filesources.DropPrimaryKeyOnIdColumnOfFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v83.filesources.MakeFileSourcesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.filesources.PopulateFileSourcesUuid;
import org.sonar.server.platform.db.migration.version.v83.grouproles.AddComponentUuidColumnToGroupRoles;
import org.sonar.server.platform.db.migration.version.v83.grouproles.AddPrimaryKeyOnUuidColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v83.grouproles.AddUuidColumnToGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v83.grouproles.DropIdColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v83.grouproles.DropPrimaryKeyOnIdColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v83.grouproles.DropResourceIdFromGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v83.grouproles.MakeGroupRolesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.grouproles.MigrateResourceIdToUuidInGroupRoles;
import org.sonar.server.platform.db.migration.version.v83.grouproles.PopulateGroupRolesUuid;
import org.sonar.server.platform.db.migration.version.v83.issuechanges.AddPrimaryKeyOnUuidColumnOfIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v83.issuechanges.AddUuidColumnToIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v83.issuechanges.DropIdColumnOfIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v83.issuechanges.DropPrimaryKeyOnIdColumnOfIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v83.issuechanges.MakeIssueChangesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.issuechanges.PopulateIssueChangesUuid;
import org.sonar.server.platform.db.migration.version.v83.issues.AddPrimaryKeyOnKeeColumnOfIssuesTable;
import org.sonar.server.platform.db.migration.version.v83.issues.DropIdColumnOfIssuesTable;
import org.sonar.server.platform.db.migration.version.v83.issues.DropPrimaryKeyOnIdColumnOfIssuesTable;
import org.sonar.server.platform.db.migration.version.v83.manualmeasures.AddPrimaryKeyOnUuidColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v83.manualmeasures.AddUuidColumnToManualMeasures;
import org.sonar.server.platform.db.migration.version.v83.manualmeasures.DropIdColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v83.manualmeasures.DropPrimaryKeyOnIdColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v83.manualmeasures.MakeManualMeasuresUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.manualmeasures.PopulateManualMeasureUuid;
import org.sonar.server.platform.db.migration.version.v83.notifications.AddPrimaryKeyOnUuidColumnOfNotificationTable;
import org.sonar.server.platform.db.migration.version.v83.notifications.AddUuidAndCreatedAtColumnsToNotification;
import org.sonar.server.platform.db.migration.version.v83.notifications.DropIdColumnOfNotificationTable;
import org.sonar.server.platform.db.migration.version.v83.notifications.DropPrimaryKeyOnIdColumnOfNotificationTable;
import org.sonar.server.platform.db.migration.version.v83.notifications.MakeNotificationUuidAndCreatedAtColumnsNotNullable;
import org.sonar.server.platform.db.migration.version.v83.notifications.PopulateNotificationUuidAndCreatedAt;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesgroups.AddPrimaryKeyOnUuidColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesgroups.AddUuidColumnToPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesgroups.DropIdColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesgroups.DropPrimaryKeyOnIdColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesgroups.MakePermTemplatesGroupsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesgroups.PopulatePermTemplatesGroupsUuid;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesusers.AddPrimaryKeyOnUuidColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesusers.AddUuidColumnToPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesusers.DropIdColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesusers.DropPrimaryKeyOnIdColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesusers.MakePermTemplatesUsersUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.permtemplatesusers.PopulatePermTemplatesUsersUuid;
import org.sonar.server.platform.db.migration.version.v83.permtplcharacteristics.AddPrimaryKeyOnUuidColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v83.permtplcharacteristics.AddUuidColumnToPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v83.permtplcharacteristics.DropIdColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v83.permtplcharacteristics.DropPrimaryKeyOnIdColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v83.permtplcharacteristics.MakePermTplCharacteristicsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.permtplcharacteristics.PopulatePermTplCharacteristicsUuid;
import org.sonar.server.platform.db.migration.version.v83.projectmeasures.AddPrimaryKeyOnUuidColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v83.projectmeasures.AddUuidColumnToProjectMeasures;
import org.sonar.server.platform.db.migration.version.v83.projectmeasures.DropIdColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v83.projectmeasures.DropPrimaryKeyOnIdColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v83.projectmeasures.MakeProjectMeasuresUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.projectmeasures.PopulateProjectMeasureUuid;
import org.sonar.server.platform.db.migration.version.v83.projectqprofiles.AddPrimaryKeyOnUuidColumnOfProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v83.projectqprofiles.AddUuidColumnToProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v83.projectqprofiles.DropIdColumnOfProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v83.projectqprofiles.DropPrimaryKeyOnIdColumnOfProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v83.projectqprofiles.MakeProjectQProfilesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.projectqprofiles.PopulateProjectQProfilesUuid;
import org.sonar.server.platform.db.migration.version.v83.properties.AddComponentUuidColumnToProperties;
import org.sonar.server.platform.db.migration.version.v83.properties.DropResourceIdFromPropertiesTable;
import org.sonar.server.platform.db.migration.version.v83.properties.MigrateResourceIdToUuidInProperties;
import org.sonar.server.platform.db.migration.version.v83.qualitygateconditions.AddPrimaryKeyOnUuidColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v83.qualitygateconditions.AddUuidColumnToQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v83.qualitygateconditions.DropIdColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v83.qualitygateconditions.DropPrimaryKeyOnIdColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v83.qualitygateconditions.MakeQualityGateConditionsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.qualitygateconditions.PopulateQualityGateConditionsUuid;
import org.sonar.server.platform.db.migration.version.v83.snapshots.issues.AddPrimaryKeyOnUuidColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v83.snapshots.issues.DropIdColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v83.snapshots.issues.DropPrimaryKeyOnIdColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v83.userroles.AddComponentUuidColumnToUserRoles;
import org.sonar.server.platform.db.migration.version.v83.userroles.AddPrimaryKeyOnUuidColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v83.userroles.AddUuidColumnToUserRolesTable;
import org.sonar.server.platform.db.migration.version.v83.userroles.DropIdColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v83.userroles.DropPrimaryKeyOnIdColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v83.userroles.DropResourceIdFromUserRolesTable;
import org.sonar.server.platform.db.migration.version.v83.userroles.MakeUserRolesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v83.userroles.MigrateResourceIdToUuidInUserRoles;
import org.sonar.server.platform.db.migration.version.v83.userroles.PopulateUserRolesUuid;
import org.sonar.server.platform.db.migration.version.v83.usertokens.AddPrimaryKeyOnUuidColumnOfUserTokensTable;
import org.sonar.server.platform.db.migration.version.v83.usertokens.AddUuidColumnToUserTokens;
import org.sonar.server.platform.db.migration.version.v83.usertokens.DropIdColumnOfUserTokensTable;
import org.sonar.server.platform.db.migration.version.v83.usertokens.DropPrimaryKeyOnIdColumnOfUserTokensTable;
import org.sonar.server.platform.db.migration.version.v83.usertokens.MakeUserTokensUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v83.usertokens.PopulateUserTokensUuid;

public class DbVersion83 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(3300, "Add 'summary_comment_enabled' boolean column to 'project_alm_settings'", AddSummaryEnabledColumnToAlmSettings.class)
      .add(3301, "Enable 'summary_comment_enabled' for GitHub based projects", PopulateSummaryCommentEnabledColumnForGitHub.class)
      .add(3302, "Add 'component_uuid' column to 'properties'", AddComponentUuidColumnToProperties.class)
      .add(3303, "Migrate 'resource_id' to 'component_uuid' in 'properties'", MigrateResourceIdToUuidInProperties.class)
      .add(3304, "Remove column 'resource_id' in 'properties'", DropResourceIdFromPropertiesTable.class)
      .add(3305, "Add 'component_uuid' column to 'group_roles'", AddComponentUuidColumnToGroupRoles.class)
      .add(3306, "Migrate 'resource_id' to 'component_uuid' in 'group_roles'", MigrateResourceIdToUuidInGroupRoles.class)
      .add(3307, "Remove column 'resource_id' in 'group_roles'", DropResourceIdFromGroupRolesTable.class)
      .add(3308, "Add 'component_uuid' column to 'user_roles'", AddComponentUuidColumnToUserRoles.class)
      .add(3309, "Migrate 'resource_id' to 'component_uuid' in 'user_roles'", MigrateResourceIdToUuidInUserRoles.class)
      .add(3310, "Remove column 'resource_id' in 'user_roles'", DropResourceIdFromUserRolesTable.class)
      .add(3311, "Remove column 'id' in 'components'", DropIdFromComponentsTable.class)

      // Migration on EVENTS table
      .add(3400, "Drop primary key on 'ID' column of 'EVENTS' table", DropPrimaryKeyOnIdColumnOfEventsTable.class)
      .add(3401, "Add primary key on 'UUID' column of 'EVENTS' table", AddPrimaryKeyOnUuidColumnOfEventsTable.class)
      .add(3402, "Drop column 'ID' of 'EVENTS' table", DropIdColumnOfEventsTable.class)

      // Migrations of NOTIFICATIONS table
      .add(3403, "Add 'uuid' and 'createdAt' columns for notifications", AddUuidAndCreatedAtColumnsToNotification.class)
      .add(3404, "Populate 'uuid' and 'createdAt columns for notifications", PopulateNotificationUuidAndCreatedAt.class)
      .add(3405, "Make 'uuid' and 'createdAt' column not nullable for notifications", MakeNotificationUuidAndCreatedAtColumnsNotNullable.class)
      .add(3406, "Drop primary key on 'ID' column of 'NOTIFICATIONS' table", DropPrimaryKeyOnIdColumnOfNotificationTable.class)
      .add(3407, "Add primary key on 'UUID' column of 'NOTIFICATIONS' table", AddPrimaryKeyOnUuidColumnOfNotificationTable.class)
      .add(3408, "Drop column 'ID' of 'NOTIFICATIONS' table", DropIdColumnOfNotificationTable.class)

      // Migration on ISSUES table
      .add(3409, "Drop primary key on 'ID' column of 'ISSUES' table", DropPrimaryKeyOnIdColumnOfIssuesTable.class)
      .add(3410, "Add primary key on 'KEE' column of 'ISSUES' table", AddPrimaryKeyOnKeeColumnOfIssuesTable.class)
      .add(3411, "Drop column 'ID' of 'ISSUES' table", DropIdColumnOfIssuesTable.class)

      // Migration on SNAPSHOTS table
      .add(3412, "Drop primary key on 'ID' column of 'SNAPSHOTS' table", DropPrimaryKeyOnIdColumnOfSnapshotsTable.class)
      .add(3413, "Add primary key on 'UUID' column of 'SNAPSHOTS' table", AddPrimaryKeyOnUuidColumnOfSnapshotsTable.class)
      .add(3414, "Drop column 'ID' of 'SNAPSHOTS' table", DropIdColumnOfSnapshotsTable.class)

      // Migration on CE_QUEUE table
      .add(3415, "Drop unique index on 'uuid' column of 'CE_QUEUE' table", DropUniqueIndexOnUuidColumnOfCeQueueTable.class)
      .add(3416, "Drop primary key on 'ID' column of 'CE_QUEUE' table", DropPrimaryKeyOnIdColumnOfCeQueueTable.class)
      .add(3417, "Add primary key on 'UUID' column of 'CE_QUEUE' table", AddPrimaryKeyOnUuidColumnOfCeQueueTable.class)
      .add(3418, "Drop column 'ID' of 'CE_QUEUE' table", DropIdColumnOfCeQueueTable.class)

      // Migration on CE_ACTIVITY table
      .add(3419, "Drop primary key on 'ID' column of 'CE_ACTIVITY' table", DropPrimaryKeyOnIdColumnOfCeActivityTable.class)
      .add(3420, "Add primary key on 'UUID' column of 'CE_ACTIVITY' table", AddPrimaryKeyOnUuidColumnOfCeActivityTable.class)
      .add(3421, "Drop column 'ID' of 'CE_ACTIVITY' table", DropIdColumnOfCeActivityTable.class)

      // Migration of DUPLICATIONS_INDEX table
      .add(3422, "Add 'uuid' columns for DUPLICATIONS_INDEX", AddUuidToDuplicationsIndexTable.class)
      .add(3423, "Populate 'uuid' columns for DUPLICATIONS_INDEX", PopulateDuplicationsIndexUuid.class)
      .add(3424, "Make 'uuid' column not nullable for DUPLICATIONS_INDEX", MakeDuplicationsIndexUuidColumnNotNullable.class)
      .add(3425, "Drop primary key on 'ID' column of 'DUPLICATIONS_INDEX' table", DropPrimaryKeyOnIdColumnOfDuplicationsIndexTable.class)
      .add(3426, "Add primary key on 'UUID' column of 'DUPLICATIONS_INDEX' table", AddPrimaryKeyOnUuidColumnOfDuplicationsIndexTable.class)
      .add(3427, "Drop column 'ID' of 'DUPLICATIONS_INDEX' table", DropIdColumnOfDuplicationsIndexTable.class)

      // Migration of ACTIVE_RULE_PARAMS table
      .add(3428, "Add 'uuid' column for 'ACTIVE_RULE_PARAMS' table", AddUuidColumnToActiveRuleParametersTable.class)
      .add(3429, "Populate 'uuid' column for 'ACTIVE_RULE_PARAMS' table", PopulateActiveRuleParametersUuid.class)
      .add(3430, "Make 'uuid' column not nullable for 'ACTIVE_RULE_PARAMS' table", MakeActiveRuleParametersUuidColumnNotNullable.class)
      .add(3431, "Drop primary key on 'ID' column of 'ACTIVE_RULE_PARAMS' table", DropPrimaryKeyOnIdColumnOfActiveRuleParametersTable.class)
      .add(3432, "Add primary key on 'UUID' column of 'ACTIVE_RULE_PARAMS' table", AddPrimaryKeyOnUuidColumnOfActiveRuleParametersTable.class)
      .add(3433, "Drop column 'ID' of 'ACTIVE_RULE_PARAMS' table", DropIdColumnOfActiveRuleParametersTable.class)

      // Migration on PROJECT_MEASURES table
      .add(3434, "Add 'uuid' columns for 'PROJECT_MEASURES'", AddUuidColumnToProjectMeasures.class)
      .add(3435, "Populate 'uuid' column for 'PROJECT_MEASURES'", PopulateProjectMeasureUuid.class)
      .add(3436, "Make 'uuid' column not nullable for 'PROJECT_MEASURES'", MakeProjectMeasuresUuidColumnNotNullable.class)
      .add(3437, "Drop primary key on 'ID' column of 'PROJECT_MEASURES' table", DropPrimaryKeyOnIdColumnOfProjectMeasuresTable.class)
      .add(3438, "Add primary key on 'UUID' column of 'PROJECT_MEASURES' table", AddPrimaryKeyOnUuidColumnOfProjectMeasuresTable.class)
      .add(3439, "Drop column 'ID' of 'PROJECT_MEASURES' table", DropIdColumnOfProjectMeasuresTable.class)

      // Migration of USER_TOKENS table
      .add(3440, "Add 'UUID' column on 'USER_TOKENS' table", AddUuidColumnToUserTokens.class)
      .add(3441, "Populate 'uuid' for 'USER_TOKENS'", PopulateUserTokensUuid.class)
      .add(3442, "Make 'uuid' column not nullable for user_tokens", MakeUserTokensUuidNotNullable.class)
      .add(3443, "Drop primary key on 'ID' column of 'USER_TOKENS' table", DropPrimaryKeyOnIdColumnOfUserTokensTable.class)
      .add(3444, "Add primary key on 'UUID' column of 'USER_TOKENS' table", AddPrimaryKeyOnUuidColumnOfUserTokensTable.class)
      .add(3445, "Drop column 'ID' of 'USER_TOKENS' table", DropIdColumnOfUserTokensTable.class)

      // Migration on PROJECT_QPROFILES table
      .add(3446, "Add 'uuid' column for 'PROJECT_QPROFILES'", AddUuidColumnToProjectQProfilesTable.class)
      .add(3447, "Populate 'uuid' column for 'PROJECT_QPROFILES'", PopulateProjectQProfilesUuid.class)
      .add(3448, "Make 'uuid' column not nullable for 'PROJECT_QPROFILES'", MakeProjectQProfilesUuidColumnNotNullable.class)
      .add(3449, "Drop primary key on 'ID' column of 'PROJECT_QPROFILES' table", DropPrimaryKeyOnIdColumnOfProjectQProfilesTable.class)
      .add(3450, "Add primary key on 'UUID' column of 'PROJECT_QPROFILES' table", AddPrimaryKeyOnUuidColumnOfProjectQProfilesTable.class)
      .add(3451, "Drop column 'ID' of 'PROJECT_QPROFILES' table", DropIdColumnOfProjectQProfilesTable.class)

      // Migration of MANUAL_MEASURES table
      .add(3452, "Add 'uuid' column for 'MANUAL_MEASURES'", AddUuidColumnToManualMeasures.class)
      .add(3453, "Populate 'uuid' column for 'MANUAL_MEASURES'", PopulateManualMeasureUuid.class)
      .add(3454, "Make 'uuid' column not nullable for 'MANUAL_MEASURES'", MakeManualMeasuresUuidColumnNotNullable.class)
      .add(3455, "Drop primary key on 'ID' column of 'MANUAL_MEASURES' table", DropPrimaryKeyOnIdColumnOfManualMeasuresTable.class)
      .add(3456, "Add primary key on 'UUID' column of 'MANUAL_MEASURES' table", AddPrimaryKeyOnUuidColumnOfManualMeasuresTable.class)
      .add(3457, "Drop column 'ID' of 'MANUAL_MEASURES' table", DropIdColumnOfManualMeasuresTable.class)

      // Migration of GROUP_ROLES table
      .add(3458, "Add 'UUID' column on 'GROUP_ROLES' table", AddUuidColumnToGroupRolesTable.class)
      .add(3459, "Populate 'uuid' for 'GROUP_ROLES'", PopulateGroupRolesUuid.class)
      .add(3460, "Make 'uuid' column not nullable for 'GROUP_ROLES'", MakeGroupRolesUuidColumnNotNullable.class)
      .add(3461, "Drop primary key on 'ID' column of 'GROUP_ROLES' table", DropPrimaryKeyOnIdColumnOfGroupRolesTable.class)
      .add(3462, "Add primary key on 'UUID' column of 'GROUP_ROLES' table", AddPrimaryKeyOnUuidColumnOfGroupRolesTable.class)
      .add(3463, "Drop column 'ID' of 'GROUP_ROLES' table", DropIdColumnOfGroupRolesTable.class)

      // Migration of USER_ROLES table
      .add(3464, "Add 'UUID' column on 'USER_ROLES' table", AddUuidColumnToUserRolesTable.class)
      .add(3465, "Populate 'uuid' for 'USER_ROLES'", PopulateUserRolesUuid.class)
      .add(3466, "Make 'uuid' column not nullable for 'USER_ROLES'", MakeUserRolesUuidColumnNotNullable.class)
      .add(3467, "Drop primary key on 'ID' column of 'USER_ROLES' table", DropPrimaryKeyOnIdColumnOfUserRolesTable.class)
      .add(3468, "Add primary key on 'UUID' column of 'USER_ROLES' table", AddPrimaryKeyOnUuidColumnOfUserRolesTable.class)
      .add(3469, "Drop column 'ID' of 'USER_ROLES' table", DropIdColumnOfUserRolesTable.class)

      // Migration of FILE_SOURCES table
      .add(3470, "Add 'UUID' column on 'FILE_SOURCES' table", AddUuidColumnToFileSourcesTable.class)
      .add(3471, "Populate 'uuid' for 'FILE_SOURCES'", PopulateFileSourcesUuid.class)
      .add(3472, "Make 'uuid' column not nullable for 'FILE_SOURCES'", MakeFileSourcesUuidColumnNotNullable.class)
      .add(3473, "Drop primary key on 'ID' column of 'FILE_SOURCES' table", DropPrimaryKeyOnIdColumnOfFileSourcesTable.class)
      .add(3474, "Add primary key on 'UUID' column of 'FILE_SOURCES' table", AddPrimaryKeyOnUuidColumnOfFileSourcesTable.class)
      .add(3475, "Drop column 'ID' of 'FILE_SOURCES' table", DropIdColumnOfFileSourcesTable.class)

      // Migration of ISSUE_CHANGES table
      .add(3476, "Add 'UUID' column on 'ISSUE_CHANGES' table", AddUuidColumnToIssueChangesTable.class)
      .add(3477, "Populate 'uuid' for 'ISSUE_CHANGES'", PopulateIssueChangesUuid.class)
      .add(3478, "Make 'uuid' column not nullable for 'ISSUE_CHANGES'", MakeIssueChangesUuidColumnNotNullable.class)
      .add(3479, "Drop primary key on 'ID' column of 'ISSUE_CHANGES' table", DropPrimaryKeyOnIdColumnOfIssueChangesTable.class)
      .add(3480, "Add primary key on 'UUID' column of 'ISSUE_CHANGES' table", AddPrimaryKeyOnUuidColumnOfIssueChangesTable.class)
      .add(3481, "Drop column 'ID' of 'ISSUE_CHANGES' table", DropIdColumnOfIssueChangesTable.class)

      // Migration of QUALITY_GATE_CONDITIONS table
      .add(3482, "Add 'UUID' column on 'QUALITY_GATE_CONDITIONS' table", AddUuidColumnToQualityGateConditionsTable.class)
      .add(3483, "Populate 'uuid' for 'QUALITY_GATE_CONDITIONS'", PopulateQualityGateConditionsUuid.class)
      .add(3484, "Make 'uuid' column not nullable for 'QUALITY_GATE_CONDITIONS'", MakeQualityGateConditionsUuidColumnNotNullable.class)
      .add(3485, "Drop primary key on 'ID' column of 'QUALITY_GATE_CONDITIONS' table", DropPrimaryKeyOnIdColumnOfQualityGateConditionsTable.class)
      .add(3486, "Add primary key on 'UUID' column of 'QUALITY_GATE_CONDITIONS' table", AddPrimaryKeyOnUuidColumnOfQualityGateConditionsTable.class)
      .add(3487, "Drop column 'ID' of 'QUALITY_GATE_CONDITIONS' table", DropIdColumnOfQualityGateConditionsTable.class)

      // Migration of PERM_TEMPLATES_GROUPS table
      .add(3488, "Add 'UUID' column on 'PERM_TEMPLATES_GROUPS' table", AddUuidColumnToPermTemplatesGroupsTable.class)
      .add(3489, "Populate 'uuid' for 'PERM_TEMPLATES_GROUPS'", PopulatePermTemplatesGroupsUuid.class)
      .add(3490, "Make 'uuid' column not nullable for 'PERM_TEMPLATES_GROUPS'", MakePermTemplatesGroupsUuidColumnNotNullable.class)
      .add(3491, "Drop primary key on 'ID' column of 'PERM_TEMPLATES_GROUPS' table", DropPrimaryKeyOnIdColumnOfPermTemplatesGroupsTable.class)
      .add(3492, "Add primary key on 'UUID' column of 'PERM_TEMPLATES_GROUPS' table", AddPrimaryKeyOnUuidColumnOfPermTemplatesGroupsTable.class)
      .add(3493, "Drop column 'ID' of 'PERM_TEMPLATES_GROUPS' table", DropIdColumnOfPermTemplatesGroupsTable.class)

      // Migration of PERM_TPL_CHARACTERISTICS table
      .add(3494, "Add 'UUID' column on 'PERM_TPL_CHARACTERISTICS' table", AddUuidColumnToPermTplCharacteristicsTable.class)
      .add(3495, "Populate 'uuid' for 'PERM_TPL_CHARACTERISTICS'", PopulatePermTplCharacteristicsUuid.class)
      .add(3496, "Make 'uuid' column not nullable for 'PERM_TPL_CHARACTERISTICS'", MakePermTplCharacteristicsUuidColumnNotNullable.class)
      .add(3497, "Drop primary key on 'ID' column of 'PERM_TPL_CHARACTERISTICS' table", DropPrimaryKeyOnIdColumnOfPermTplCharacteristicsTable.class)
      .add(3498, "Add primary key on 'UUID' column of 'PERM_TPL_CHARACTERISTICS' table", AddPrimaryKeyOnUuidColumnOfPermTplCharacteristicsTable.class)
      .add(3499, "Drop column 'ID' of 'PERM_TPL_CHARACTERISTICS' table", DropIdColumnOfPermTplCharacteristicsTable.class)

      // Migration of PERM_TEMPLATES_USERS table
      .add(3500, "Add 'UUID' column on 'PERM_TEMPLATES_USERS' table", AddUuidColumnToPermTemplatesUsersTable.class)
      .add(3501, "Populate 'uuid' for 'PERM_TEMPLATES_USERS'", PopulatePermTemplatesUsersUuid.class)
      .add(3502, "Make 'uuid' column not nullable for 'PERM_TEMPLATES_USERS'", MakePermTemplatesUsersUuidColumnNotNullable.class)
      .add(3503, "Drop primary key on 'ID' column of 'PERM_TEMPLATES_USERS' table", DropPrimaryKeyOnIdColumnOfPermTemplatesUsersTable.class)
      .add(3504, "Add primary key on 'UUID' column of 'PERM_TEMPLATES_USERS' table", AddPrimaryKeyOnUuidColumnOfPermTemplatesUsersTable.class)
      .add(3505, "Drop column 'ID' of 'PERM_TEMPLATES_USERS' table", DropIdColumnOfPermTemplatesUsersTable.class)

      // Migration of ACTIVE_RULES table
      .add(3506, "Add 'UUID' column on 'ACTIVE_RULES' table", AddUuidColumnToActiveRulesTable.class)
      .add(3507, "Populate 'uuid' for 'ACTIVE_RULES'", PopulateActiveRulesUuid.class)
      .add(3508, "Make 'uuid' column not nullable for 'ACTIVE_RULES'", MakeActiveRulesUuidColumnNotNullable.class)

      // Migration of FK in ACTIVE_RULE_PARAMETERS to ACTIVE_RULES
      .add(3509, "Add 'active_rule_uuid' column on 'ACTIVE_RULE_PARAMETERS' table", AddActiveRuleUuidColumnToActiveRuleParameters.class)
      .add(3510, "Populate 'active_rule_uuid' for 'ACTIVE_RULE_PARAMETERS'", PopulateActiveRuleParametersActiveRuleUuid.class)
      .add(3511, "Make 'active_rule_uuid' column not nullable for 'ACTIVE_RULE_PARAMETERS'", MakeActiveRuleParametersActiveRuleUuidNotNullable.class)
      .add(3512, "Drop index on 'active_rule_id' column of 'ACTIVE_RULE_PARAMETERS' table", DropIndexOnActiveRuleIdOfActiveRuleParametersTable.class)
      .add(3513, "Add index on 'active_rule_uuid' column of 'ACTIVE_RULE_PARAMETERS' table", AddIndexOnActiveRuleUuidOfActiveRuleParametersTable.class)

      // Finish migration of ACTIVE_RULES
      .add(3514, "Drop primary key on 'ID' column of 'ACTIVE_RULES' table", DropPrimaryKeyOnIdColumnOfActiveRulesTable.class)
      .add(3515, "Add primary key on 'UUID' column of 'ACTIVE_RULES' table", AddPrimaryKeyOnUuidColumnOfActiveRulesTable.class)
      .add(3516, "Drop column 'ID' of 'ACTIVE_RULES' table", DropIdColumnOfActiveRulesTable.class)
      .add(3517, "Drop column 'active_rule_id' of 'ACTIVE_RULE_PARAMETERS' table", DropActiveRuleIdColumnOfActiveRuleParametersTable.class)

    ;
  }
}
