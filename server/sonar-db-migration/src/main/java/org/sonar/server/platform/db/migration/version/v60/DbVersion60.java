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
package org.sonar.server.platform.db.migration.version.v60;

import java.util.stream.Stream;
import org.sonar.server.platform.db.migration.step.MigrationStepRegistry;
import org.sonar.server.platform.db.migration.version.DbVersion;

public class DbVersion60 implements DbVersion {
  @Override
  public Stream<Object> getSupportComponents() {
    return Stream.of(
      // Migration1223
      FixProjectUuidOfDeveloperProjects.class,
      CleanUsurperRootComponents.class);
  }

  @Override
  public void addSteps(MigrationStepRegistry registry) {
    registry
      .add(1200, "Create table PERM_TPL_CHARACTERISTICS", CreatePermTemplatesCharacteristics.class)
      .add(1205, "Drop index resource_index_rid from RESOURCE_INDEX", DropResourceIndexRidFromResourceIndex.class)
      .add(1207, "Drop unused columns on PROJECT_MEASURES", DropUnusedMeasuresColumns.class)
      .add(1208, "Add columns SNAPSHOTS.*COMPONENT_UUID", AddComponentUuidColumnsToSnapshots.class)
      .add(1209, "Populate column SNAPSHOTS.*COMPONENT_UUID", PopulateComponentUuidColumnsOfSnapshots.class)
      .add(1210, "Create temporary indices for migration 1211", CreateTemporaryIndicesFor1211.class)
      .add(1211, "Clean orphan rows in SNAPSHOTS", CleanOrphanRowsInSnapshots.class)
      .add(1212, "Drop temporary indices for migration 1211", DropTemporaryIndicesOf1210.class)
      .add(1213, "Make column SNAPSHOTS.UUID not nullable", MakeComponentUuidColumnsNotNullOnSnapshots.class)
      .add(1214, "Drop columns SNAPSHOTS.SNAPSHOT_*_ID", DropSnapshotProjectIdFromSnapshots.class)
      .add(1215, "Drop columns SNAPSHOTS.*_ID", DropIdColumnsFromSnapshots.class)
      .add(1216, "Add column PROJECT_MEASURES.COMPONENT_UUID", AddComponentUuidColumnToMeasures.class)
      .add(1217, "Populate column PROJECT_MEASURES.COMPONENT_UUID", PopulateComponentUuidOfMeasures.class)
      .add(1218, "Delete orphan measures without component", DeleteOrphanMeasuresWithoutComponent.class)
      .add(1219, "Make column PROJECT_MEASURES.COMPONENT_UUID not nullable", MakeComponentUuidNotNullOnMeasures.class)
      .add(1220, "Drop column PROJECT_MEASURES.PROJECT_ID", DropProjectIdColumnFromMeasures.class)
      .add(1221, "Add index measures_component_uuid", AddIndexOnComponentUuidOfMeasures.class)
      .add(1222, "Drop columns USERS.REMEMBER_TOKEN_*", DropRememberMeColumnsFromUsers.class)
      .add(1223, "Clean orphan rows and fix incorrect data in table PROJECTS", Migration1223.class)
      .add(1224, "Add columns PROJECTS.*_UUID", AddUuidColumnsToProjects.class)
      .add(1225, "Populate columns PROJECTS.*_UUID", PopulateUuidColumnsOfProjects.class)
      .add(1226, "Clean orphan rows in table PROJECTS", CleanOrphanRowsInProjects.class)
      .add(1227, "Drop index projects_uuid", DropIndexProjectsUuidFromProjects.class)
      .add(1228, "Make columns PROJECTS.*_UUID not nullable", MakeUuidColumnsNotNullOnProjects.class)
      .add(1229, "Recreate index projects_uuid", RecreateIndexProjectsUuidFromProjects.class)
      .add(1230, "Drop index projects_root_id", DropIndexProjectsRootIdFromProjects.class)
      .add(1231, "Drop columns PROJECTS.*_ID", DropIdColumnsFromProjects.class)
      .add(1232, "Add column SNAPSHOTS.UUID", AddUuidColumnToSnapshots.class)
      .add(1233, "Populate column SNAPSHOTS.UUID", PopulateUuidColumnOnSnapshots.class)
      .add(1234, "Make column SNAPSHOTS.UUID not nullable", MakeUuidColumnNotNullOnSnapshots.class)
      .add(1235, "Add unique index analyses_uuid", AddUniqueIndexOnUuidOfSnapshots.class)
      .add(1236, "Add column CE_ACTIVITY.ANALYSIS_UUID", AddAnalysisUuidColumnToCeActivity.class)
      .add(1237, "Populate column CE_ACTIVITY.ANALYSIS_UUID", PopulateAnalysisUuidColumnOnCeActivity.class)
      .add(1238, "Drop column CE_ACTIVITY.SNAPSHOT_ID", DropSnapshotIdColumnFromCeActivity.class)
      .add(1239, "Add columns DUPLICATION_INDEX.*_UUID", AddComponentUuidAndAnalysisUuidColumnToDuplicationsIndex.class)
      .add(1240, "Populate columns DUPLICATION_INDEX.*_UUID", PopulateComponentUuidAndAnalysisUuidOfDuplicationsIndex.class)
      .add(1241, "Clean orphan rows in table DUPLICATION_INDEX", DeleteOrphanDuplicationsIndexRowsWithoutComponentOrAnalysis.class)
      .add(1242, "Make columns DUPLICATION_INDEX.*_UUID not nullable", MakeComponentUuidAndAnalysisUuidNotNullOnDuplicationsIndex.class)
      .add(1243, "Drop index duplications_index_sid", DropIndexDuplicationsIndexSidFromDuplicationsIndex.class)
      .add(1244, "Drop columns DUPLICATION_INDEX.*SNAPSHOT_ID", DropSnapshotIdColumnsFromDuplicationsIndex.class)
      .add(1246, "Add column RULES_PROFILES.LAST_USED", AddLastUsedColumnToRulesProfiles.class)
      .add(1247, "Populate column RULES_PROFILES.LAST_USED", PopulateLastUsedColumnOfRulesProfiles.class)
      .add(1248, "Add column EVENTS.ANALYSIS_UUID", AddAnalysisUuidColumnToEvents.class)
      .add(1249, "Populate column EVENTS.ANALYSIS_UUID", PopulateAnalysisUuidOnEvents.class)
      .add(1250, "Clean events without analysis_uuid", CleanEventsWithoutAnalysisUuid.class)
      .add(1251, "Clean events without snapshot_id", CleanEventsWithoutSnapshotId.class)
      .add(1252, "Make column EVENTS.ANALYSIS_UUID not nullable", MakeAnalysisUuidNotNullOnEvents.class)
      .add(1253, "Drop index events_snapshot_id", DropIndexEventsSnapshotIdFromEvents.class)
      .add(1254, "Drop columns EVENTS.SNAPSHOT_ID", DropSnapshotIdColumnFromEvents.class)
      .add(1256, "Add column PROJECTS.UUID_PATH", AddUuidPathColumnToProjects.class)
      .add(1257, "Populate column PROJECTS.UUID_PATH", PopulateUuidPathColumnOnProjects.class)
      .add(1258, "Make column PROJECTS.UUID_PATH not nullable", MakeUuidPathColumnNotNullOnProjects.class)
      .add(1259, "Remove password of non local users", RemoveUsersPasswordWhenNotLocal.class)
      .add(1260, "Add column ACTIVITIES.PROFILE_KEY", AddProfileKeyToActivities.class)
      .add(1261, "Populate column ACTIVITIES.PROFILE_KEY", PopulateProfileKeyOfActivities.class)
      .add(1262, "Make column ACTIVITIES.PROFILE_KEY not nullable", MakeProfileKeyNotNullOnActivities.class)
      .add(1263, "Add column RULES_PROFILES.USER_UPDATED_AT", AddUserUpdatedAtToRulesProfiles.class)
      .add(1264, "Populate column RULES_PROFILES.USER_UPDATED_AT", PopulateUserUpdatedAtOfRulesProfiles.class)
      .add(1265, "Add column PROJECT_MEASURES.ANALYSIS_UUID", AddAnalysisUuidColumnToMeasures.class)
      .add(1266, "Add index measures_analysis_metric", AddIndexOnAnalysisUuidOfMeasures.class)
      .add(1267, "Populate column PROJECT_MEASURES.ANALYSIS_UUID", PopulateAnalysisUuidOnMeasures.class)
      .add(1268, "Clean orphan measures", CleanMeasuresWithNullAnalysisUuid.class)
      .add(1269, "Temporary drop of index measures_analysis_metric", TemporarilyDropIndexOfAnalysisUuidOnMeasures.class)
      .add(1270, "Make column PROJECT_MEASURES.ANALYSIS_UUID not nullable", MakeAnalysisUuidNotNullOnMeasures.class)
      .add(1271, "Restore index measures_analysis_metric", AddIndexOnAnalysisUuidOfMeasures.class)
      .add(1272, "Delete snapshots but the one of root components", DropTreesOfSnapshots.class)
      .add(1273, "Drop indices on tree columns of table SNAPSHOTS", DropIndicesOnTreeColumnsOfSnapshots.class)
      .add(1274, "Drop tree columns of table SNAPSHOTS", DropTreeColumnsFromSnapshots.class)
      .add(1275, "Drop index measures_sid_metric", DropIndexOnSnapshotIdOfMeasures.class)
      .add(1276, "Drop column PROJECT_MEASURES.SNAPSHOT_ID", DropSnapshotIdColumnFromMeasures.class)
      .add(1277, "Add columns PROJECTS.B_*", AddBColumnsToProjects.class);
  }
}
