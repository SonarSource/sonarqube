/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
package org.sonar.server.platform.db.migration.version.v84;

import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;
import org.sonar.server.platform.db.migration.version.v84.activeruleparameters.AddPrimaryKeyOnUuidColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activeruleparameters.AddUuidColumnToActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activeruleparameters.DropIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activeruleparameters.DropPrimaryKeyOnIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activeruleparameters.MakeActiveRuleParametersUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.activeruleparameters.PopulateActiveRuleParametersUuid;
import org.sonar.server.platform.db.migration.version.v84.activerules.AddActiveRuleUuidColumnToActiveRuleParameters;
import org.sonar.server.platform.db.migration.version.v84.activerules.AddIndexOnActiveRuleUuidOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.AddPrimaryKeyOnUuidColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.AddUuidColumnToActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.DropActiveRuleIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.DropIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.DropIndexOnActiveRuleIdOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.DropPrimaryKeyOnIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.activerules.MakeActiveRuleParametersActiveRuleUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.activerules.MakeActiveRulesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.activerules.PopulateActiveRuleParametersActiveRuleUuid;
import org.sonar.server.platform.db.migration.version.v84.activerules.PopulateActiveRulesUuid;
import org.sonar.server.platform.db.migration.version.v84.alm.AddClientIdAndClientSecretColumns;
import org.sonar.server.platform.db.migration.version.v84.ceactivity.AddPrimaryKeyOnUuidColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v84.ceactivity.DropIdColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v84.ceactivity.DropPrimaryKeyOnIdColumnOfCeActivityTable;
import org.sonar.server.platform.db.migration.version.v84.cequeue.AddPrimaryKeyOnUuidColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v84.cequeue.DropIdColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v84.cequeue.DropPrimaryKeyOnIdColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v84.cequeue.DropUniqueIndexOnUuidColumnOfCeQueueTable;
import org.sonar.server.platform.db.migration.version.v84.duplicationsindex.AddPrimaryKeyOnUuidColumnOfDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v84.duplicationsindex.AddUuidToDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v84.duplicationsindex.DropIdColumnOfDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v84.duplicationsindex.DropPrimaryKeyOnIdColumnOfDuplicationsIndexTable;
import org.sonar.server.platform.db.migration.version.v84.duplicationsindex.MakeDuplicationsIndexUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.duplicationsindex.PopulateDuplicationsIndexUuid;
import org.sonar.server.platform.db.migration.version.v84.events.AddPrimaryKeyOnUuidColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v84.events.DropIdColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v84.events.DropPrimaryKeyOnIdColumnOfEventsTable;
import org.sonar.server.platform.db.migration.version.v84.filesources.AddPrimaryKeyOnUuidColumnOfFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v84.filesources.AddUuidColumnToFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v84.filesources.DropIdColumnOfFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v84.filesources.DropPrimaryKeyOnIdColumnOfFileSourcesTable;
import org.sonar.server.platform.db.migration.version.v84.filesources.MakeFileSourcesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.filesources.PopulateFileSourcesUuid;
import org.sonar.server.platform.db.migration.version.v84.grouproles.AddPrimaryKeyOnUuidColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.grouproles.AddUuidColumnToGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.grouproles.DropIdColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.grouproles.DropPrimaryKeyOnIdColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.grouproles.MakeGroupRolesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.grouproles.PopulateGroupRolesUuid;
import org.sonar.server.platform.db.migration.version.v84.groups.AddPrimaryKeyOnUuidColumnOfGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.AddUuidColumnToGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.DropIdColumnOfGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.DropPrimaryKeyOnIdColumnOfGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.MakeGroupsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.groups.PopulateGroupsUuid;
import org.sonar.server.platform.db.migration.version.v84.groups.grouproles.AddGroupUuidColumnToGroupRoles;
import org.sonar.server.platform.db.migration.version.v84.groups.grouproles.AddIndexOnGroupUuidOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.groups.grouproles.DropGroupIdColumnOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.groups.grouproles.DropIndexOnGroupIdOfGroupRolesTable;
import org.sonar.server.platform.db.migration.version.v84.groups.grouproles.PopulateGroupRolesGroupUuid;
import org.sonar.server.platform.db.migration.version.v84.groups.groupsusers.AddGroupUuidColumnToGroupsUsers;
import org.sonar.server.platform.db.migration.version.v84.groups.groupsusers.AddIndexOnGroupUuidOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.groups.groupsusers.DropGroupIdColumnOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.groups.groupsusers.DropIndexOnGroupIdOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.groups.groupsusers.MakeGroupsUsersGroupUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.groups.groupsusers.PopulateGroupsUsersGroupUuid;
import org.sonar.server.platform.db.migration.version.v84.groups.organizations.AddDefaultGroupUuidColumnToOrganizations;
import org.sonar.server.platform.db.migration.version.v84.groups.organizations.DropDefaultGroupIdColumnOfOrganizationsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.organizations.PopulateOrganizationsDefaultGroupUuid;
import org.sonar.server.platform.db.migration.version.v84.groups.permtemplatesgroups.AddGroupUuidColumnToPermTemplatesGroups;
import org.sonar.server.platform.db.migration.version.v84.groups.permtemplatesgroups.DropGroupIdColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.permtemplatesgroups.PopulatePermTemplatesGroupsGroupUuid;
import org.sonar.server.platform.db.migration.version.v84.groups.qprofileeditgroups.AddGroupUuidColumnToQProfileEditGroups;
import org.sonar.server.platform.db.migration.version.v84.groups.qprofileeditgroups.AddIndexOnGroupUuidOfQProfileEditGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.qprofileeditgroups.DropGroupIdColumnOfQProfileEditGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.qprofileeditgroups.DropIndexOnGroupIdOfQProfileEditGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.groups.qprofileeditgroups.MakeQProfileEditGroupsGroupUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.groups.qprofileeditgroups.PopulateQProfileEditGroupsGroupUuid;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.AddIndexOnIssueKeyOfIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.AddIndexOnKeeOfIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.AddPrimaryKeyOnUuidColumnOfIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.CopyIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.DropIssueChangesTable;
import org.sonar.server.platform.db.migration.version.v84.issuechanges.RenameIssueChangesCopyToIssueChanges;
import org.sonar.server.platform.db.migration.version.v84.issues.AddPrimaryKeyOnKeeColumnOfIssuesTable;
import org.sonar.server.platform.db.migration.version.v84.manualmeasures.AddPrimaryKeyOnUuidColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.manualmeasures.AddUuidColumnToManualMeasures;
import org.sonar.server.platform.db.migration.version.v84.manualmeasures.DropIdColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.manualmeasures.DropPrimaryKeyOnIdColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.manualmeasures.MakeManualMeasuresUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.manualmeasures.PopulateManualMeasureUuid;
import org.sonar.server.platform.db.migration.version.v84.metrics.AddPrimaryKeyOnUuidColumnOfMetricsTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.AddUuidColumnToMetricsTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.DropIdColumnOfMetricsTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.DropPrimaryKeyOnIdColumnOfMetricsTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.MakeMetricsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.metrics.PopulateMetricsUuid;
import org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures.AddIndexOnMetricUuidOfLiveMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures.AddIndexOnProjectUuidOfLiveMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures.AddPKeyOnUuidOfLiveMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures.CopyLiveMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures.DropLiveMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.livemeasures.RenameLiveMeasuresCopyToLiveMeasures;
import org.sonar.server.platform.db.migration.version.v84.metrics.manualmeasures.AddMetricUuidColumnToManualMeasures;
import org.sonar.server.platform.db.migration.version.v84.metrics.manualmeasures.DropMetricIdColumnOfManualMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.manualmeasures.MakeManualMeasuresMetricUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.metrics.manualmeasures.PopulateManualMeasuresMetricUuid;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.AddIndexOnMetricUuidAndAnalysisUuidOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.AddIndexOnMetricUuidOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.AddMetricUuidColumnToProjectMeasures;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.DeleteSecurityReviewRatingProjectMeasures;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.DropIndexOnMetricIdOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.DropMetricIdColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.MakeProjectMeasuresMetricUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.metrics.projectmeasures.PopulateProjectMeasuresMetricUuid;
import org.sonar.server.platform.db.migration.version.v84.metrics.qualitygateconditions.AddMetricUuidColumnToQualityGateConditions;
import org.sonar.server.platform.db.migration.version.v84.metrics.qualitygateconditions.DropMetricIdColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v84.metrics.qualitygateconditions.MakeQualityGateConditionsMetricUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.metrics.qualitygateconditions.PopulateQualityGateConditionsMetricUuid;
import org.sonar.server.platform.db.migration.version.v84.notifications.AddPrimaryKeyOnUuidColumnOfNotificationTable;
import org.sonar.server.platform.db.migration.version.v84.notifications.AddUuidAndCreatedAtColumnsToNotification;
import org.sonar.server.platform.db.migration.version.v84.notifications.DropIdColumnOfNotificationTable;
import org.sonar.server.platform.db.migration.version.v84.notifications.DropPrimaryKeyOnIdColumnOfNotificationTable;
import org.sonar.server.platform.db.migration.version.v84.notifications.MakeNotificationUuidAndCreatedAtColumnsNotNullable;
import org.sonar.server.platform.db.migration.version.v84.notifications.PopulateNotificationUuidAndCreatedAt;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.AddPrimaryKeyOnUuidColumnOfPermissionTemplatesTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.AddUuidColumnToPermissionTemplates;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.DropIdColumnOfPermissionTemplatesTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.DropKeeColumnOfPermissionTemplatesTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.DropPrimaryKeyOnIdColumnOfPermissionTemplatesTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.MakePermissionTemplateUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.PopulatePermissionTemplatesUuid;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesgroups.AddTemplateUuidColumnToPermTemplatesGroups;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesgroups.DropTemplateIdColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesgroups.MakePermTemplatesGroupsTemplateUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesgroups.PopulatePermTemplatesGroupsTemplateUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesusers.AddTemplateUuidColumnToPermTemplatesUsers;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesusers.DropTemplateIdColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesusers.MakePermTemplatesUsersTemplateUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtemplatesusers.PopulatePermTemplatesUsersTemplateUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtplcharacteristics.AddTemplateUuidColumnToPermTplCharacteristics;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtplcharacteristics.AddUniqueIndexOnTemplateUuidAndPermissionKeyColumnsOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtplcharacteristics.DropTemplateIdColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtplcharacteristics.DropUniqueIndexOnTemplateIdAndPermissionKeyColumnsOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtplcharacteristics.MakePermTplCharacteristicsTemplateUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permissiontemplates.fk.permtplcharacteristics.PopulatePermTplCharacteristicsTemplateUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesgroups.AddPrimaryKeyOnUuidColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesgroups.AddUuidColumnToPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesgroups.DropIdColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesgroups.DropPrimaryKeyOnIdColumnOfPermTemplatesGroupsTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesgroups.MakePermTemplatesGroupsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesgroups.PopulatePermTemplatesGroupsUuid;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesusers.AddPrimaryKeyOnUuidColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesusers.AddUuidColumnToPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesusers.DropIdColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesusers.DropPrimaryKeyOnIdColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesusers.MakePermTemplatesUsersUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permtemplatesusers.PopulatePermTemplatesUsersUuid;
import org.sonar.server.platform.db.migration.version.v84.permtplcharacteristics.AddPrimaryKeyOnUuidColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permtplcharacteristics.AddUuidColumnToPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permtplcharacteristics.DropIdColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permtplcharacteristics.DropPrimaryKeyOnIdColumnOfPermTplCharacteristicsTable;
import org.sonar.server.platform.db.migration.version.v84.permtplcharacteristics.MakePermTplCharacteristicsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.permtplcharacteristics.PopulatePermTplCharacteristicsUuid;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.AddPrimaryKeyOnUuidColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.AddTechIndexOnUuidOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.AddUuidColumnToProjectMeasures;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.DropIdColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.DropPrimaryKeyOnIdColumnOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.DropTechIndexOnUuidOfProjectMeasuresTable;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.MakeProjectMeasuresUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.projectmeasures.PopulateProjectMeasureUuid;
import org.sonar.server.platform.db.migration.version.v84.projectqprofiles.AddPrimaryKeyOnUuidColumnOfProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.projectqprofiles.AddUuidColumnToProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.projectqprofiles.DropIdColumnOfProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.projectqprofiles.DropPrimaryKeyOnIdColumnOfProjectQProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.projectqprofiles.MakeProjectQProfilesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.projectqprofiles.PopulateProjectQProfilesUuid;
import org.sonar.server.platform.db.migration.version.v84.properties.AddPrimaryKeyOnUuidColumnOfPropertiesTable;
import org.sonar.server.platform.db.migration.version.v84.properties.AddUuidColumnToProperties;
import org.sonar.server.platform.db.migration.version.v84.properties.DropIdColumnOfPropertiesTable;
import org.sonar.server.platform.db.migration.version.v84.properties.DropPrimaryKeyOnIdColumnOfPropertiesTable;
import org.sonar.server.platform.db.migration.version.v84.properties.MakePropertiesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.properties.PopulatePropertiesUuid;
import org.sonar.server.platform.db.migration.version.v84.qualitygateconditions.AddPrimaryKeyOnUuidColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygateconditions.AddUuidColumnToQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygateconditions.DropIdColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygateconditions.DropPrimaryKeyOnIdColumnOfQualityGateConditionsTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygateconditions.MakeQualityGateConditionsUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.qualitygateconditions.PopulateQualityGateConditionsUuid;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.AddPrimaryKeyOnUuidColumnOfQGatesTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.AddQGateUuidColumnForQGateConditions;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.DropIdColumnOfQGateTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.DropOrphansQGateConditions;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.DropPrimaryKeyOnIdColumnOfQGatesTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.DropQGateIdColumnForQGateConditions;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.DropUniqueIndexOnUuidColumnOfQualityGatesTable;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.MakeQGateUuidColumnNotNullableForQGateConditions;
import org.sonar.server.platform.db.migration.version.v84.qualitygates.PopulateQGateUuidColumnForQGateConditions;
import org.sonar.server.platform.db.migration.version.v84.rules.AddPrimaryKeyOnUuidColumnOfRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.AddUuidAndTemplateUuidColumnsToRules;
import org.sonar.server.platform.db.migration.version.v84.rules.DropIdColumnOfRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.DropPrimaryKeyOnIdColumnOfRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.DropTemplateIdColumnOfRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.MakeRulesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rules.PopulateRulesTemplateUuid;
import org.sonar.server.platform.db.migration.version.v84.rules.PopulateRulesUuid;
import org.sonar.server.platform.db.migration.version.v84.rules.activerules.AddIndexToActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.activerules.AddRuleUuidColumnToActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.activerules.DropIndexOnRuleIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.activerules.DropRuleIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.activerules.MakeActiveRulesRuleUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rules.activerules.PopulateActiveRulesRuleUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys.AddIndexToDeprecatedRuleKeysTable;
import org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys.AddRuleUuidColumnToDeprecatedRuleKeysTable;
import org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys.DropIndexOnRuleIdColumnOfDeprecatedRuleKeysTable;
import org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys.DropRuleIdColumnOfDeprecatedRuleKeysTable;
import org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys.MakeDeprecatedRuleKeysRuleUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rules.deprecatedrulekeys.PopulateDeprecatedRuleKeysRuleUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.rules.issues.AddIndexesToIssuesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.issues.CopyIssuesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.issues.DropIssuesTable;
import org.sonar.server.platform.db.migration.version.v84.rules.issues.RenameIssuesCopyToIssues;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesmetadata.AddPrimaryKeyOnUuidAndOrganizationUuidColumnOfRulesMetadataTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesmetadata.AddRuleUuidColumnToRulesMetadataTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesmetadata.DropPrimaryKeyOnIdColumnOfRulesMetadataTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesmetadata.DropRuleIdColumnOfRulesMetadataTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesmetadata.MakeRulesMetadataRuleUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesmetadata.PopulateRulesMetadataRuleUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters.AddIndexesToRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters.AddRuleUuidColumnToRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters.DropIndexesOnRuleIdColumnOfRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters.DropRuleIdColumnOfRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters.MakeRulesParametersRuleUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rules.rulesparameters.PopulateRulesParametersRuleUuidColumn;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.AddPrimaryKeyOnUuidColumnOfRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.AddUuidColumnToRulesParameters;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.DropIdColumnOfRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.DropPrimaryKeyOnIdColumnOfRulesParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.MakeRulesParametersUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.PopulateRulesParametersUuid;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.fk.AddRulesParameterUuidColumnToActiveRuleParameters;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.fk.DropRulesParameterIdColumnOfActiveRuleParametersTable;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.fk.MakeActiveRuleParametersRulesParameterUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rulesparameters.fk.PopulateActiveRuleParametersRulesParameterUuid;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.AddPrimaryKeyOnUuidColumnOfRulesProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.AddUuidColumnToRulesProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.DropIdColumnOfRulesProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.DropKeeColumnOfRulesProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.DropPrimaryKeyOnIdColumnOfRulesProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.DropUniqueIndexOnKeeColumnOfRulesProfilesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.MakeRulesProfilesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.PopulateRulesProfilesUuid;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules.AddProfileUuidColumnToActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules.AddUniqueIndexOnProfileUuidColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules.DropProfileIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules.DropUniqueIndexOnProfileIdColumnOfActiveRulesTable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules.MakeActiveRulesProfileUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.activerules.PopulateActiveRulesProfileUuid;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.orgqprofiles.PopulateOrgQProfilesRulesProfileUuid;
import org.sonar.server.platform.db.migration.version.v84.rulesprofiles.fk.qprofilechanges.PopulateQProfileChangesRulesProfileUuid;
import org.sonar.server.platform.db.migration.version.v84.snapshots.issues.AddPrimaryKeyOnUuidColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v84.snapshots.issues.DropIdColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v84.snapshots.issues.DropPrimaryKeyOnIdColumnOfSnapshotsTable;
import org.sonar.server.platform.db.migration.version.v84.userroles.AddPrimaryKeyOnUuidColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.userroles.AddUuidColumnToUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.userroles.DropIdColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.userroles.DropPrimaryKeyOnIdColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.userroles.MakeUserRolesUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.userroles.PopulateUserRolesUuid;
import org.sonar.server.platform.db.migration.version.v84.users.AddPrimaryKeyOnUuidColumnOfUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.DropIdColumnOfUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.DropPrimaryKeyOnIdColumnOfUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.DropUniqueIndexOnUuidColumnOfUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.AddIndexOnUserUuidOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.AddUniqueIndexOnUserUuidAndGroupIdOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.AddUserUuidColumnToGroupsUsers;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.DropIndexOnUserIdOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.DropUniqueIndexOnUserIdAndGroupIdOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.DropUserIdColumnOfGroupsUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.MakeGroupsUsersUserUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.groupsusers.PopulateGroupsUsersUserUuid;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.AddIndexOnUserUuidOfOrganizationMembersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.AddPrimaryKeyOnUserUuidAndOrganizationUuidColumnsOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.AddUserUuidColumnToOrganizationMembers;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.DropIndexOnUserIdOfOrganizationMembersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.DropPrimaryKeyOnUserIdAndOrganizationUuidOfOrganizationMembersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.DropUserIdColumnOfOrganizationMembersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.MakeOrganizationMembersUserUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.organizationmembers.PopulateOrganizationMembersUserUuid;
import org.sonar.server.platform.db.migration.version.v84.users.fk.permtemplatesusers.AddUserUuidColumnToPermTemplatesUsers;
import org.sonar.server.platform.db.migration.version.v84.users.fk.permtemplatesusers.DropUserIdColumnOfPermTemplatesUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.permtemplatesusers.MakePermTemplatesUsersUserUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.permtemplatesusers.PopulatePermTemplatesUsersUserUuid;
import org.sonar.server.platform.db.migration.version.v84.users.fk.properties.AddUserUuidColumnToPropertiesUsers;
import org.sonar.server.platform.db.migration.version.v84.users.fk.properties.DropUserIdColumnOfPropertiesTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.properties.PopulatePropertiesUserUuid;
import org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers.AddUniqueIndexOnUserUuidAndQProfileUuidOfQProfileEditUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers.AddUserUuidColumnToQProfileEditUsers;
import org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers.DropUniqueIndexOnUserIdAndQProfileUuidOfQProfileEditUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers.DropUserIdColumnOfQProfileEditUsersTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers.MakeQProfileEditUsersUserUuidColumnNotNullable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.qprofileeditusers.PopulateQProfileEditUsersUserUuid;
import org.sonar.server.platform.db.migration.version.v84.users.fk.userroles.AddIndexOnUserUuidOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.userroles.AddUserUuidColumnToUserRoles;
import org.sonar.server.platform.db.migration.version.v84.users.fk.userroles.DropIndexOnUserIdOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.userroles.DropUserIdColumnOfUserRolesTable;
import org.sonar.server.platform.db.migration.version.v84.users.fk.userroles.PopulateUserRolesUserUuid;
import org.sonar.server.platform.db.migration.version.v84.usertokens.AddPrimaryKeyOnUuidColumnOfUserTokensTable;
import org.sonar.server.platform.db.migration.version.v84.usertokens.AddUuidColumnToUserTokens;
import org.sonar.server.platform.db.migration.version.v84.usertokens.DropIdColumnOfUserTokensTable;
import org.sonar.server.platform.db.migration.version.v84.usertokens.DropPrimaryKeyOnIdColumnOfUserTokensTable;
import org.sonar.server.platform.db.migration.version.v84.usertokens.MakeUserTokensUuidNotNullable;
import org.sonar.server.platform.db.migration.version.v84.usertokens.PopulateUserTokensUuid;
import org.sonar.server.platform.db.migration.version.v85.AddIndexOnProjectUuidOnIssueChangesTable;

