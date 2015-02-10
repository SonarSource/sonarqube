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
import org.sonar.server.db.migrations.v36.ViolationMigration;
import org.sonar.server.db.migrations.v42.CompleteIssueMessageMigration;
import org.sonar.server.db.migrations.v42.PackageKeysMigration;
import org.sonar.server.db.migrations.v43.*;
import org.sonar.server.db.migrations.v44.*;
import org.sonar.server.db.migrations.v45.AddMissingRuleParameterDefaultValuesMigration;
import org.sonar.server.db.migrations.v45.DeleteMeasuresOnDeletedProfilesMigration;
import org.sonar.server.db.migrations.v451.AddMissingCustomRuleParametersMigration;
import org.sonar.server.db.migrations.v451.DeleteUnescapedActivities;
import org.sonar.server.db.migrations.v50.*;
import org.sonar.server.db.migrations.v51.*;

import java.util.List;

public interface DatabaseMigrations {

  List<Class<? extends DatabaseMigration>> CLASSES = ImmutableList.of(
    // 3.6
    ViolationMigration.class,

    // 4.2
    PackageKeysMigration.class, CompleteIssueMessageMigration.class,

    // 4.3
    ConvertIssueDebtToMinutesMigration.class,
    IssueChangelogMigration.class,
    TechnicalDebtMeasuresMigration.class,
    DevelopmentCostMeasuresMigration.class,
    RequirementMeasuresMigration.class,
    NotResolvedIssuesOnRemovedComponentsMigration.class,

    // 4.4
    IssueActionPlanKeyMigration.class,
    MeasureDataMigration.class,
    FeedQProfileKeysMigration.class,
    FeedQProfileDatesMigration.class,
    ChangeLogMigration.class,
    ConvertProfileMeasuresMigration.class,

    // 4.5
    AddMissingRuleParameterDefaultValuesMigration.class,
    DeleteMeasuresOnDeletedProfilesMigration.class,

    // 4.5.1
    AddMissingCustomRuleParametersMigration.class,
    DeleteUnescapedActivities.class,

    // 5.0
    InsertProjectsAuthorizationUpdatedAtMigration.class,
    PopulateProjectsUuidColumnsMigration.class,
    ReplaceIssueFiltersProjectKeyByUuid.class,
    FeedSnapshotSourcesUpdatedAt.class,
    FeedFileSources.class,
    FeedIssueLongDates.class,
    RemoveSortFieldFromIssueFiltersMigration.class,

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
    FeedProjectMeasuresLongDates.class,
    FeedManualMeasuresLongDates.class
    );
}
