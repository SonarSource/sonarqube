/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.component;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.sonar.db.ce.CeActivityDto.Status.CANCELED;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.DESC;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

class SnapshotDaoIT {

  private final Random random = new SecureRandom();

  @RegisterExtension
  private final DbTester db = DbTester.create(System2.INSTANCE);

  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();
  private final SnapshotDao underTest = dbClient.snapshotDao();

  @Test
  void selectByUuid() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(newAnalysis(project)
      .setUuid("ABCD")
      .setStatus("P")
      .setLast(true)
      .setPeriodMode("days")
      .setPeriodParam("30")
      .setPeriodDate(1500000000001L)
      .setProjectVersion("2.1.0")
      .setBuildString("2.1.0.2336")
      .setAnalysisDate(1500000000006L)
      .setCreatedAt(1403042400000L)
      .setRevision("sha1"));

    SnapshotDto result = underTest.selectByUuid(db.getSession(), "ABCD").get();
    assertThat(result.getUuid()).isEqualTo("ABCD");
    assertThat(result.getRootComponentUuid()).isEqualTo(project.uuid());
    assertThat(result.getStatus()).isEqualTo("P");
    assertThat(result.getLast()).isTrue();
    assertThat(result.getProjectVersion()).isEqualTo("2.1.0");
    assertThat(result.getBuildString()).isEqualTo("2.1.0.2336");
    assertThat(result.getPeriodMode()).isEqualTo("days");
    assertThat(result.getPeriodModeParameter()).isEqualTo("30");
    assertThat(result.getPeriodDate()).isEqualTo(1500000000001L);
    assertThat(result.getAnalysisDate()).isEqualTo(1500000000006L);
    assertThat(result.getCreatedAt()).isEqualTo(1403042400000L);
    assertThat(result.getRevision()).isEqualTo("sha1");

