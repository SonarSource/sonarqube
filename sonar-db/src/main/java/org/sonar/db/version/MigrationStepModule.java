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
import org.sonar.db.version.v451.AddMissingCustomRuleParametersMigrationStep;
import org.sonar.db.version.v451.DeleteUnescapedActivities;
import org.sonar.db.version.v50.FeedFileSources;
import org.sonar.db.version.v50.FeedIssueLongDates;
import org.sonar.db.version.v50.FeedSnapshotSourcesUpdatedAt;
import org.sonar.db.version.v50.InsertProjectsAuthorizationUpdatedAtMigrationStep;
import org.sonar.db.version.v50.PopulateProjectsUuidColumnsMigrationStep;
import org.sonar.db.version.v50.RemoveSortFieldFromIssueFiltersMigrationStep;
import org.sonar.db.version.v50.ReplaceIssueFiltersProjectKeyByUuid;
import org.sonar.db.version.v51.AddIssuesColumns;
import org.sonar.db.version.v51.AddNewCharacteristics;
import org.sonar.db.version.v51.CopyScmAccountsFromAuthorsToUsers;
import org.sonar.db.version.v51.DropIssuesColumns;
import org.sonar.db.version.v51.FeedAnalysisReportsLongDates;
import org.sonar.db.version.v51.FeedEventsLongDates;
import org.sonar.db.version.v51.FeedFileSourcesBinaryData;
import org.sonar.db.version.v51.FeedIssueChangesLongDates;
import org.sonar.db.version.v51.FeedIssueComponentUuids;
import org.sonar.db.version.v51.FeedIssueTags;
import org.sonar.db.version.v51.FeedIssuesLongDates;
import org.sonar.db.version.v51.FeedManualMeasuresLongDates;
import org.sonar.db.version.v51.FeedSnapshotsLongDates;
import org.sonar.db.version.v51.FeedUsersLongDates;
import org.sonar.db.version.v51.RemovePermissionsOnModulesMigrationStep;
import org.sonar.db.version.v51.RenameComponentRelatedParamsInIssueFilters;
import org.sonar.db.version.v51.UpdateProjectsModuleUuidPath;
import org.sonar.db.version.v52.AddManualMeasuresComponentUuidColumn;
import org.sonar.db.version.v52.FeedEventsComponentUuid;
import org.sonar.db.version.v52.FeedFileSourcesDataType;
import org.sonar.db.version.v52.FeedManualMeasuresComponentUuid;
import org.sonar.db.version.v52.FeedMetricsBooleans;
import org.sonar.db.version.v52.FeedProjectLinksComponentUuid;
import org.sonar.db.version.v52.IncreasePrecisionOfNumerics;
import org.sonar.db.version.v52.MoveProjectProfileAssociation;
import org.sonar.db.version.v52.RemoveAnalysisReportsFromActivities;
import org.sonar.db.version.v52.RemoveComponentLibraries;
import org.sonar.db.version.v52.RemoveDuplicatedComponentKeys;
import org.sonar.db.version.v52.RemoveRuleMeasuresOnIssues;
import org.sonar.db.version.v52.RemoveSnapshotLibraries;
import org.sonar.db.version.v53.FixMsSqlCollation;
import org.sonar.db.version.v53.UpdateCustomDashboardInLoadedTemplates;
import org.sonar.db.version.v54.AddUsersIdentityColumns;
import org.sonar.db.version.v54.IncreaseProjectsNameColumnsSize;
import org.sonar.db.version.v54.InsertGateAdminPermissionForEachProfileAdmin;
import org.sonar.db.version.v54.MigrateDisabledUsersToOnlyKeepLoginAndName;
import org.sonar.db.version.v54.MigrateQualityGatesConditions;
import org.sonar.db.version.v54.MigrateUsersIdentity;
import org.sonar.db.version.v54.RemoveComponentPageProperties;
import org.sonar.db.version.v54.RemovePreviewPermission;
import org.sonar.db.version.v55.AddActiveRulesLongDateColumns;
import org.sonar.db.version.v55.AddIssuesType;
import org.sonar.db.version.v55.AddRulesColumns;
import org.sonar.db.version.v55.DeleteManualIssues;
import org.sonar.db.version.v55.DeleteManualRules;
import org.sonar.db.version.v55.DeleteMeasuresWithCharacteristicId;
import org.sonar.db.version.v55.DeleteMeasuresWithRuleId;
import org.sonar.db.version.v55.DropActiveRulesDateColumns;
import org.sonar.db.version.v55.DropRulesDatesAndCharacteristics;
import org.sonar.db.version.v55.FeedActiveRulesLongDateColumns;
import org.sonar.db.version.v55.FeedIssueTypes;
import org.sonar.db.version.v55.FeedRulesLongDateColumns;
import org.sonar.db.version.v55.FeedRulesTypes;