public class DbVersion84 implements DbVersion {
  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
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

      // Migration on SNAPSHOTS table
      .add(3409, "Drop primary key on 'ID' column of 'SNAPSHOTS' table", DropPrimaryKeyOnIdColumnOfSnapshotsTable.class)
      .add(3410, "Add primary key on 'UUID' column of 'SNAPSHOTS' table", AddPrimaryKeyOnUuidColumnOfSnapshotsTable.class)
      .add(3411, "Drop column 'ID' of 'SNAPSHOTS' table", DropIdColumnOfSnapshotsTable.class)

      // Migration on CE_QUEUE table
      .add(3412, "Drop unique index on 'uuid' column of 'CE_QUEUE' table", DropUniqueIndexOnUuidColumnOfCeQueueTable.class)
      .add(3413, "Drop primary key on 'ID' column of 'CE_QUEUE' table", DropPrimaryKeyOnIdColumnOfCeQueueTable.class)
      .add(3414, "Add primary key on 'UUID' column of 'CE_QUEUE' table", AddPrimaryKeyOnUuidColumnOfCeQueueTable.class)
      .add(3415, "Drop column 'ID' of 'CE_QUEUE' table", DropIdColumnOfCeQueueTable.class)

      // Migration on CE_ACTIVITY table
      .add(3416, "Drop primary key on 'ID' column of 'CE_ACTIVITY' table", DropPrimaryKeyOnIdColumnOfCeActivityTable.class)
      .add(3417, "Add primary key on 'UUID' column of 'CE_ACTIVITY' table", AddPrimaryKeyOnUuidColumnOfCeActivityTable.class)
      .add(3418, "Drop column 'ID' of 'CE_ACTIVITY' table", DropIdColumnOfCeActivityTable.class)

