/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
package org.sonar.db.version;

import org.sonar.core.platform.Module;
import org.sonar.db.version.v55.CreateInitialSchema;
import org.sonar.db.version.v55.PopulateInitialSchema;
import org.sonar.db.version.v56.FixLengthOfIssuesMessageOnOracle;
import org.sonar.db.version.v56.FixTypeOfRuleTypeOnMysql;
import org.sonar.db.version.v56.RemoveUselessIndexesOnGroupRoles;
import org.sonar.db.version.v56.UpdateUsersExternalIdentityWhenEmpty;
import org.sonar.db.version.v60.AddAnalysisUuidColumnToCeActivity;
import org.sonar.db.version.v60.AddAnalysisUuidColumnToEvents;
import org.sonar.db.version.v60.AddAnalysisUuidColumnToMeasures;
import org.sonar.db.version.v60.AddBColumnsToProjects;
import org.sonar.db.version.v60.AddComponentUuidAndAnalysisUuidColumnToDuplicationsIndex;
import org.sonar.db.version.v60.AddComponentUuidColumnToMeasures;
import org.sonar.db.version.v60.AddComponentUuidColumnsToSnapshots;
import org.sonar.db.version.v60.AddIndexOnAnalysisUuidOfMeasures;
import org.sonar.db.version.v60.AddIndexOnComponentUuidOfMeasures;
import org.sonar.db.version.v60.AddLastUsedColumnToRulesProfiles;
import org.sonar.db.version.v60.AddProfileKeyToActivities;
import org.sonar.db.version.v60.AddUniqueIndexOnUuidOfSnapshots;
import org.sonar.db.version.v60.AddUserUpdatedAtToRulesProfiles;
import org.sonar.db.version.v60.AddUuidColumnToSnapshots;
import org.sonar.db.version.v60.AddUuidColumnsToProjects;
import org.sonar.db.version.v60.AddUuidColumnsToResourceIndex;
import org.sonar.db.version.v60.AddUuidPathColumnToProjects;
import org.sonar.db.version.v60.CleanEventsWithoutAnalysisUuid;
import org.sonar.db.version.v60.CleanEventsWithoutSnapshotId;
import org.sonar.db.version.v60.CleanMeasuresWithNullAnalysisUuid;
import org.sonar.db.version.v60.CleanOrphanRowsInProjects;
import org.sonar.db.version.v60.CleanOrphanRowsInResourceIndex;
import org.sonar.db.version.v60.CleanOrphanRowsInSnapshots;
import org.sonar.db.version.v60.CleanUsurperRootComponents;
import org.sonar.db.version.v60.CreatePermTemplatesCharacteristics;
import org.sonar.db.version.v60.CreateTemporaryIndicesFor1211;
import org.sonar.db.version.v60.DeleteOrphanDuplicationsIndexRowsWithoutComponentOrAnalysis;
import org.sonar.db.version.v60.DeleteOrphanMeasuresWithoutComponent;
import org.sonar.db.version.v60.DropIdColumnsFromProjects;
import org.sonar.db.version.v60.DropIdColumnsFromResourceIndex;
import org.sonar.db.version.v60.DropIdColumnsFromSnapshots;
import org.sonar.db.version.v60.DropIndexDuplicationsIndexSidFromDuplicationsIndex;
import org.sonar.db.version.v60.DropIndexEventsSnapshotIdFromEvents;
import org.sonar.db.version.v60.DropIndexOnSnapshotIdOfMeasures;
import org.sonar.db.version.v60.DropIndexProjectsRootIdFromProjects;
import org.sonar.db.version.v60.DropIndexProjectsUuidFromProjects;
import org.sonar.db.version.v60.DropIndicesOnTreeColumnsOfSnapshots;
import org.sonar.db.version.v60.DropProjectIdColumnFromMeasures;
import org.sonar.db.version.v60.DropRememberMeColumnsFromUsers;
import org.sonar.db.version.v60.DropResourceIndexRidFromResourceIndex;
import org.sonar.db.version.v60.DropSnapshotIdColumnFromCeActivity;
import org.sonar.db.version.v60.DropSnapshotIdColumnFromEvents;
import org.sonar.db.version.v60.DropSnapshotIdColumnFromMeasures;
import org.sonar.db.version.v60.DropSnapshotIdColumnsFromDuplicationsIndex;
import org.sonar.db.version.v60.DropSnapshotProjectIdFromSnapshots;
import org.sonar.db.version.v60.DropTemporaryIndicesOf1210;
import org.sonar.db.version.v60.DropTreeColumnsFromSnapshots;
import org.sonar.db.version.v60.DropTreesOfSnapshots;
import org.sonar.db.version.v60.DropUnusedMeasuresColumns;
import org.sonar.db.version.v60.FixProjectUuidOfDeveloperProjects;
import org.sonar.db.version.v60.MakeAnalysisUuidNotNullOnEvents;
import org.sonar.db.version.v60.MakeAnalysisUuidNotNullOnMeasures;
import org.sonar.db.version.v60.MakeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex;
import org.sonar.db.version.v60.MakeComponentUuidColumnsNotNullOnSnapshots;
import org.sonar.db.version.v60.MakeComponentUuidNotNullOnMeasures;
import org.sonar.db.version.v60.MakeProfileKeyNotNullOnActivities;
import org.sonar.db.version.v60.MakeUuidColumnNotNullOnSnapshots;
import org.sonar.db.version.v60.MakeUuidColumnsNotNullOnProjects;
import org.sonar.db.version.v60.MakeUuidColumnsNotNullOnResourceIndex;
import org.sonar.db.version.v60.MakeUuidPathColumnNotNullOnProjects;
import org.sonar.db.version.v60.PopulateAnalysisUuidColumnOnCeActivity;
import org.sonar.db.version.v60.PopulateAnalysisUuidOnEvents;
import org.sonar.db.version.v60.PopulateAnalysisUuidOnMeasures;
import org.sonar.db.version.v60.PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex;
import org.sonar.db.version.v60.PopulateComponentUuidColumnsOfSnapshots;
import org.sonar.db.version.v60.PopulateComponentUuidOfMeasures;
import org.sonar.db.version.v60.PopulateLastUsedColumnOfRulesProfiles;
import org.sonar.db.version.v60.PopulateProfileKeyOfActivities;
import org.sonar.db.version.v60.PopulateUserUpdatedAtOfRulesProfiles;
import org.sonar.db.version.v60.PopulateUuidColumnOnSnapshots;
import org.sonar.db.version.v60.PopulateUuidColumnsOfProjects;
import org.sonar.db.version.v60.PopulateUuidColumnsOfResourceIndex;
import org.sonar.db.version.v60.PopulateUuidPathColumnOnProjects;
import org.sonar.db.version.v60.RecreateIndexProjectsUuidFromProjects;
import org.sonar.db.version.v60.RemoveUsersPasswordWhenNotLocal;
import org.sonar.db.version.v60.TemporarilyDropIndexOfAnalysisUuidOnMeasures;
import org.sonar.db.version.v61.AddBUuidPathToProjects;
import org.sonar.db.version.v61.AddErrorColumnsToCeActivity;
import org.sonar.db.version.v61.CopyActivitiesToQprofileChanges;
import org.sonar.db.version.v61.CreateTableCeTaskInput;
import org.sonar.db.version.v61.CreateTableInternalProperties;
import org.sonar.db.version.v61.CreateTableProperties2;
import org.sonar.db.version.v61.CreateTableQprofileChanges;
import org.sonar.db.version.v61.CreateTableRuleRepositories;
import org.sonar.db.version.v61.CreateTableScannerContext;
import org.sonar.db.version.v61.DeleteProjectDashboards;
import org.sonar.db.version.v61.DeleteReportsFromCeQueue;
import org.sonar.db.version.v61.DropIsGlobalFromDashboards;
import org.sonar.db.version.v61.DropTableActivities;
import org.sonar.db.version.v61.DropTableProperties;
import org.sonar.db.version.v61.PopulateTableProperties2;
import org.sonar.db.version.v61.RemoveViewsDefinitionFromProperties;
import org.sonar.db.version.v61.RenameTableProperties2ToProperties;
import org.sonar.db.version.v61.ShrinkModuleUuidPathOfProjects;
import org.sonar.db.version.v62.AddIsRootColumnOnTableUsers;
import org.sonar.db.version.v62.AddOrganizationUuidToGroupRoles;
import org.sonar.db.version.v62.AddOrganizationUuidToGroups;
import org.sonar.db.version.v62.AddOrganizationUuidToPermissionTemplates;
import org.sonar.db.version.v62.AddOrganizationUuidToUserRoles;
import org.sonar.db.version.v62.CreateDefaultOrganization;
import org.sonar.db.version.v62.CreateTableOrganizations;
import org.sonar.db.version.v62.CreateTableWebhookDeliveries;
import org.sonar.db.version.v62.DeletePermissionShareDashboard;
import org.sonar.db.version.v62.DropIssueFiltersTables;
import org.sonar.db.version.v62.DropMeasureFiltersTables;
import org.sonar.db.version.v62.DropRelatedDashboardTables;
import org.sonar.db.version.v62.IncludeOrganizationUuidInUniqueIndexOfGroupRoles;
import org.sonar.db.version.v62.MakeOrganizationUuidNotNullOnGroupRoles;
import org.sonar.db.version.v62.MakeOrganizationUuidNotNullOnGroups;
import org.sonar.db.version.v62.MakeOrganizationUuidNotNullOnPermissionTemplates;
import org.sonar.db.version.v62.MakeOrganizationUuidNotNullOnUserRoles;
import org.sonar.db.version.v62.MakeRootColumnNotNullOnTableUsers;
import org.sonar.db.version.v62.PopulateIsRootColumnOnTableUsers;
import org.sonar.db.version.v62.PopulateOrganizationUuidOfGroupRoles;
import org.sonar.db.version.v62.PopulateOrganizationUuidOfGroups;
import org.sonar.db.version.v62.PopulateOrganizationUuidOfPermissionTemplates;
import org.sonar.db.version.v62.PopulateOrganizationUuidOfUserRoles;
import org.sonar.db.version.v62.UpdateQualityGateConditionsOnCoverage;
import org.sonar.db.version.v63.AddUuidToEvents;
import org.sonar.db.version.v63.MakeUuidNotNullOnEvents;
import org.sonar.db.version.v63.PopulateUuidColumnOfEvents;