public class MigrationStepModule extends Module {
  @Override
  protected void configureModule() {
    add(
      // 4.5.1
      AddMissingCustomRuleParametersMigrationStep.class,
      DeleteUnescapedActivities.class,

      // 5.0
      InsertProjectsAuthorizationUpdatedAtMigrationStep.class,
      PopulateProjectsUuidColumnsMigrationStep.class,
      ReplaceIssueFiltersProjectKeyByUuid.class,
      FeedSnapshotSourcesUpdatedAt.class,
      FeedFileSources.class,
      FeedIssueLongDates.class,
      RemoveSortFieldFromIssueFiltersMigrationStep.class,

      // 5.1
      FeedIssueTags.class,
      FeedUsersLongDates.class,
      RenameComponentRelatedParamsInIssueFilters.class,
      CopyScmAccountsFromAuthorsToUsers.class,
      FeedIssueChangesLongDates.class,
      FeedAnalysisReportsLongDates.class,
      UpdateProjectsModuleUuidPath.class,
      FeedIssueComponentUuids.class,
      FeedSnapshotsLongDates.class,
      FeedIssuesLongDates.class,
      FeedFileSourcesBinaryData.class,
      FeedManualMeasuresLongDates.class,
      FeedEventsLongDates.class,
      AddNewCharacteristics.class,
      RemovePermissionsOnModulesMigrationStep.class,
      AddIssuesColumns.class,
      DropIssuesColumns.class,

      // 5.2
      FeedProjectLinksComponentUuid.class,
      FeedEventsComponentUuid.class,
      MoveProjectProfileAssociation.class,
      FeedFileSourcesDataType.class,
      FeedMetricsBooleans.class,
      AddManualMeasuresComponentUuidColumn.class,
      FeedManualMeasuresComponentUuid.class,
      RemoveSnapshotLibraries.class,
      RemoveComponentLibraries.class,
      RemoveDuplicatedComponentKeys.class,
      IncreasePrecisionOfNumerics.class,
      RemoveAnalysisReportsFromActivities.class,
      RemoveRuleMeasuresOnIssues.class,

      // 5.3
      FixMsSqlCollation.class,
      UpdateCustomDashboardInLoadedTemplates.class,

      // 5.4
      InsertGateAdminPermissionForEachProfileAdmin.class,
      RemoveComponentPageProperties.class,
      AddUsersIdentityColumns.class,
      MigrateUsersIdentity.class,
      MigrateQualityGatesConditions.class,
      MigrateDisabledUsersToOnlyKeepLoginAndName.class,
      RemovePreviewPermission.class,
      IncreaseProjectsNameColumnsSize.class,

      // 5.5
      AddRulesColumns.class,
      FeedRulesLongDateColumns.class,
      DeleteMeasuresWithCharacteristicId.class,
      AddActiveRulesLongDateColumns.class,
      FeedActiveRulesLongDateColumns.class,
      AddIssuesType.class,
      FeedIssueTypes.class,
      DropRulesDatesAndCharacteristics.class,
      DropActiveRulesDateColumns.class,
      FeedRulesTypes.class,
      DeleteMeasuresWithRuleId.class,
      DeleteManualIssues.class,
      DeleteManualRules.class
    );
  }
}