      // Migration of DUPLICATIONS_INDEX table
      .add(3419, "Add 'uuid' columns for DUPLICATIONS_INDEX", AddUuidToDuplicationsIndexTable.class)
      .add(3420, "Populate 'uuid' columns for DUPLICATIONS_INDEX", PopulateDuplicationsIndexUuid.class)
      .add(3421, "Make 'uuid' column not nullable for DUPLICATIONS_INDEX", MakeDuplicationsIndexUuidColumnNotNullable.class)
      .add(3422, "Drop primary key on 'ID' column of 'DUPLICATIONS_INDEX' table", DropPrimaryKeyOnIdColumnOfDuplicationsIndexTable.class)
      .add(3423, "Add primary key on 'UUID' column of 'DUPLICATIONS_INDEX' table", AddPrimaryKeyOnUuidColumnOfDuplicationsIndexTable.class)
      .add(3424, "Drop column 'ID' of 'DUPLICATIONS_INDEX' table", DropIdColumnOfDuplicationsIndexTable.class)

      // Migration of ACTIVE_RULE_PARAMS table
      .add(3425, "Add 'uuid' column for 'ACTIVE_RULE_PARAMS' table", AddUuidColumnToActiveRuleParametersTable.class)
      .add(3426, "Populate 'uuid' column for 'ACTIVE_RULE_PARAMS' table", PopulateActiveRuleParametersUuid.class)
      .add(3427, "Make 'uuid' column not nullable for 'ACTIVE_RULE_PARAMS' table", MakeActiveRuleParametersUuidColumnNotNullable.class)
      .add(3428, "Drop primary key on 'ID' column of 'ACTIVE_RULE_PARAMS' table", DropPrimaryKeyOnIdColumnOfActiveRuleParametersTable.class)
      .add(3429, "Add primary key on 'UUID' column of 'ACTIVE_RULE_PARAMS' table", AddPrimaryKeyOnUuidColumnOfActiveRuleParametersTable.class)
      .add(3430, "Drop column 'ID' of 'ACTIVE_RULE_PARAMS' table", DropIdColumnOfActiveRuleParametersTable.class)

      // Migration on PROJECT_MEASURES table
      .add(3431, "Add 'uuid' columns for 'PROJECT_MEASURES'", AddUuidColumnToProjectMeasures.class)
      .add(3432, "Add tech index on 'group_uuid' column of 'PROJECT_MEASURES' table", AddTechIndexOnUuidOfProjectMeasuresTable.class)
      .add(3433, "Populate 'uuid' column for 'PROJECT_MEASURES'", PopulateProjectMeasureUuid.class)
      .add(3434, "Drop tech index on 'group_id' column of 'PROJECT_MEASURES' table", DropTechIndexOnUuidOfProjectMeasuresTable.class)
      .add(3435, "Make 'uuid' column not nullable for 'PROJECT_MEASURES'", MakeProjectMeasuresUuidColumnNotNullable.class)
      .add(3436, "Drop primary key on 'ID' column of 'PROJECT_MEASURES' table", DropPrimaryKeyOnIdColumnOfProjectMeasuresTable.class)
      .add(3437, "Add primary key on 'UUID' column of 'PROJECT_MEASURES' table", AddPrimaryKeyOnUuidColumnOfProjectMeasuresTable.class)
      .add(3438, "Drop column 'ID' of 'PROJECT_MEASURES' table", DropIdColumnOfProjectMeasuresTable.class)

