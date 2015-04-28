/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.db.migrations;

import com.google.common.collect.ImmutableList;
import org.sonar.server.db.migrations.v36.ViolationMigrationStep;
import org.sonar.server.db.migrations.v42.CompleteIssueMessageMigrationStep;
import org.sonar.server.db.migrations.v42.PackageKeysMigrationStep;
import org.sonar.server.db.migrations.v43.ConvertIssueDebtToMinutesMigrationStep;
import org.sonar.server.db.migrations.v43.DevelopmentCostMeasuresMigrationStep;
import org.sonar.server.db.migrations.v43.IssueChangelogMigrationStep;
import org.sonar.server.db.migrations.v43.NotResolvedIssuesOnRemovedComponentsMigrationStep;
import org.sonar.server.db.migrations.v43.RequirementMeasuresMigrationStep;
import org.sonar.server.db.migrations.v43.TechnicalDebtMeasuresMigrationStep;
import org.sonar.server.db.migrations.v44.ChangeLogMigrationStep;
import org.sonar.server.db.migrations.v44.ConvertProfileMeasuresMigrationStep;
import org.sonar.server.db.migrations.v44.FeedQProfileDatesMigrationStep;
import org.sonar.server.db.migrations.v44.FeedQProfileKeysMigrationStep;
import org.sonar.server.db.migrations.v44.IssueActionPlanKeyMigrationStep;
import org.sonar.server.db.migrations.v44.MeasureDataMigrationStep;
import org.sonar.server.db.migrations.v45.AddMissingRuleParameterDefaultValuesMigrationStep;
import org.sonar.server.db.migrations.v45.DeleteMeasuresOnDeletedProfilesMigrationStep;
import org.sonar.server.db.migrations.v451.AddMissingCustomRuleParametersMigrationStep;
import org.sonar.server.db.migrations.v451.DeleteUnescapedActivities;
import org.sonar.server.db.migrations.v50.FeedFileSources;
import org.sonar.server.db.migrations.v50.FeedIssueLongDates;
import org.sonar.server.db.migrations.v50.FeedSnapshotSourcesUpdatedAt;
import org.sonar.server.db.migrations.v50.InsertProjectsAuthorizationUpdatedAtMigrationStep;
import org.sonar.server.db.migrations.v50.PopulateProjectsUuidColumnsMigrationStep;
import org.sonar.server.db.migrations.v50.RemoveSortFieldFromIssueFiltersMigrationStep;
import org.sonar.server.db.migrations.v50.ReplaceIssueFiltersProjectKeyByUuid;
import org.sonar.server.db.migrations.v51.AddIssuesColumns;
import org.sonar.server.db.migrations.v51.AddNewCharacteristics;
import org.sonar.server.db.migrations.v51.CopyScmAccountsFromAuthorsToUsers;
import org.sonar.server.db.migrations.v51.DropIssuesColumns;
import org.sonar.server.db.migrations.v51.FeedAnalysisReportsLongDates;
import org.sonar.server.db.migrations.v51.FeedEventsLongDates;
import org.sonar.server.db.migrations.v51.FeedFileSourcesBinaryData;
import org.sonar.server.db.migrations.v51.FeedIssueChangesLongDates;
import org.sonar.server.db.migrations.v51.FeedIssueComponentUuids;
import org.sonar.server.db.migrations.v51.FeedIssueTags;
import org.sonar.server.db.migrations.v51.FeedIssuesLongDates;
import org.sonar.server.db.migrations.v51.FeedManualMeasuresLongDates;
import org.sonar.server.db.migrations.v51.FeedSemaphoresLongDates;
import org.sonar.server.db.migrations.v51.FeedSnapshotsLongDates;
import org.sonar.server.db.migrations.v51.FeedUsersLongDates;
import org.sonar.server.db.migrations.v51.RemovePermissionsOnModulesMigrationStep;
import org.sonar.server.db.migrations.v51.RenameComponentRelatedParamsInIssueFilters;
import org.sonar.server.db.migrations.v51.UpdateProjectsModuleUuidPath;
import org.sonar.server.db.migrations.v52.AddDependenciesColumns;
import org.sonar.server.db.migrations.v52.AddDependenciesComponentUuidColumns;
import org.sonar.server.db.migrations.v52.DropDependenciesComponentColumns;
import org.sonar.server.db.migrations.v52.FeedDependenciesComponentUuids;
import org.sonar.server.db.migrations.v52.FeedEventsComponentUuid;
import org.sonar.server.db.migrations.v52.FeedFileSourcesDataType;
import org.sonar.server.db.migrations.v52.FeedProjectLinksComponentUuid;
import org.sonar.server.db.migrations.v52.MoveProjectProfileAssociation;

import java.util.List;

public interface MigrationSteps {

  List<Class<? extends MigrationStep>> CLASSES = ImmutableList.of(
    // 3.6
    ViolationMigrationStep.class,

    // 4.2
    PackageKeysMigrationStep.class, CompleteIssueMessageMigrationStep.class,

    // 4.3
    ConvertIssueDebtToMinutesMigrationStep.class,
    IssueChangelogMigrationStep.class,
    TechnicalDebtMeasuresMigrationStep.class,
    DevelopmentCostMeasuresMigrationStep.class,
    RequirementMeasuresMigrationStep.class,
    NotResolvedIssuesOnRemovedComponentsMigrationStep.class,

    // 4.4
    IssueActionPlanKeyMigrationStep.class,
    MeasureDataMigrationStep.class,
    FeedQProfileKeysMigrationStep.class,
    FeedQProfileDatesMigrationStep.class,
    ChangeLogMigrationStep.class,
    ConvertProfileMeasuresMigrationStep.class,

    // 4.5
    AddMissingRuleParameterDefaultValuesMigrationStep.class,
    DeleteMeasuresOnDeletedProfilesMigrationStep.class,

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
    FeedSemaphoresLongDates.class,
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
    AddDependenciesComponentUuidColumns.class,
    FeedDependenciesComponentUuids.class,
    DropDependenciesComponentColumns.class,
    FeedFileSourcesDataType.class,
    AddDependenciesColumns.class
    );
}