    assertThat(underTest.selectByUuid(db.getSession(), "DOES_NOT_EXIST")).isNotPresent();
  }

  @Test
  void selectLastAnalysisByRootComponentUuid_returns_absent_when_no_last_snapshot() {
    Optional<SnapshotDto> snapshot = underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), "uuid_123");

    assertThat(snapshot).isNotPresent();
  }

  @Test
  void selectLastAnalysisByRootComponentUuid_returns_snapshot_flagged_as_last() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(project1, t -> t.setLast(false));
    SnapshotDto lastSnapshot1 = db.components().insertSnapshot(project1, t -> t.setLast(true));
    db.components().insertSnapshot(project1, t -> t.setLast(false));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(project2, t -> t.setLast(false));
    SnapshotDto lastSnapshot2 = db.components().insertSnapshot(project2, t -> t.setLast(true));
    db.components().insertSnapshot(project2, t -> t.setLast(false));
    db.components().insertSnapshot(project2, t -> t.setLast(false));

    assertThat(underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), project1.uuid()).get().getUuid())
      .isEqualTo(lastSnapshot1.getUuid());
    assertThat(underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), project2.uuid()).get().getUuid())
      .isEqualTo(lastSnapshot2.getUuid());
    assertThat(underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), "does not exist"))
      .isEmpty();
  }

  @Test
  void selectLastAnalysisByRootComponentUuid_returns_absent_if_only_unprocessed_snapshots() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(project1, t -> t.setLast(false));
    db.components().insertSnapshot(project1, t -> t.setLast(false));
    db.components().insertSnapshot(project1, t -> t.setLast(false));

    assertThat(underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), project1.uuid()))
      .isNotPresent();
  }

  @Test
  void selectLastAnalysesByRootComponentUuids_returns_empty_list_if_empty_input() {
    List<SnapshotDto> result = underTest.selectLastAnalysesByRootComponentUuids(dbSession, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  void selectLastAnalysesByRootComponentUuids_returns_snapshots_flagged_as_last() {
    ComponentDto firstProject = db.components().insertComponent(newPrivateProjectDto("PROJECT_UUID_1"));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(firstProject).setLast(false));
    SnapshotDto lastSnapshotOfFirstProject = dbClient.snapshotDao().insert(dbSession, newAnalysis(firstProject).setLast(true));
    ComponentDto secondProject = db.components().insertComponent(newPrivateProjectDto("PROJECT_UUID_2"));
    SnapshotDto lastSnapshotOfSecondProject = dbClient.snapshotDao().insert(dbSession, newAnalysis(secondProject).setLast(true));
    db.components().insertProjectAndSnapshot(newPrivateProjectDto());

    List<SnapshotDto> result = underTest.selectLastAnalysesByRootComponentUuids(dbSession, newArrayList(firstProject.uuid(),
      secondProject.uuid()));

    assertThat(result).extracting(SnapshotDto::getUuid).containsOnly(lastSnapshotOfFirstProject.getUuid(),
      lastSnapshotOfSecondProject.getUuid());
  }

  @Test
  void selectLastAnalysisDateByProjects_is_empty_if_no_project_passed() {
    assertThat(underTest.selectLastAnalysisDateByProjectUuids(dbSession, emptyList())).isEmpty();
  }

  @Test
  void selectLastAnalysisDateByProjects_returns_all_existing_projects_with_a_snapshot() {
    ProjectData project1 = db.components().insertPrivateProject();

    ProjectData project2 = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project2.getMainBranchComponent());
    ComponentDto branch2 = db.components().insertProjectBranch(project2.getMainBranchComponent());

    ProjectData project3 = db.components().insertPrivateProject();

    dbClient.snapshotDao().insert(dbSession, newAnalysis(project1.getMainBranchComponent()).setLast(false).setCreatedAt(2L));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(project1.getMainBranchComponent()).setLast(true).setCreatedAt(1L));

    dbClient.snapshotDao().insert(dbSession, newAnalysis(project2.getMainBranchComponent()).setLast(true).setCreatedAt(2L));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(branch2).setLast(false).setCreatedAt(5L));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(branch1).setLast(true).setCreatedAt(4L));

    List<ProjectLastAnalysisDateDto> lastAnalysisByProject = underTest.selectLastAnalysisDateByProjectUuids(dbSession,
      List.of(project1.projectUuid(), project2.projectUuid(), project3.projectUuid(), "non-existing"));

    assertThat(lastAnalysisByProject).extracting(ProjectLastAnalysisDateDto::getProjectUuid, ProjectLastAnalysisDateDto::getDate)
      .containsOnly(tuple(project1.projectUuid(), 1L), tuple(project2.projectUuid(), 4L));
  }

  @Test
  void selectLastAnalysisDateByProjects_returns_all_existing_projects_and_portfolios_with_a_snapshot() {
    ProjectData project1 = db.components().insertPrivateProject();
    ComponentDto branch1 = db.components().insertProjectBranch(project1.getMainBranchComponent());
    ComponentDto portfolio = db.components().insertPrivatePortfolio();

    dbClient.snapshotDao().insert(dbSession, newAnalysis(project1.getMainBranchComponent()).setLast(true).setCreatedAt(1L));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(portfolio).setLast(true).setCreatedAt(2L));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(branch1).setLast(true).setCreatedAt(3L));

    List<ProjectLastAnalysisDateDto> lastAnalysisByProject = underTest.selectLastAnalysisDateByProjectUuids(dbSession,
      List.of(project1.projectUuid(), portfolio.uuid()));

    assertThat(lastAnalysisByProject).extracting(ProjectLastAnalysisDateDto::getProjectUuid, ProjectLastAnalysisDateDto::getDate)
      .containsOnly(tuple(project1.projectUuid(), 3L), tuple(portfolio.uuid(), 2L));
  }

  @Test
  void selectAnalysesByQuery_all() {
    List<SnapshotDto> snapshots = IntStream.range(0, 1 + random.nextInt(5))
      .mapToObj(i -> {
        ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
        return IntStream.range(0, 1 + random.nextInt(20))
          .mapToObj(j -> db.components().insertSnapshot(project));
      })
      .flatMap(t -> t)
      .toList();

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery()))
      .extracting(SnapshotDto::getUuid)
      .containsOnly(snapshots.stream().map(SnapshotDto::getUuid).toArray(String[]::new));
  }

  @Test
  void selectAnalysesByQuery_by_component_uuid() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    List<SnapshotDto> snapshots1 = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(j -> db.components().insertSnapshot(project1))
      .toList();
    List<SnapshotDto> snapshots2 = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(j -> db.components().insertSnapshot(project2))
      .toList();

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid(project1.uuid())))
      .extracting(SnapshotDto::getUuid)
      .containsOnly(snapshots1.stream().map(SnapshotDto::getUuid).toArray(String[]::new));
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid(project2.uuid())))
      .extracting(SnapshotDto::getUuid)
      .containsOnly(snapshots2.stream().map(SnapshotDto::getUuid).toArray(String[]::new));
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid("does not exist")))
      .isEmpty();
  }

  @Test
  void selectAnalysesByQuery_sort_by_date() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    List<SnapshotDto> snapshots = IntStream.range(0, 1 + random.nextInt(20))
      .mapToObj(j -> SnapshotTesting.newAnalysis(project).setCreatedAt(1_000L + j))
      .collect(toList());
    Collections.shuffle(snapshots);
    snapshots.forEach(db.components()::insertSnapshot);

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid(project.uuid()).setSort(BY_DATE,
      ASC)))
      .extracting(SnapshotDto::getUuid)
      .containsExactly(snapshots.stream()
        .sorted(Comparator.comparingLong(SnapshotDto::getCreatedAt))
        .map(SnapshotDto::getUuid)
        .toArray(String[]::new));
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid(project.uuid()).setSort(BY_DATE,
      DESC)))
      .extracting(SnapshotDto::getUuid)
      .containsExactly(snapshots.stream()
        .sorted(Comparator.comparingLong(SnapshotDto::getCreatedAt).reversed())
        .map(SnapshotDto::getUuid)
        .toArray(String[]::new));
  }

  @Test
  void selectAnalysesByQuery_get_last() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(project1, t -> t.setLast(false));
    SnapshotDto lastSnapshot = db.components().insertSnapshot(project1, t -> t.setLast(true));
    db.components().insertSnapshot(project1, t -> t.setLast(false));

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid(project1.uuid()).setIsLast(true)))
      .extracting(SnapshotDto::getUuid)
      .containsOnly(lastSnapshot.getUuid());
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid("unknown").setIsLast(true)))
      .isEmpty();
  }

  @Test
  void selectAnalysesByQuery_get_last_supports_multiple_last_snapshots() {
    ComponentDto project1 = db.components().insertPrivateProject().getMainBranchComponent();
    db.components().insertSnapshot(project1, t -> t.setLast(false));
    SnapshotDto lastSnapshot1 = db.components().insertSnapshot(project1, t -> t.setLast(true));
    SnapshotDto lastSnapshot2 = db.components().insertSnapshot(project1, t -> t.setLast(true));
    db.components().insertSnapshot(project1, t -> t.setLast(false));

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setRootComponentUuid(project1.uuid()).setIsLast(true)))
      .extracting(SnapshotDto::getUuid)
      .containsOnly(lastSnapshot1.getUuid(), lastSnapshot2.getUuid());
  }

  @Test
  void selectOldestSnapshot() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    db.getDbClient().snapshotDao().insert(dbSession,
      newAnalysis(project).setCreatedAt(5L),
      newAnalysis(project).setCreatedAt(2L),
      newAnalysis(project).setCreatedAt(1L));
    dbSession.commit();

    Optional<SnapshotDto> dto = underTest.selectOldestAnalysis(dbSession, project.uuid());
    assertThat(dto).isNotEmpty();
    assertThat(dto.get().getCreatedAt()).isOne();

    assertThat(underTest.selectOldestAnalysis(dbSession, "blabla")).isEmpty();
  }

  @Test
  void selectFinishedByComponentUuidsAndFromDates() {
    long from = 1_500_000_000_000L;
    long otherFrom = 1_200_000_000_000L;
    ProjectData firstProject = db.components().insertPublicProject();
    ProjectData secondProject = db.components().insertPublicProject();
    ProjectData thirdProject = db.components().insertPublicProject();
    SnapshotDto finishedAnalysis = db.components().insertSnapshot(firstProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(firstProject.getMainBranchComponent().uuid(), finishedAnalysis, SUCCESS);
    SnapshotDto otherFinishedAnalysis = db.components().insertSnapshot(firstProject,
      s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from + 1_000_000L));
    insertActivity(firstProject.getMainBranchComponent().uuid(), otherFinishedAnalysis, SUCCESS);
    SnapshotDto oldAnalysis = db.components().insertSnapshot(firstProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from - 1L));
    insertActivity(firstProject.getMainBranchComponent().uuid(), oldAnalysis, SUCCESS);
    SnapshotDto analysisOnSecondProject = db.components().insertSnapshot(secondProject,
      s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(otherFrom));
    insertActivity(secondProject.getMainBranchComponent().uuid(), analysisOnSecondProject, SUCCESS);
    SnapshotDto oldAnalysisOnThirdProject = db.components().insertSnapshot(thirdProject,
      s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(otherFrom - 1L));
    insertActivity(thirdProject.getMainBranchComponent().uuid(), oldAnalysisOnThirdProject, SUCCESS);

    List<SnapshotDto> result = underTest.selectFinishedByProjectUuidsAndFromDates(dbSession,
      Arrays.asList(firstProject.projectUuid(), secondProject.projectUuid(), thirdProject.projectUuid()),
      Arrays.asList(from, otherFrom, otherFrom));

    assertThat(result).extracting(SnapshotDto::getUuid)
      .containsExactlyInAnyOrder(finishedAnalysis.getUuid(), otherFinishedAnalysis.getUuid(), analysisOnSecondProject.getUuid());
  }

  @Test
  void selectFinishedByComponentUuidsAndFromDates_returns_processed_analysis_even_if_analysis_failed() {
    long from = 1_500_000_000_000L;
    ProjectData project = db.components().insertPublicProject();
    SnapshotDto unprocessedAnalysis = db.components().insertSnapshot(project,
      s -> s.setStatus(STATUS_UNPROCESSED).setCreatedAt(from + 1_000_000L));
    insertActivity(project.getMainBranchComponent().uuid(), unprocessedAnalysis, CANCELED);
    SnapshotDto finishedAnalysis = db.components().insertSnapshot(project, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.getMainBranchComponent().uuid(), finishedAnalysis, SUCCESS);
    SnapshotDto canceledAnalysis = db.components().insertSnapshot(project, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.getMainBranchComponent().uuid(), canceledAnalysis, CANCELED);

    List<SnapshotDto> result = underTest.selectFinishedByProjectUuidsAndFromDates(dbSession,
      singletonList(project.getProjectDto().getUuid()), singletonList(from));

    assertThat(result).extracting(SnapshotDto::getUuid).containsExactlyInAnyOrder(finishedAnalysis.getUuid(), canceledAnalysis.getUuid());
  }

  @Test
  void selectFinishedByComponentUuidsAndFromDates_return_branches_analysis() {
    long from = 1_500_000_000_000L;
    ProjectData project = db.components().insertPublicProject();
    ComponentDto firstBranch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto secondBranch = db.components().insertProjectBranch(project.getMainBranchComponent());
    SnapshotDto finishedAnalysis = db.components().insertSnapshot(firstBranch, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.getProjectDto().getUuid(), finishedAnalysis, SUCCESS);
    SnapshotDto otherFinishedAnalysis = db.components().insertSnapshot(firstBranch,
      s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from + 1_000_000L));
    insertActivity(project.getProjectDto().getUuid(), otherFinishedAnalysis, SUCCESS);
    SnapshotDto oldAnalysis = db.components().insertSnapshot(firstBranch, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from - 1L));
    insertActivity(project.getProjectDto().getUuid(), oldAnalysis, SUCCESS);
    SnapshotDto analysisOnSecondBranch = db.components().insertSnapshot(secondBranch,
      s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.getProjectDto().getUuid(), analysisOnSecondBranch, SUCCESS);

    List<SnapshotDto> result = underTest.selectFinishedByProjectUuidsAndFromDates(dbSession,
      singletonList(project.getProjectDto().getUuid()),
      singletonList(from));

    assertThat(result).extracting(SnapshotDto::getUuid)
      .containsExactlyInAnyOrder(finishedAnalysis.getUuid(), otherFinishedAnalysis.getUuid(), analysisOnSecondBranch.getUuid());
  }

  @Test
  void selectSnapshotBefore() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis1 = newAnalysis(project).setCreatedAt(50L).setPeriodDate(20L);
    SnapshotDto analysis2 = newAnalysis(project).setCreatedAt(20L).setPeriodDate(10L);
    SnapshotDto analysis3 = newAnalysis(project).setCreatedAt(10L).setPeriodDate(null);
    db.components().insertSnapshots(analysis1, analysis2, analysis3);

    assertThat(underTest.selectSnapshotBefore(project.uuid(), 50L, dbSession))
      .extracting(ViewsSnapshotDto::getUuid, ViewsSnapshotDto::getCreatedAt, ViewsSnapshotDto::getLeakDate)
      .containsExactlyInAnyOrder(analysis2.getUuid(), analysis2.getCreatedAt(), analysis2.getPeriodDate());

    assertThat(underTest.selectSnapshotBefore(project.uuid(), 20L, dbSession))
      .extracting(ViewsSnapshotDto::getUuid, ViewsSnapshotDto::getLeakDate)
      .containsExactlyInAnyOrder(analysis3.getUuid(), null);

    assertThat(underTest.selectSnapshotBefore(project.uuid(), 5L, dbSession)).isNull();
  }

  @Test
  void insert() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    SnapshotDto dto = underTest.insert(db.getSession(), newAnalysis(project)
      .setStatus("P")
      .setLast(true)
      .setPeriodMode("days")
      .setPeriodParam("30")
      .setPeriodDate(1500000000001L)
      .setProjectVersion("2.1-SNAPSHOT")
      .setAnalysisDate(1500000000006L)
      .setCreatedAt(1403042400000L));

    assertThat(dto.getUuid()).isNotNull();
    assertThat(dto.getRootComponentUuid()).isEqualTo(project.uuid());
    assertThat(dto.getStatus()).isEqualTo("P");
    assertThat(dto.getLast()).isTrue();
    assertThat(dto.getPeriodMode()).isEqualTo("days");
    assertThat(dto.getPeriodModeParameter()).isEqualTo("30");
    assertThat(dto.getPeriodDate()).isEqualTo(1500000000001L);
    assertThat(dto.getAnalysisDate()).isEqualTo(1500000000006L);
    assertThat(dto.getCreatedAt()).isEqualTo(1403042400000L);
    assertThat(dto.getProjectVersion()).isEqualTo("2.1-SNAPSHOT");
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyNonEmptyStrings")
  void insert_with_null_and_empty_and_non_empty_projectVersion(@Nullable String projectVersion) {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    SnapshotDto dto = underTest.insert(db.getSession(), newAnalysis(project).setProjectVersion(projectVersion));

    assertThat(dto.getProjectVersion()).isEqualTo(projectVersion);
  }

  @ParameterizedTest
  @MethodSource("nullAndEmptyNonEmptyStrings")
  void insert_with_null_and_empty_and_non_empty_buildString(@Nullable String buildString) {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    SnapshotDto dto = underTest.insert(db.getSession(), newAnalysis(project).setBuildString(buildString));

    assertThat(dto.getBuildString()).isEqualTo(buildString);
  }

  static Object[][] nullAndEmptyNonEmptyStrings() {
    return new Object[][]{
      {null},
      {""},
      {secure().nextAlphanumeric(7)},
    };
  }

  @Test
  void insert_snapshots() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    underTest.insert(db.getSession(),
      newAnalysis(project).setLast(false).setUuid("u5"),
      newAnalysis(project).setLast(false).setUuid("u6"));
    db.getSession().commit();

    assertThat(db.countRowsOfTable("snapshots")).isEqualTo(2);
  }

  @Test
  void isLast_is_true_when_no_previous_snapshot() {
    SnapshotDto snapshot = defaultSnapshot();

    boolean isLast = SnapshotDao.isLast(snapshot, null);

    assertThat(isLast).isTrue();
  }

  @Test
  void isLast_is_true_when_previous_snapshot_is_older() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    SnapshotDto snapshot = defaultSnapshot().setCreatedAt(today.getTime());
    SnapshotDto previousLastSnapshot = defaultSnapshot().setCreatedAt(yesterday.getTime());

    boolean isLast = SnapshotDao.isLast(snapshot, previousLastSnapshot);

    assertThat(isLast).isTrue();
  }

  @Test
  void isLast_is_false_when_previous_snapshot_is_newer() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    SnapshotDto snapshot = defaultSnapshot().setCreatedAt(yesterday.getTime());
    SnapshotDto previousLastSnapshot = defaultSnapshot().setCreatedAt(today.getTime());

    boolean isLast = SnapshotDao.isLast(snapshot, previousLastSnapshot);

    assertThat(isLast).isFalse();
  }

  @Test
  void switchIsLastFlagAndSetProcessedStatus() {
    insertAnalysis("P1", "A1", SnapshotDto.STATUS_PROCESSED, true);
    insertAnalysis("P1", "A2", SnapshotDto.STATUS_UNPROCESSED, false);
    insertAnalysis("P2", "A3", SnapshotDto.STATUS_PROCESSED, true);
    db.commit();

    underTest.switchIsLastFlagAndSetProcessedStatus(db.getSession(), "P1", "A2");

    verifyStatusAndIsLastFlag("A1", SnapshotDto.STATUS_PROCESSED, false);
    verifyStatusAndIsLastFlag("A2", SnapshotDto.STATUS_PROCESSED, true);
    // other project is untouched
    verifyStatusAndIsLastFlag("A3", SnapshotDto.STATUS_PROCESSED, true);
  }

  @Test
  void update() {
    SnapshotDto analysis = insertAnalysis("P1", "A1", STATUS_PROCESSED, false);
    db.commit();
    analysis
      .setRootComponentUuid("P42")
      .setProjectVersion("5.6.3")
      .setStatus(STATUS_UNPROCESSED);

    underTest.update(dbSession, analysis);

    SnapshotDto result = underTest.selectByUuid(dbSession, "A1").get();
    assertThat(result.getProjectVersion()).isEqualTo("5.6.3");
    assertThat(result.getStatus()).isEqualTo(STATUS_UNPROCESSED);
    assertThat(result.getRootComponentUuid()).isEqualTo("P1");
  }

  private SnapshotDto insertAnalysis(String projectUuid, String uuid, String status, boolean isLastFlag) {
    SnapshotDto snapshot = newAnalysis(newPrivateProjectDto(projectUuid))
      .setLast(isLastFlag)
      .setStatus(status)
      .setUuid(uuid);
    underTest.insert(db.getSession(), snapshot);
    return snapshot;
  }

  private void verifyStatusAndIsLastFlag(String uuid, String expectedStatus, boolean expectedLastFlag) {
    Optional<SnapshotDto> analysis = underTest.selectByUuid(db.getSession(), uuid);
    assertThat(analysis.get().getStatus()).isEqualTo(expectedStatus);
    assertThat(analysis.get().getLast()).isEqualTo(expectedLastFlag);
  }

  private static SnapshotDto defaultSnapshot() {
    return new SnapshotDto()
      .setUuid("u1")
      .setRootComponentUuid("uuid_3")
      .setStatus("P")
      .setLast(true)
      .setProjectVersion("2.1-SNAPSHOT")
      .setPeriodMode("days1")
      .setPeriodParam("30")
      .setPeriodDate(1_500_000_000_001L)
      .setAnalysisDate(1_500_000_000_006L);
  }

  private CeActivityDto insertActivity(String projectUuid, SnapshotDto analysis, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(projectUuid);
    queueDto.setUuid(secure().nextAlphanumeric(40));
    queueDto.setCreatedAt(random.nextLong(Long.MAX_VALUE));
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(random.nextLong(10000));
    activityDto.setExecutedAt(random.nextLong(Long.MAX_VALUE));
    activityDto.setAnalysisUuid(analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }
}