      // Migration of USER_TOKENS table
      .add(3439, "Add 'UUID' column on 'USER_TOKENS' table", AddUuidColumnToUserTokens.class)
      .add(3440, "Populate 'uuid' for 'USER_TOKENS'", PopulateUserTokensUuid.class)
      .add(3441, "Make 'uuid' column not nullable for user_tokens", MakeUserTokensUuidNotNullable.class)
      .add(3442, "Drop primary key on 'ID' column of 'USER_TOKENS' table", DropPrimaryKeyOnIdColumnOfUserTokensTable.class)
      .add(3443, "Add primary key on 'UUID' column of 'USER_TOKENS' table", AddPrimaryKeyOnUuidColumnOfUserTokensTable.class)
      .add(3444, "Drop column 'ID' of 'USER_TOKENS' table", DropIdColumnOfUserTokensTable.class)

      // Migration on PROJECT_QPROFILES table
      .add(3445, "Add 'uuid' column for 'PROJECT_QPROFILES'", AddUuidColumnToProjectQProfilesTable.class)
      .add(3446, "Populate 'uuid' column for 'PROJECT_QPROFILES'", PopulateProjectQProfilesUuid.class)
      .add(3447, "Make 'uuid' column not nullable for 'PROJECT_QPROFILES'", MakeProjectQProfilesUuidColumnNotNullable.class)
      .add(3448, "Drop primary key on 'ID' column of 'PROJECT_QPROFILES' table", DropPrimaryKeyOnIdColumnOfProjectQProfilesTable.class)
      .add(3449, "Add primary key on 'UUID' column of 'PROJECT_QPROFILES' table", AddPrimaryKeyOnUuidColumnOfProjectQProfilesTable.class)
      .add(3450, "Drop column 'ID' of 'PROJECT_QPROFILES' table", DropIdColumnOfProjectQProfilesTable.class)

      // Migration of MANUAL_MEASURES table
      .add(3451, "Add 'uuid' column for 'MANUAL_MEASURES'", AddUuidColumnToManualMeasures.class)
      .add(3452, "Populate 'uuid' column for 'MANUAL_MEASURES'", PopulateManualMeasureUuid.class)
      .add(3453, "Make 'uuid' column not nullable for 'MANUAL_MEASURES'", MakeManualMeasuresUuidColumnNotNullable.class)
      .add(3454, "Drop primary key on 'ID' column of 'MANUAL_MEASURES' table", DropPrimaryKeyOnIdColumnOfManualMeasuresTable.class)
      .add(3455, "Add primary key on 'UUID' column of 'MANUAL_MEASURES' table", AddPrimaryKeyOnUuidColumnOfManualMeasuresTable.class)
      .add(3456, "Drop column 'ID' of 'MANUAL_MEASURES' table", DropIdColumnOfManualMeasuresTable.class)

      // Migration of GROUP_ROLES table
      .add(3457, "Add 'UUID' column on 'GROUP_ROLES' table", AddUuidColumnToGroupRolesTable.class)
      .add(3458, "Populate 'uuid' for 'GROUP_ROLES'", PopulateGroupRolesUuid.class)
      .add(3459, "Make 'uuid' column not nullable for 'GROUP_ROLES'", MakeGroupRolesUuidColumnNotNullable.class)
      .add(3460, "Drop primary key on 'ID' column of 'GROUP_ROLES' table", DropPrimaryKeyOnIdColumnOfGroupRolesTable.class)
      .add(3461, "Add primary key on 'UUID' column of 'GROUP_ROLES' table", AddPrimaryKeyOnUuidColumnOfGroupRolesTable.class)
      .add(3462, "Drop column 'ID' of 'GROUP_ROLES' table", DropIdColumnOfGroupRolesTable.class)

      // Migration of USER_ROLES table
      .add(3463, "Add 'UUID' column on 'USER_ROLES' table", AddUuidColumnToUserRolesTable.class)
      .add(3464, "Populate 'uuid' for 'USER_ROLES'", PopulateUserRolesUuid.class)
      .add(3465, "Make 'uuid' column not nullable for 'USER_ROLES'", MakeUserRolesUuidColumnNotNullable.class)
      .add(3466, "Drop primary key on 'ID' column of 'USER_ROLES' table", DropPrimaryKeyOnIdColumnOfUserRolesTable.class)
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
      .add(3476, "Copy 'ISSUE_CHANGES' table to 'ISSUE_CHANGES_COPY'", CopyIssueChangesTable.class)
      .add(3477, "Drop 'ISSUE_CHANGES' table", DropIssueChangesTable.class)
      .add(3478, "Rename table 'ISSUE_CHANGES_COPY' to 'ISSUE_CHANGES'", RenameIssueChangesCopyToIssueChanges.class)
      .add(3479, "Add index on 'ISSUE_KEY' of 'ISSUE_CHANGES' table", AddIndexOnIssueKeyOfIssueChangesTable.class)
      .add(3480, "Add index on 'KEE' of 'ISSUE_CHANGES' table", AddIndexOnKeeOfIssueChangesTable.class)
      .add(3481, "Add primary key on 'UUID' column of 'ISSUE_CHANGES' table", AddPrimaryKeyOnUuidColumnOfIssueChangesTable.class)
      .add(3482, "Add index on 'project_uuid' for table 'ISSUE_CHANGES'", AddIndexOnProjectUuidOnIssueChangesTable.class)

      // Migration of QUALITY_GATE_CONDITIONS table
      .add(3483, "Add 'UUID' column on 'QUALITY_GATE_CONDITIONS' table", AddUuidColumnToQualityGateConditionsTable.class)
      .add(3484, "Populate 'uuid' for 'QUALITY_GATE_CONDITIONS'", PopulateQualityGateConditionsUuid.class)
      .add(3485, "Make 'uuid' column not nullable for 'QUALITY_GATE_CONDITIONS'", MakeQualityGateConditionsUuidColumnNotNullable.class)
      .add(3486, "Drop primary key on 'ID' column of 'QUALITY_GATE_CONDITIONS' table", DropPrimaryKeyOnIdColumnOfQualityGateConditionsTable.class)
      .add(3487, "Add primary key on 'UUID' column of 'QUALITY_GATE_CONDITIONS' table", AddPrimaryKeyOnUuidColumnOfQualityGateConditionsTable.class)
      .add(3488, "Drop column 'ID' of 'QUALITY_GATE_CONDITIONS' table", DropIdColumnOfQualityGateConditionsTable.class)

      // Migration of PERM_TEMPLATES_GROUPS table
      .add(3489, "Add 'UUID' column on 'PERM_TEMPLATES_GROUPS' table", AddUuidColumnToPermTemplatesGroupsTable.class)
      .add(3490, "Populate 'uuid' for 'PERM_TEMPLATES_GROUPS'", PopulatePermTemplatesGroupsUuid.class)
      .add(3491, "Make 'uuid' column not nullable for 'PERM_TEMPLATES_GROUPS'", MakePermTemplatesGroupsUuidColumnNotNullable.class)
      .add(3492, "Drop primary key on 'ID' column of 'PERM_TEMPLATES_GROUPS' table", DropPrimaryKeyOnIdColumnOfPermTemplatesGroupsTable.class)
      .add(3493, "Add primary key on 'UUID' column of 'PERM_TEMPLATES_GROUPS' table", AddPrimaryKeyOnUuidColumnOfPermTemplatesGroupsTable.class)
      .add(3494, "Drop column 'ID' of 'PERM_TEMPLATES_GROUPS' table", DropIdColumnOfPermTemplatesGroupsTable.class)

      // Migration of PERM_TPL_CHARACTERISTICS table
      .add(3495, "Add 'UUID' column on 'PERM_TPL_CHARACTERISTICS' table", AddUuidColumnToPermTplCharacteristicsTable.class)
      .add(3496, "Populate 'uuid' for 'PERM_TPL_CHARACTERISTICS'", PopulatePermTplCharacteristicsUuid.class)
      .add(3497, "Make 'uuid' column not nullable for 'PERM_TPL_CHARACTERISTICS'", MakePermTplCharacteristicsUuidColumnNotNullable.class)
      .add(3498, "Drop primary key on 'ID' column of 'PERM_TPL_CHARACTERISTICS' table", DropPrimaryKeyOnIdColumnOfPermTplCharacteristicsTable.class)
      .add(3499, "Add primary key on 'UUID' column of 'PERM_TPL_CHARACTERISTICS' table", AddPrimaryKeyOnUuidColumnOfPermTplCharacteristicsTable.class)
      .add(3500, "Drop column 'ID' of 'PERM_TPL_CHARACTERISTICS' table", DropIdColumnOfPermTplCharacteristicsTable.class)

      // Migration of PERM_TEMPLATES_USERS table
      .add(3501, "Add 'UUID' column on 'PERM_TEMPLATES_USERS' table", AddUuidColumnToPermTemplatesUsersTable.class)
      .add(3502, "Populate 'uuid' for 'PERM_TEMPLATES_USERS'", PopulatePermTemplatesUsersUuid.class)
      .add(3503, "Make 'uuid' column not nullable for 'PERM_TEMPLATES_USERS'", MakePermTemplatesUsersUuidColumnNotNullable.class)
      .add(3504, "Drop primary key on 'ID' column of 'PERM_TEMPLATES_USERS' table", DropPrimaryKeyOnIdColumnOfPermTemplatesUsersTable.class)
      .add(3505, "Add primary key on 'UUID' column of 'PERM_TEMPLATES_USERS' table", AddPrimaryKeyOnUuidColumnOfPermTemplatesUsersTable.class)
      .add(3506, "Drop column 'ID' of 'PERM_TEMPLATES_USERS' table", DropIdColumnOfPermTemplatesUsersTable.class)