public class MigrationStepModule extends Module {
  @Override
  protected void configureModule() {
    add(
      // 5.5
      CreateInitialSchema.class,
      PopulateInitialSchema.class,

      // 5.6
      RemoveUselessIndexesOnGroupRoles.class,
      FixTypeOfRuleTypeOnMysql.class,
      FixLengthOfIssuesMessageOnOracle.class,
      UpdateUsersExternalIdentityWhenEmpty.class,

      // 6.0
      AddUuidColumnsToResourceIndex.class,
      PopulateUuidColumnsOfResourceIndex.class,
      CleanOrphanRowsInResourceIndex.class,
      MakeUuidColumnsNotNullOnResourceIndex.class,
      DropIdColumnsFromResourceIndex.class,
      DropUnusedMeasuresColumns.class,
      AddComponentUuidColumnsToSnapshots.class,
      PopulateComponentUuidColumnsOfSnapshots.class,
      CleanOrphanRowsInSnapshots.class,
      MakeComponentUuidColumnsNotNullOnSnapshots.class,
      DropIdColumnsFromSnapshots.class,
      AddComponentUuidColumnToMeasures.class,
      PopulateComponentUuidOfMeasures.class,
      DeleteOrphanMeasuresWithoutComponent.class,
      MakeComponentUuidNotNullOnMeasures.class,
      DropProjectIdColumnFromMeasures.class,
      DropRememberMeColumnsFromUsers.class,
      AddUuidColumnsToProjects.class,
      PopulateUuidColumnsOfProjects.class,
      CleanOrphanRowsInProjects.class,
      MakeUuidColumnsNotNullOnProjects.class,
      DropIdColumnsFromProjects.class,
      AddLastUsedColumnToRulesProfiles.class,
      PopulateLastUsedColumnOfRulesProfiles.class,
      AddProfileKeyToActivities.class,
      PopulateProfileKeyOfActivities.class,
      MakeProfileKeyNotNullOnActivities.class,
      AddUserUpdatedAtToRulesProfiles.class,
      PopulateUserUpdatedAtOfRulesProfiles.class,
      CreateTemporaryIndicesFor1211.class,
      AddIndexOnComponentUuidOfMeasures.class,
      RecreateIndexProjectsUuidFromProjects.class,
      AddUniqueIndexOnUuidOfSnapshots.class,
      AddIndexOnAnalysisUuidOfMeasures.class,

      // SNAPSHOTS.UUID
      AddUuidColumnToSnapshots.class,
      PopulateUuidColumnOnSnapshots.class,
      MakeUuidColumnNotNullOnSnapshots.class,

      // CE_ACTIVITY.ANALYSIS_UUID
      AddAnalysisUuidColumnToCeActivity.class,
      PopulateAnalysisUuidColumnOnCeActivity.class,
      DropSnapshotIdColumnFromCeActivity.class,

      // UUID columns of DUPLICATION_INDEX
      AddComponentUuidAndAnalysisUuidColumnToDuplicationsIndex.class,
      PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex.class,
      DeleteOrphanDuplicationsIndexRowsWithoutComponentOrAnalysis.class,
      MakeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex.class,
      DropSnapshotIdColumnsFromDuplicationsIndex.class,

      // EVENTS.ANALYSIS_UUID
      AddAnalysisUuidColumnToEvents.class,
      PopulateAnalysisUuidOnEvents.class,
      CleanEventsWithoutAnalysisUuid.class,
      CleanEventsWithoutSnapshotId.class,
      MakeAnalysisUuidNotNullOnEvents.class,
      DropSnapshotIdColumnFromEvents.class,

      FixProjectUuidOfDeveloperProjects.class,
      // PROJECTS.UUID_PATH
      AddUuidPathColumnToProjects.class,
      PopulateUuidPathColumnOnProjects.class,
      MakeUuidPathColumnNotNullOnProjects.class,

      RemoveUsersPasswordWhenNotLocal.class,

      // PROJECT_MEASURES.ANALYSIS_UUID
      AddAnalysisUuidColumnToMeasures.class,
      PopulateAnalysisUuidOnMeasures.class,
      CleanMeasuresWithNullAnalysisUuid.class,
      MakeAnalysisUuidNotNullOnMeasures.class,

      CleanUsurperRootComponents.class,

      DropTreesOfSnapshots.class,
      DropTreeColumnsFromSnapshots.class,
      DropSnapshotIdColumnFromMeasures.class,
      AddBColumnsToProjects.class,
      CreatePermTemplatesCharacteristics.class,
      DropTemporaryIndicesOf1210.class,
      DropResourceIndexRidFromResourceIndex.class,
      DropSnapshotProjectIdFromSnapshots.class,
      DropIndexProjectsUuidFromProjects.class,
      DropIndexProjectsRootIdFromProjects.class,
      DropIndexDuplicationsIndexSidFromDuplicationsIndex.class,
      DropIndexEventsSnapshotIdFromEvents.class,
      TemporarilyDropIndexOfAnalysisUuidOnMeasures.class,
      DropIndexOnSnapshotIdOfMeasures.class,
      DropIndicesOnTreeColumnsOfSnapshots.class,

      // 6.1
      DeleteProjectDashboards.class,
      DropIsGlobalFromDashboards.class,
      CreateTableCeTaskInput.class,
      DeleteReportsFromCeQueue.class,
      ShrinkModuleUuidPathOfProjects.class,
      AddBUuidPathToProjects.class,
      AddErrorColumnsToCeActivity.class,
      CreateTableScannerContext.class,
      CreateTableInternalProperties.class,
      CreateTableProperties2.class,
      PopulateTableProperties2.class,
      RemoveViewsDefinitionFromProperties.class,
      CreateTableQprofileChanges.class,
      CopyActivitiesToQprofileChanges.class,
      CreateTableRuleRepositories.class,
      DropTableActivities.class,
      DropTableProperties.class,
      RenameTableProperties2ToProperties.class,

      // 6.2
      CreateTableOrganizations.class,
      CreateDefaultOrganization.class,
      DeletePermissionShareDashboard.class,
      AddIsRootColumnOnTableUsers.class,
      PopulateIsRootColumnOnTableUsers.class,
      MakeRootColumnNotNullOnTableUsers.class,
      AddOrganizationUuidToGroups.class,
      PopulateOrganizationUuidOfGroups.class,
      MakeOrganizationUuidNotNullOnGroups.class,
      AddOrganizationUuidToUserRoles.class,
      AddOrganizationUuidToPermissionTemplates.class,
      PopulateOrganizationUuidOfUserRoles.class,
      MakeOrganizationUuidNotNullOnUserRoles.class,
      PopulateOrganizationUuidOfPermissionTemplates.class,
      MakeOrganizationUuidNotNullOnPermissionTemplates.class,
      AddOrganizationUuidToGroupRoles.class,
      PopulateOrganizationUuidOfGroupRoles.class,
      MakeOrganizationUuidNotNullOnGroupRoles.class,
      UpdateQualityGateConditionsOnCoverage.class,
      DropRelatedDashboardTables.class,
      DropMeasureFiltersTables.class,
      DropIssueFiltersTables.class,
      CreateTableWebhookDeliveries.class,
      IncludeOrganizationUuidInUniqueIndexOfGroupRoles.class,

      // 6.3
      AddUuidToEvents.class,
      PopulateUuidColumnOfEvents.class,
      MakeUuidNotNullOnEvents.class);
  }
}