      // Migration of ACTIVE_RULES table
      .add(3507, "Add 'UUID' column on 'ACTIVE_RULES' table", AddUuidColumnToActiveRulesTable.class)
      .add(3508, "Populate 'uuid' for 'ACTIVE_RULES'", PopulateActiveRulesUuid.class)
      .add(3509, "Make 'uuid' column not nullable for 'ACTIVE_RULES'", MakeActiveRulesUuidColumnNotNullable.class)

      // Migration of FK in ACTIVE_RULE_PARAMETERS to ACTIVE_RULES
      .add(3510, "Add 'active_rule_uuid' column on 'ACTIVE_RULE_PARAMETERS' table", AddActiveRuleUuidColumnToActiveRuleParameters.class)
      .add(3511, "Populate 'active_rule_uuid' for 'ACTIVE_RULE_PARAMETERS'", PopulateActiveRuleParametersActiveRuleUuid.class)
      .add(3512, "Make 'active_rule_uuid' column not nullable for 'ACTIVE_RULE_PARAMETERS'", MakeActiveRuleParametersActiveRuleUuidNotNullable.class)
      .add(3513, "Drop index on 'active_rule_id' column of 'ACTIVE_RULE_PARAMETERS' table", DropIndexOnActiveRuleIdOfActiveRuleParametersTable.class)
      .add(3514, "Add index on 'active_rule_uuid' column of 'ACTIVE_RULE_PARAMETERS' table", AddIndexOnActiveRuleUuidOfActiveRuleParametersTable.class)

      // Finish migration of ACTIVE_RULES
      .add(3515, "Drop primary key on 'ID' column of 'ACTIVE_RULES' table", DropPrimaryKeyOnIdColumnOfActiveRulesTable.class)
      .add(3516, "Add primary key on 'UUID' column of 'ACTIVE_RULES' table", AddPrimaryKeyOnUuidColumnOfActiveRulesTable.class)
      .add(3517, "Drop column 'ID' of 'ACTIVE_RULES' table", DropIdColumnOfActiveRulesTable.class)
      .add(3518, "Drop column 'active_rule_id' of 'ACTIVE_RULE_PARAMETERS' table", DropActiveRuleIdColumnOfActiveRuleParametersTable.class)

      // Migration on RULES_PARAMETERS table - populate uuid column
      .add(3519, "Add 'uuid' column for 'RULES_PARAMETERS'", AddUuidColumnToRulesParameters.class)
      .add(3520, "Populate 'uuid' column for 'RULES_PARAMETERS'", PopulateRulesParametersUuid.class)
      .add(3521, "Make 'uuid' column not nullable for 'RULES_PARAMETERS'", MakeRulesParametersUuidColumnNotNullable.class)

      // Migration of ACTIVE_RULE_PARAMS FK to RULES_PARAMETERS, switch from ruleParamId to ruleParamUuid
      .add(3522, "Add 'rules_parameter_uuid' column for 'ACTIVE_RULE_PARAMS' table", AddRulesParameterUuidColumnToActiveRuleParameters.class)
      .add(3523, "Populate 'rules_parameter_uuid' column for 'ACTIVE_RULE_PARAMS' table", PopulateActiveRuleParametersRulesParameterUuid.class)
      .add(3524, "Make 'rules_parameter_uuid' column not nullable for 'ACTIVE_RULE_PARAMS' table", MakeActiveRuleParametersRulesParameterUuidColumnNotNullable.class)
      .add(3525, "Drop column 'rules_parameter_id' of 'ACTIVE_RULE_PARAMS' table", DropRulesParameterIdColumnOfActiveRuleParametersTable.class)

      // Migration on RULES_PARAMETERS table change PK
      .add(3526, "Drop primary key on 'ID' column of 'RULES_PARAMETERS' table", DropPrimaryKeyOnIdColumnOfRulesParametersTable.class)
      .add(3527, "Add primary key on 'UUID' column of 'RULES_PARAMETERS' table", AddPrimaryKeyOnUuidColumnOfRulesParametersTable.class)
      .add(3528, "Drop column 'ID' of 'RULES_PARAMETERS' table", DropIdColumnOfRulesParametersTable.class)

      // Migration of METRICS table
      .add(3529, "Add 'UUID' column on 'METRICS' table", AddUuidColumnToMetricsTable.class)
      .add(3530, "Populate 'uuid' for 'METRICS'", PopulateMetricsUuid.class)
      .add(3531, "Make 'uuid' column not nullable for 'METRICS'", MakeMetricsUuidColumnNotNullable.class)

      // Migration of FK in PROJECT_MEASURES to METRICS
      .add(3532, "Add 'metric_uuid' column on 'PROJECT_MEASURES' table", AddMetricUuidColumnToProjectMeasures.class)
      .add(3533, "Populate 'metric_uuid' for 'PROJECT_MEASURES'", PopulateProjectMeasuresMetricUuid.class)
      .add(3534, "Make 'metric_uuid' column not nullable for 'PROJECT_MEASURES'", MakeProjectMeasuresMetricUuidNotNullable.class)
      .add(3535, "Drop index on 'metric_id' and 'analysis_uuid' columns of 'PROJECT_MEASURES' table", DropIndexOnMetricIdOfProjectMeasuresTable.class)
      .add(3536, "Add index on 'metric_uuid' and 'analysis_uuid' columns of 'PROJECT_MEASURES' table", AddIndexOnMetricUuidAndAnalysisUuidOfProjectMeasuresTable.class)

      // Migration of FK in QUALITY_GATE_CONDITIONS to METRICS
      .add(3537, "Add 'metric_uuid' column on 'QUALITY_GATE_CONDITIONS' table", AddMetricUuidColumnToQualityGateConditions.class)
      .add(3538, "Populate 'metric_uuid' for 'QUALITY_GATE_CONDITIONS'", PopulateQualityGateConditionsMetricUuid.class)
      .add(3539, "Make 'metric_uuid' column not nullable for 'QUALITY_GATE_CONDITIONS'", MakeQualityGateConditionsMetricUuidNotNullable.class)

      // Migration of FK in LIVE_MEASURES to METRICS
      .add(3540, "Copy 'LIVE_MEASURES' table to 'LIVE_MEASURES_COPY'", CopyLiveMeasuresTable.class)
      .add(3541, "Drop 'LIVE_MEASURES' table", DropLiveMeasuresTable.class)
      .add(3542, "Rename table 'LIVE_MEASURES_COPY' to 'LIVE_MEASURES'", RenameLiveMeasuresCopyToLiveMeasures.class)
      .add(3543, "Add primary key on 'uuid' column of 'LIVE_MEASURES' table", AddPKeyOnUuidOfLiveMeasuresTable.class)
      .add(3544, "Add index on 'project_uuid' column of 'LIVE_MEASURES' table", AddIndexOnProjectUuidOfLiveMeasuresTable.class)
      .add(3545, "Add index on 'metric_uuid' column of 'LIVE_MEASURES' table", AddIndexOnMetricUuidOfLiveMeasuresTable.class)

      // Migration of FK in MANUAL_MEASURES to METRICS
      .add(3546, "Add 'metric_uuid' column on 'MANUAL_MEASURES' table", AddMetricUuidColumnToManualMeasures.class)
      .add(3547, "Populate 'metric_uuid' for 'MANUAL_MEASURES'", PopulateManualMeasuresMetricUuid.class)
      .add(3548, "Make 'metric_uuid' column not nullable for 'MANUAL_MEASURES'", MakeManualMeasuresMetricUuidNotNullable.class)

      // Finish migration of METRICS
      .add(3549, "Drop primary key on 'ID' column of 'METRICS' table", DropPrimaryKeyOnIdColumnOfMetricsTable.class)
      .add(3550, "Add primary key on 'UUID' column of 'METRICS' table", AddPrimaryKeyOnUuidColumnOfMetricsTable.class)
      .add(3551, "Drop column 'METRIC_ID' of 'PROJECT_MEASURES' table", DropMetricIdColumnOfProjectMeasuresTable.class)
      .add(3552, "Drop column 'METRIC_ID' of 'QUALITY_GATE_CONDITIONS' table", DropMetricIdColumnOfQualityGateConditionsTable.class)
      .add(3554, "Drop column 'METRIC_ID' of 'MANUAL_MEASURES' table", DropMetricIdColumnOfManualMeasuresTable.class)
      .add(3555, "Drop column 'ID' of 'METRICS' table", DropIdColumnOfMetricsTable.class)

      // Migration of PERMISSION_TEMPLATES table
      .add(3556, "Add 'UUID' column on 'PERMISSION_TEMPLATES' table", AddUuidColumnToPermissionTemplates.class)
      .add(3557, "Populate 'uuid' for 'PERMISSION_TEMPLATES'", PopulatePermissionTemplatesUuid.class)
      .add(3558, "Make 'uuid' column not nullable for user_tokens", MakePermissionTemplateUuidColumnNotNullable.class)

      // Migration of PERM_TEMPLATES_GROUPS FK to PERMISSION_TEMPLATES, switch from templateId to templateUuid
      .add(3559, "Add 'template_uuid' column for 'PERM_TEMPLATES_GROUPS' table", AddTemplateUuidColumnToPermTemplatesGroups.class)
      .add(3560, "Populate 'template_uuid' column for 'PERM_TEMPLATES_GROUPS' table", PopulatePermTemplatesGroupsTemplateUuidColumn.class)
      .add(3561, "Make 'template_uuid' column not nullable for 'PERM_TEMPLATES_GROUPS' table", MakePermTemplatesGroupsTemplateUuidColumnNotNullable.class)
      .add(3562, "Drop column 'template_id' of 'PERM_TEMPLATES_GROUPS' table", DropTemplateIdColumnOfPermTemplatesGroupsTable.class)

      // Migration of PERM_TEMPLATES_USERS FK to PERMISSION_TEMPLATES, switch from templateId to templateUuid
      .add(3563, "Add 'template_uuid' column for 'PERM_TEMPLATES_USERS' table", AddTemplateUuidColumnToPermTemplatesUsers.class)
      .add(3564, "Populate 'template_uuid' column for 'PERM_TEMPLATES_USERS' table", PopulatePermTemplatesUsersTemplateUuidColumn.class)
      .add(3565, "Make 'template_uuid' column not nullable for 'PERM_TEMPLATES_USERS' table", MakePermTemplatesUsersTemplateUuidColumnNotNullable.class)
      .add(3566, "Drop column 'template_id' of 'PERM_TEMPLATES_USERS' table", DropTemplateIdColumnOfPermTemplatesUsersTable.class)

      // Migration of PERM_TPL_CHARACTERISTICS FK to PERMISSION_TEMPLATES, switch from templateId to templateUuid
      .add(3567, "Add 'template_uuid' column for 'PERM_TPL_CHARACTERISTICS' table", AddTemplateUuidColumnToPermTplCharacteristics.class)
      .add(3568, "Populate 'template_uuid' column for 'PERM_TPL_CHARACTERISTICS' table", PopulatePermTplCharacteristicsTemplateUuidColumn.class)
      .add(3569, "Make 'template_uuid' column not nullable for 'PERM_TPL_CHARACTERISTICS' table", MakePermTplCharacteristicsTemplateUuidColumnNotNullable.class)
      .add(3570, "Drop unique constraint on 'template_id', 'permission_key' columns 'PERM_TPL_CHARACTERISTICS' table",
        DropUniqueIndexOnTemplateIdAndPermissionKeyColumnsOfPermTplCharacteristicsTable.class)
      .add(3571, "Add unique constraint on 'template_uuid', 'permission_key' columns 'PERM_TPL_CHARACTERISTICS' table",
        AddUniqueIndexOnTemplateUuidAndPermissionKeyColumnsOfPermTplCharacteristicsTable.class)

      .add(3572, "Drop column 'template_id' of 'PERM_TPL_CHARACTERISTICS' table", DropTemplateIdColumnOfPermTplCharacteristicsTable.class)

      .add(3573, "Drop primary key on 'ID' column of 'PERMISSION_TEMPLATES' table", DropPrimaryKeyOnIdColumnOfPermissionTemplatesTable.class)
      .add(3574, "Add primary key on 'UUID' column of 'PERMISSION_TEMPLATES' table", AddPrimaryKeyOnUuidColumnOfPermissionTemplatesTable.class)

      .add(3575, "Drop column 'ID' of 'PERMISSION_TEMPLATES' table", DropIdColumnOfPermissionTemplatesTable.class)
      .add(3576, "Drop column 'KEE' of 'PERMISSION_TEMPLATES' table", DropKeeColumnOfPermissionTemplatesTable.class)

      // Migration on RULES_PROFILES table
      .add(3577, "Add 'uuid' column for 'RULES_PROFILES'", AddUuidColumnToRulesProfilesTable.class)
      .add(3578, "Populate 'uuid' column for 'RULES_PROFILES'", PopulateRulesProfilesUuid.class)
      .add(3579, "Make 'uuid' column not nullable for 'RULES_PROFILES'", MakeRulesProfilesUuidColumnNotNullable.class)

      // Migration of ORG_QPROFILES FK to RULES_PROFILES
      .add(3580, "Populate 'rules_profile_uuid' column for 'ORG_QPROFILES' table", PopulateOrgQProfilesRulesProfileUuid.class)

      // Migration of QPROFILE_CHANGES FK to RULES_PROFILES
      .add(3581, "Populate 'rules_profile_uuid' column for 'QPROFILE_CHANGES' table", PopulateQProfileChangesRulesProfileUuid.class)

      // Migration of ACTIVE_RULES FK to RULES_PROFILES, switch from profile_id to profile_uuid
      .add(3582, "Add 'profile_uuid' column for 'ACTIVE_RULES' table", AddProfileUuidColumnToActiveRulesTable.class)
      .add(3583, "Populate 'profile_uuid' column for 'ACTIVE_RULES' table", PopulateActiveRulesProfileUuid.class)
      .add(3584, "Make 'profile_uuid' column not nullable for 'ACTIVE_RULES' table", MakeActiveRulesProfileUuidColumnNotNullable.class)

      .add(3585, "Drop unique constraint on 'profile_id', 'rule_id' columns 'ACTIVE_RULES' table", DropUniqueIndexOnProfileIdColumnOfActiveRulesTable.class)
      .add(3586, "Add unique constraint on 'profile_uuid', 'rule_id' columns 'ACTIVE_RULES' table", AddUniqueIndexOnProfileUuidColumnOfActiveRulesTable.class)

      .add(3587, "Drop column 'profile_id' of 'ACTIVE_RULES' table", DropProfileIdColumnOfActiveRulesTable.class)

      .add(3588, "Drop unique constraint on 'kee' columns 'RULES_PROFILES' table", DropUniqueIndexOnKeeColumnOfRulesProfilesTable.class)
      .add(3589, "Drop column 'kee' of 'RULES_PROFILES' table", DropKeeColumnOfRulesProfilesTable.class)

      .add(3590, "Drop primary key on 'ID' column of 'RULES_PROFILES' table", DropPrimaryKeyOnIdColumnOfRulesProfilesTable.class)
      .add(3591, "Add primary key on 'UUID' column of 'RULES_PROFILES' table", AddPrimaryKeyOnUuidColumnOfRulesProfilesTable.class)
      .add(3592, "Drop column 'ID' of 'RULES_PROFILES' table", DropIdColumnOfRulesProfilesTable.class)

      // Migration of PROPERTIES table
      .add(3593, "Add 'uuid' column for 'PROPERTIES'", AddUuidColumnToProperties.class)
      .add(3594, "Populate 'uuid' for 'PROPERTIES'", PopulatePropertiesUuid.class)
      .add(3595, "Make 'uuid' column not nullable for 'PROPERTIES'", MakePropertiesUuidColumnNotNullable.class)
      .add(3596, "Drop primary key on 'ID' column of 'PROPERTIES' table", DropPrimaryKeyOnIdColumnOfPropertiesTable.class)
      .add(3597, "Add primary key on 'UUID' column of 'PROPERTIES' table", AddPrimaryKeyOnUuidColumnOfPropertiesTable.class)
      .add(3598, "Drop column 'ID' of 'PROPERTIES' table", DropIdColumnOfPropertiesTable.class)

      // Migration of GROUPS table
      .add(3599, "Add 'UUID' column on 'GROUPS' table", AddUuidColumnToGroupsTable.class)
      .add(3600, "Populate 'uuid' for 'GROUPS'", PopulateGroupsUuid.class)
      .add(3601, "Make 'uuid' column not nullable for 'GROUPS'", MakeGroupsUuidColumnNotNullable.class)

      // Migration of FK in GROUP_ROLES to GROUPS
      .add(3602, "Add 'group_uuid' column on 'GROUP_ROLES' table", AddGroupUuidColumnToGroupRoles.class)
      .add(3603, "Populate 'group_uuid' for 'GROUP_ROLES'", PopulateGroupRolesGroupUuid.class)
      .add(3604, "Drop index on 'group_id' column of 'GROUP_ROLES' table", DropIndexOnGroupIdOfGroupRolesTable.class)
      .add(3605, "Add index on 'group_uuid' column of 'GROUP_ROLES' table", AddIndexOnGroupUuidOfGroupRolesTable.class)

      // Migration of FK in GROUPS_USERS to GROUPS
      .add(3606, "Add 'group_uuid' column on 'GROUPS_USERS' table", AddGroupUuidColumnToGroupsUsers.class)
      .add(3607, "Populate 'group_uuid' for 'GROUPS_USERS'", PopulateGroupsUsersGroupUuid.class)
      .add(3608, "Make 'group_uuid' column not nullable for 'GROUPS_USERS'", MakeGroupsUsersGroupUuidNotNullable.class)
      .add(3609, "Drop index on 'group_id' column of 'GROUPS_USERS' table", DropIndexOnGroupIdOfGroupsUsersTable.class)
      .add(3610, "Add index on 'group_uuid' column of 'GROUPS_USERS' table", AddIndexOnGroupUuidOfGroupsUsersTable.class)

      // Migration of FK in ORGANIZATIONS to GROUPS
      .add(3611, "Add 'default_group_uuid' column on 'ORGANIZATIONS' table", AddDefaultGroupUuidColumnToOrganizations.class)
      .add(3612, "Populate 'default_group_uuid' for 'ORGANIZATIONS'", PopulateOrganizationsDefaultGroupUuid.class)

      // Migration of FK in PERM_TEMPLATES_GROUPS to GROUPS
      .add(3613, "Add 'group_uuid' column on 'PERM_TEMPLATES_GROUPS' table", AddGroupUuidColumnToPermTemplatesGroups.class)
      .add(3614, "Populate 'group_uuid' for 'PERM_TEMPLATES_GROUPS'", PopulatePermTemplatesGroupsGroupUuid.class)

      // Migration of FK in QPROFILE_EDIT_GROUPS to GROUPS
      .add(3615, "Add 'group_uuid' column on 'QPROFILE_EDIT_GROUPS' table", AddGroupUuidColumnToQProfileEditGroups.class)
      .add(3616, "Populate 'group_uuid' for 'QPROFILE_EDIT_GROUPS'", PopulateQProfileEditGroupsGroupUuid.class)
      .add(3617, "Make 'group_uuid' column not nullable for 'QPROFILE_EDIT_GROUPS'", MakeQProfileEditGroupsGroupUuidNotNullable.class)
      .add(3618, "Drop index on 'group_id' column of 'QPROFILE_EDIT_GROUPS' table", DropIndexOnGroupIdOfQProfileEditGroupsTable.class)
      .add(3619, "Add index on 'group_uuid' column of 'QPROFILE_EDIT_GROUPS' table", AddIndexOnGroupUuidOfQProfileEditGroupsTable.class)

      // Finish migration of Groups
      .add(3620, "Drop primary key on 'ID' column of 'GROUPS' table", DropPrimaryKeyOnIdColumnOfGroupsTable.class)
      .add(3621, "Add primary key on 'UUID' column of 'GROUPS' table", AddPrimaryKeyOnUuidColumnOfGroupsTable.class)

      .add(3622, "Drop column 'group_id' of 'GROUP_ROLES' table", DropGroupIdColumnOfGroupRolesTable.class)
      .add(3623, "Drop column 'group_id' of 'GROUPS_USERS' table", DropGroupIdColumnOfGroupsUsersTable.class)
      .add(3624, "Drop column 'group_id' of 'ORGANIZATIONS' table", DropDefaultGroupIdColumnOfOrganizationsTable.class)
      .add(3625, "Drop column 'group_id' of 'PERM_TEMPLATES_GROUPS' table", DropGroupIdColumnOfPermTemplatesGroupsTable.class)
      .add(3626, "Drop column 'group_id' of 'QPROFILE_EDIT_GROUPS' table", DropGroupIdColumnOfQProfileEditGroupsTable.class)
      .add(3627, "Drop column 'ID' of 'GROUPS' table", DropIdColumnOfGroupsTable.class)

      // Migration of QUALITY_GATES_CONDITIONS FK to QUALITY_GATES, switch from qgate_id to qgate_uuid
      .add(3628, "Add 'qgate_uuid' column for quality gates conditions", AddQGateUuidColumnForQGateConditions.class)
      .add(3629, "Populate 'qgate_uuid' column for quality gates conditions", PopulateQGateUuidColumnForQGateConditions.class)
      .add(3630, "drop orphans quality gates conditions", DropOrphansQGateConditions.class)
      .add(3631, "Make 'qgate_uuid' column not nullable for quality gates conditions", MakeQGateUuidColumnNotNullableForQGateConditions.class)
      .add(3632, "Drop 'qgate_id' column for quality gates conditions", DropQGateIdColumnForQGateConditions.class)

      // Migrations of QUALITY_GATES table
      .add(3633, "Drop primary key on 'ID' column of 'QUALITY_GATES' table", DropPrimaryKeyOnIdColumnOfQGatesTable.class)
      .add(3634, "drop unique index on 'UUID' column of 'QUALITY_GATES' table", DropUniqueIndexOnUuidColumnOfQualityGatesTable.class)
      .add(3635, "Add primary key on 'UUID' column of 'QUALITY_GATES' table", AddPrimaryKeyOnUuidColumnOfQGatesTable.class)
      .add(3636, "Drop column 'ID' of 'QUALITY_GATES' table", DropIdColumnOfQGateTable.class)

      // Migration of FK in GROUPS_USERS to USERS
      .add(3637, "Add 'user_uuid' column on 'GROUPS_USERS' table", AddUserUuidColumnToGroupsUsers.class)
      .add(3638, "Populate 'user_uuid' for 'GROUPS_USERS'", PopulateGroupsUsersUserUuid.class)
      .add(3639, "Make 'user_uuid' column not nullable for 'GROUPS_USERS'", MakeGroupsUsersUserUuidColumnNotNullable.class)
      .add(3640, "Drop index on 'user_id' column of 'GROUPS_USERS' table", DropIndexOnUserIdOfGroupsUsersTable.class)
      .add(3641, "Add index on 'user_uuid' column of 'GROUPS_USERS' table", AddIndexOnUserUuidOfGroupsUsersTable.class)
      .add(3642, "Drop index on 'user_id', 'group_id' columns of 'GROUPS_USERS' table", DropUniqueIndexOnUserIdAndGroupIdOfGroupsUsersTable.class)
      .add(3643, "Add unique index on 'user_uuid', 'group_id' columns of 'GROUPS_USERS' table", AddUniqueIndexOnUserUuidAndGroupIdOfGroupsUsersTable.class)
      .add(3644, "Drop column on 'user_id' column of 'GROUPS_USERS' table", DropUserIdColumnOfGroupsUsersTable.class)

      // Migration of FK in ORGANIZATION_MEMBERS to USERS
      .add(3645, "Add 'user_uuid' column on 'ORGANIZATION_MEMBERS' table", AddUserUuidColumnToOrganizationMembers.class)
      .add(3646, "Populate 'user_uuid' for 'ORGANIZATION_MEMBERS'", PopulateOrganizationMembersUserUuid.class)
      .add(3647, "Make 'user_uuid' not-null for 'ORGANIZATION_MEMBERS'", MakeOrganizationMembersUserUuidColumnNotNullable.class)
      .add(3648, "Drop index on 'user_id' column of 'ORGANIZATION_MEMBERS' table", DropIndexOnUserIdOfOrganizationMembersTable.class)
      .add(3649, "Add index on 'user_uuid' column of 'ORGANIZATION_MEMBERS' table", AddIndexOnUserUuidOfOrganizationMembersTable.class)
      .add(3650, "Drop primary key on 'user_id' column of 'ORGANIZATION_MEMBERS' table", DropPrimaryKeyOnUserIdAndOrganizationUuidOfOrganizationMembersTable.class)
      .add(3651, "Add PK on 'user_uuid', 'organization_uuid' columns of 'ORGANIZATION_MEMBERS' table", AddPrimaryKeyOnUserUuidAndOrganizationUuidColumnsOfUserRolesTable.class)
      .add(3652, "Drop column on 'user_id' column of 'ORGANIZATION_MEMBERS' table", DropUserIdColumnOfOrganizationMembersTable.class)

      // Migration of FK in PERM_TEMPLATES_USERS to USERS
      .add(3653, "Add 'user_uuid' column on 'PERM_TEMPLATES_USERS' table", AddUserUuidColumnToPermTemplatesUsers.class)
      .add(3654, "Populate 'user_uuid' for 'PERM_TEMPLATES_USERS'", PopulatePermTemplatesUsersUserUuid.class)
      .add(3655, "Make 'user_uuid' not-null for 'PERM_TEMPLATES_USERS'", MakePermTemplatesUsersUserUuidColumnNotNullable.class)
      .add(3656, "Drop column on 'user_id' column of 'PERM_TEMPLATES_USERS' table", DropUserIdColumnOfPermTemplatesUsersTable.class)

      // Migration of FK in PROPERTIES to USERS
      .add(3657, "Add 'user_uuid' column on 'PROPERTIES' table", AddUserUuidColumnToPropertiesUsers.class)
      .add(3658, "Populate 'user_uuid' for 'PROPERTIES'", PopulatePropertiesUserUuid.class)
      .add(3659, "Drop column on 'user_id' column of 'PROPERTIES' table", DropUserIdColumnOfPropertiesTable.class)

      // Migration of FK in QPROFILE_EDIT_USERS to USERS
      .add(3660, "Add 'user_uuid' column on 'QPROFILE_EDIT_USERS' table", AddUserUuidColumnToQProfileEditUsers.class)
      .add(3661, "Populate 'user_uuid' for 'QPROFILE_EDIT_USERS'", PopulateQProfileEditUsersUserUuid.class)
      .add(3662, "Make 'user_uuid' not-null for 'QPROFILE_EDIT_USERS'", MakeQProfileEditUsersUserUuidColumnNotNullable.class)
      .add(3663, "Drop unique index on 'user_id','qprofile_uuid' columns of 'QPROFILE_EDIT_USERS' table", DropUniqueIndexOnUserIdAndQProfileUuidOfQProfileEditUsersTable.class)
      .add(3664, "Add unique index on 'user_uuid','qprofile_uuid' columns of 'QPROFILE_EDIT_USERS' table", AddUniqueIndexOnUserUuidAndQProfileUuidOfQProfileEditUsersTable.class)
      .add(3665, "Drop column on 'user_id' column of 'QPROFILE_EDIT_USERS' table", DropUserIdColumnOfQProfileEditUsersTable.class)

      .add(3666, "Add 'user_uuid' column on 'USER_ROLES' table", AddUserUuidColumnToUserRoles.class)
      .add(3667, "Populate 'user_uuid' for 'USER_ROLES'", PopulateUserRolesUserUuid.class)
      .add(3668, "Drop unique index on 'user_id' column of 'USER_ROLES' table", DropIndexOnUserIdOfUserRolesTable.class)
      .add(3669, "Add unique index on 'user_uuid' columns of 'USER_ROLES' table", AddIndexOnUserUuidOfUserRolesTable.class)
      .add(3670, "Drop column on 'user_id' column of 'USER_ROLES' table", DropUserIdColumnOfUserRolesTable.class)

      .add(3671, "Drop unique index on 'user_id' column of 'USERS' table", DropUniqueIndexOnUuidColumnOfUsersTable.class)
      .add(3672, "Drop PK index on 'id' column of 'USERS' table", DropPrimaryKeyOnIdColumnOfUsersTable.class)
      .add(3673, "Add PK index on 'uuid' column of 'USERS' table", AddPrimaryKeyOnUuidColumnOfUsersTable.class)
      .add(3674, "Drop 'id' column of 'USERS' table", DropIdColumnOfUsersTable.class)

      // Migration of RULES table
      .add(3675, "Add 'uuid' column for 'RULES'", AddUuidAndTemplateUuidColumnsToRules.class)
      .add(3676, "Populate 'uuid' column for 'RULES'", PopulateRulesUuid.class)
      .add(3677, "Make 'uuid' column not nullable for 'RULES'", MakeRulesUuidColumnNotNullable.class)
      .add(3678, "Populate 'templateUuid' column for 'RULES'", PopulateRulesTemplateUuid.class)
      .add(3679, "Drop column 'templateId' column for 'RULES'", DropTemplateIdColumnOfRulesTable.class)
      // Migration of RULES_METADATA FK to RULES, switch from rule_id to rule_uuid
      .add(3680, "Add 'RULE_UUID' column for 'RULES_METADATA' table", AddRuleUuidColumnToRulesMetadataTable.class)
      .add(3681, "Populate 'RULE_UUID' column for 'RULES_METADATA' table", PopulateRulesMetadataRuleUuidColumn.class)
      .add(3682, "Make 'RULE_UUID' column not nullable for 'RULES_METADATA' table", MakeRulesMetadataRuleUuidColumnNotNullable.class)
      .add(3683, "Drop primary key on 'RULE_ID' column of 'RULES_METADATA' table", DropPrimaryKeyOnIdColumnOfRulesMetadataTable.class)
      .add(3684, "Add primary key on 'RULE_UUID' column of 'RULES_METADATA' table", AddPrimaryKeyOnUuidAndOrganizationUuidColumnOfRulesMetadataTable.class)
      .add(3685, "Drop column 'RULE_ID' of 'RULES_METADATA' table", DropRuleIdColumnOfRulesMetadataTable.class)
      // Migration of RULES_PARAMETERS FK to RULES, switch from rule_id to rule_uuid
      .add(3686, "Add 'RULE_UUID' column for 'RULES_PARAMETERS' table", AddRuleUuidColumnToRulesParametersTable.class)
      .add(3687, "Populate 'RULE_UUID' column for 'RULES_PARAMETERS' table", PopulateRulesParametersRuleUuidColumn.class)
      .add(3688, "Make 'RULE_UUID' column not nullable for 'RULES_PARAMETERS' table", MakeRulesParametersRuleUuidColumnNotNullable.class)
      .add(3689, "Drop indexes on 'RULE_ID' of 'RULES_PARAMETERS' table", DropIndexesOnRuleIdColumnOfRulesParametersTable.class)
      .add(3690, "Add indexes to 'RULES_PARAMETERS' table", AddIndexesToRulesParametersTable.class)
      .add(3691, "Drop column 'RULE_ID' of 'RULES_PARAMETERS' table", DropRuleIdColumnOfRulesParametersTable.class)
      // Migration of ACTIVE_RULES FK to RULES, switch from rule_id to rule_uuid
      .add(3692, "Add 'RULE_UUID' column for 'ACTIVE_RULES' table", AddRuleUuidColumnToActiveRulesTable.class)
      .add(3693, "Populate 'RULE_UUID' column for 'ACTIVE_RULES' table", PopulateActiveRulesRuleUuidColumn.class)
      .add(3694, "Make 'RULE_UUID' column not nullable for 'ACTIVE_RULES' table", MakeActiveRulesRuleUuidColumnNotNullable.class)
      .add(3695, "Drop indexes on 'RULE_ID' of 'ACTIVE_RULES' table", DropIndexOnRuleIdColumnOfActiveRulesTable.class)
      .add(3696, "Add indexes to 'ACTIVE_RULES' table", AddIndexToActiveRulesTable.class)
      .add(3697, "Drop column 'RULE_ID' of 'ACTIVE_RULES' table", DropRuleIdColumnOfActiveRulesTable.class)
      // Migration of DEPRECATED_RULE_KEYS FK to RULES, switch from rule_id to rule_uuid
      .add(3698, "Add 'RULE_UUID' column for 'DEPRECATED_RULE_KEYS' table", AddRuleUuidColumnToDeprecatedRuleKeysTable.class)
      .add(3699, "Populate 'RULE_UUID' column for 'DEPRECATED_RULE_KEYS' table", PopulateDeprecatedRuleKeysRuleUuidColumn.class)
      .add(3700, "Make 'RULE_UUID' column not nullable for 'DEPRECATED_RULE_KEYS' table", MakeDeprecatedRuleKeysRuleUuidColumnNotNullable.class)
      .add(3701, "Drop index on 'RULE_ID' of 'DEPRECATED_RULE_KEYS' table", DropIndexOnRuleIdColumnOfDeprecatedRuleKeysTable.class)
      .add(3702, "Add index to 'DEPRECATED_RULE_KEYS' table", AddIndexToDeprecatedRuleKeysTable.class)
      .add(3703, "Drop column 'RULE_ID' of 'DEPRECATED_RULE_KEYS' table", DropRuleIdColumnOfDeprecatedRuleKeysTable.class)
      // Migration of ISSUE FK to RULES, switch from rule_id to rule_uuid
      .add(3704, "Copy 'ISSUES' table to 'ISSUES_COPY", CopyIssuesTable.class)
      .add(3705, "Drop 'ISSUES' table", DropIssuesTable.class)
      .add(3706, "Rename 'ISSUES_COPY' table to 'ISSUES'", RenameIssuesCopyToIssues.class)
      .add(3707, "Add indexes to 'ISSUES' table", AddIndexesToIssuesTable.class)
      .add(3708, "Add primary key on 'KEE' column of 'ISSUES' table", AddPrimaryKeyOnKeeColumnOfIssuesTable.class)

      // continue with RULES table cleanup
      .add(3709, "Drop primary key on 'ID' column of 'RULES' table", DropPrimaryKeyOnIdColumnOfRulesTable.class)
      .add(3710, "Add primary key on 'UUID' column of 'RULES' table", AddPrimaryKeyOnUuidColumnOfRulesTable.class)
      .add(3711, "Drop column 'ID' of 'RULES' table", DropIdColumnOfRulesTable.class)

      .add(3800, "Remove favourites for components with qualifiers 'DIR', 'FIL', 'UTS'", RemoveFilesFavouritesFromProperties.class)
      .add(3801, "Create 'SESSION_TOKENS' table", CreateSessionTokensTable.class)
      .add(3802, "Create 'SAML_MESSAGE_IDS' table", CreateSamlMessageIdsTable.class)

      .add(3803, "Add 'need_issue_sync' column to 'project_branches' table", AddProjectBranchesNeedIssueSync.class)
      .add(3804, "Populate 'need_issue_sync' of 'project_branches'", PopulateProjectBranchesNeedIssueSync.class)
      .add(3805, "Make 'need_issue_sync' of 'project_branches' not null", MakeProjectBranchesNeedIssueSyncNonNull.class)

      // Migration of ALM_SETTINGS table
      .add(3807, "Add columns 'CLIENT_ID' and 'CLIENT_SECRET' to 'ALM_SETTINGS' table", AddClientIdAndClientSecretColumns.class)


      // Removing old data from project_measures
      .add(3808, "Add index on 'metric_uuid' column of 'PROJECT_MEASURES' table", AddIndexOnMetricUuidOfProjectMeasuresTable.class)
      .add(3809, "Remove old Security Review Rating ProjectMeasures", DeleteSecurityReviewRatingProjectMeasures.class)
    ;
  }
}
