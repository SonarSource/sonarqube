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
package org.sonar.db.component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.utils.DateUtils;
import org.sonar.api.utils.System2;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.organization.OrganizationTesting;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.assertj.core.api.Assertions.assertThat;
import static org.sonar.db.ce.CeActivityDto.Status.CANCELED;
import static org.sonar.db.ce.CeActivityDto.Status.SUCCESS;
import static org.sonar.db.component.ComponentTesting.newPrivateProjectDto;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotQuery.SORT_FIELD.BY_DATE;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.ASC;
import static org.sonar.db.component.SnapshotQuery.SORT_ORDER.DESC;
import static org.sonar.db.component.SnapshotTesting.newAnalysis;

public class SnapshotDaoTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public DbTester db = DbTester.create(System2.INSTANCE);

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();

  private SnapshotDao underTest = dbClient.snapshotDao();

  @Test
  public void test_selectByUuid() {
    ComponentDto project = db.components().insertPrivateProject();
    db.components().insertSnapshot(newAnalysis(project)
      .setUuid("ABCD")
      .setStatus("P")
      .setLast(true)
      .setPurgeStatus(1)
      .setPeriodMode("days")
      .setPeriodParam("30")
      .setPeriodDate(1500000000001L)
      .setVersion("2.1-SNAPSHOT")
      .setBuildDate(1500000000006L)
      .setCreatedAt(1403042400000L));

    SnapshotDto result = underTest.selectByUuid(db.getSession(), "ABCD").get();
    assertThat(result.getId()).isNotNull();
    assertThat(result.getUuid()).isEqualTo("ABCD");
    assertThat(result.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(result.getStatus()).isEqualTo("P");
    assertThat(result.getLast()).isTrue();
    assertThat(result.getPurgeStatus()).isEqualTo(1);
    assertThat(result.getVersion()).isEqualTo("2.1-SNAPSHOT");
    assertThat(result.getPeriodMode()).isEqualTo("days");
    assertThat(result.getPeriodModeParameter()).isEqualTo("30");
    assertThat(result.getPeriodDate()).isEqualTo(1500000000001L);
    assertThat(result.getBuildDate()).isEqualTo(1500000000006L);
    assertThat(result.getCreatedAt()).isEqualTo(1403042400000L);
    assertThat(result.getVersion()).isEqualTo("2.1-SNAPSHOT");

    assertThat(underTest.selectByUuid(db.getSession(), "DOES_NOT_EXIST").isPresent()).isFalse();
  }

  @Test
  public void selectLastSnapshotByRootComponentUuid_returns_absent_when_no_last_snapshot() {
    Optional<SnapshotDto> snapshot = underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), "uuid_123");

    assertThat(snapshot).isNotPresent();
  }

  @Test
  public void selectLastSnapshotByRootComponentUuid_returns_snapshot_flagged_as_last() {
    db.prepareDbUnit(getClass(), "snapshots.xml");

    Optional<SnapshotDto> snapshot = underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), "uuid_2");

    assertThat(snapshot.get().getId()).isEqualTo(4L);
  }

  @Test
  public void selectLastSnapshotByRootComponentUuid_returns_absent_if_only_unprocessed_snapshots() {
    db.prepareDbUnit(getClass(), "snapshots.xml");

    Optional<SnapshotDto> snapshot = underTest.selectLastAnalysisByRootComponentUuid(db.getSession(), "uuid_5");

    assertThat(snapshot).isNotPresent();
  }

  @Test
  public void selectLastSnapshotsByRootComponentUuids_returns_empty_list_if_empty_input() {
    List<SnapshotDto> result = underTest.selectLastAnalysesByRootComponentUuids(dbSession, emptyList());

    assertThat(result).isEmpty();
  }

  @Test
  public void selectLastSnapshotsByRootComponentUuids_returns_snapshots_flagged_as_last() {
    ComponentDto firstProject = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "PROJECT_UUID_1"));
    dbClient.snapshotDao().insert(dbSession, newAnalysis(firstProject).setLast(false));
    SnapshotDto lastSnapshotOfFirstProject = dbClient.snapshotDao().insert(dbSession, newAnalysis(firstProject).setLast(true));
    ComponentDto secondProject = db.components().insertComponent(newPrivateProjectDto(db.getDefaultOrganization(), "PROJECT_UUID_2"));
    SnapshotDto lastSnapshotOfSecondProject = dbClient.snapshotDao().insert(dbSession, newAnalysis(secondProject).setLast(true));
    db.components().insertProjectAndSnapshot(newPrivateProjectDto(db.getDefaultOrganization()));

    List<SnapshotDto> result = underTest.selectLastAnalysesByRootComponentUuids(dbSession, newArrayList(firstProject.uuid(), secondProject.uuid()));

    assertThat(result).extracting(SnapshotDto::getId).containsOnly(lastSnapshotOfFirstProject.getId(), lastSnapshotOfSecondProject.getId());
  }

  @Test
  public void select_snapshots_by_query() {
    db.prepareDbUnit(getClass(), "select_snapshots_by_query.xml");

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery())).hasSize(6);

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("ABCD").setSort(BY_DATE, ASC)).get(0).getId()).isEqualTo(1L);
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("ABCD").setSort(BY_DATE, DESC)).get(0).getId()).isEqualTo(3L);
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("ABCD"))).hasSize(3);
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("UNKOWN"))).isEmpty();
    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("GHIJ"))).isEmpty();
  }

  @Test
  public void select_snapshot_by_query() {
    db.prepareDbUnit(getClass(), "select_snapshots_by_query.xml");

    assertThat(underTest.selectAnalysesByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("ABCD").setIsLast(true))).isNotNull();
    assertThat(underTest.selectAnalysisByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("UNKOWN"))).isNull();
    assertThat(underTest.selectAnalysisByQuery(db.getSession(), new SnapshotQuery().setComponentUuid("GHIJ"))).isNull();
  }

  @Test
  public void fail_with_ISE_to_select_snapshot_by_query_when_more_than_one_result() {
    db.prepareDbUnit(getClass(), "select_snapshots_by_query.xml");

    expectedException.expect(IllegalStateException.class);
    expectedException.expectMessage("Expected one analysis to be returned, got 6");

    underTest.selectAnalysisByQuery(db.getSession(), new SnapshotQuery());
  }

  @Test
  public void select_previous_version_snapshots() {
    db.prepareDbUnit(getClass(), "select_previous_version_snapshots.xml");

    List<SnapshotDto> snapshots = underTest.selectPreviousVersionSnapshots(db.getSession(), "ABCD", "1.2-SNAPSHOT");
    assertThat(snapshots).hasSize(2);

    SnapshotDto firstSnapshot = snapshots.get(0);
    assertThat(firstSnapshot.getVersion()).isEqualTo("1.1");

    // All snapshots are returned on an unknown version
    assertThat(underTest.selectPreviousVersionSnapshots(db.getSession(), "ABCD", "UNKNOWN")).hasSize(3);
  }

  @Test
  public void select_first_snapshots() {
    ComponentDto project = newPrivateProjectDto(db.getDefaultOrganization());
    db.getDbClient().componentDao().insert(dbSession, project);

    db.getDbClient().snapshotDao().insert(dbSession,
      newAnalysis(project).setCreatedAt(5L),
      newAnalysis(project).setCreatedAt(2L),
      newAnalysis(project).setCreatedAt(1L));
    dbSession.commit();

    SnapshotDto dto = underTest.selectOldestSnapshot(dbSession, project.uuid());
    assertThat(dto).isNotNull();
    assertThat(dto.getCreatedAt()).isEqualTo(1L);

    assertThat(underTest.selectOldestSnapshot(dbSession, "blabla")).isNull();
  }

  @Test
  public void selectFinishedByComponentUuidsAndFromDates() {
    long from = 1_500_000_000_000L;
    long otherFrom = 1_200_000_000_000L;
    ComponentDto firstProject = db.components().insertMainBranch();
    ComponentDto secondProject = db.components().insertMainBranch();
    ComponentDto thirdProject = db.components().insertMainBranch();
    SnapshotDto finishedAnalysis = db.components().insertSnapshot(firstProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(firstProject.uuid(), finishedAnalysis, SUCCESS);
    SnapshotDto otherFinishedAnalysis = db.components().insertSnapshot(firstProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from + 1_000_000L));
    insertActivity(firstProject.uuid(), otherFinishedAnalysis, SUCCESS);
    SnapshotDto oldAnalysis = db.components().insertSnapshot(firstProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from - 1L));
    insertActivity(firstProject.uuid(), oldAnalysis, SUCCESS);
    SnapshotDto analysisOnSecondProject = db.components().insertSnapshot(secondProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(otherFrom));
    insertActivity(secondProject.uuid(), analysisOnSecondProject, SUCCESS);
    SnapshotDto oldAnalysisOnThirdProject = db.components().insertSnapshot(thirdProject, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(otherFrom - 1L));
    insertActivity(thirdProject.uuid(), oldAnalysisOnThirdProject, SUCCESS);

    List<SnapshotDto> result = underTest.selectFinishedByComponentUuidsAndFromDates(dbSession,
      Arrays.asList(firstProject.uuid(), secondProject.uuid(), thirdProject.uuid()),
      Arrays.asList(from, otherFrom, otherFrom));

    assertThat(result).extracting(SnapshotDto::getUuid)
      .containsExactlyInAnyOrder(finishedAnalysis.getUuid(), otherFinishedAnalysis.getUuid(), analysisOnSecondProject.getUuid());
  }

  @Test
  public void selectFinishedByComponentUuidsAndFromDates_ignores_unsuccessful_analysis() {
    long from = 1_500_000_000_000L;
    ComponentDto project = db.components().insertMainBranch();
    SnapshotDto unprocessedAnalysis = db.components().insertSnapshot(project, s -> s.setStatus(STATUS_UNPROCESSED).setCreatedAt(from + 1_000_000L));
    insertActivity(project.uuid(), unprocessedAnalysis, CANCELED);
    SnapshotDto finishedAnalysis = db.components().insertSnapshot(project, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.uuid(), finishedAnalysis, SUCCESS);
    SnapshotDto canceledAnalysis = db.components().insertSnapshot(project, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.uuid(), canceledAnalysis, CANCELED);

    List<SnapshotDto> result = underTest.selectFinishedByComponentUuidsAndFromDates(dbSession, singletonList(project.uuid()), singletonList(from));

    assertThat(result).extracting(SnapshotDto::getUuid)
      .containsExactlyInAnyOrder(finishedAnalysis.getUuid());
  }

  @Test
  public void selectFinishedByComponentUuidsAndFromDates_return_branches_analysis() {
    long from = 1_500_000_000_000L;
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto firstBranch = db.components().insertProjectBranch(project);
    ComponentDto secondBranch = db.components().insertProjectBranch(project);
    SnapshotDto finishedAnalysis = db.components().insertSnapshot(firstBranch, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.uuid(), finishedAnalysis, SUCCESS);
    SnapshotDto otherFinishedAnalysis = db.components().insertSnapshot(firstBranch, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from + 1_000_000L));
    insertActivity(project.uuid(), otherFinishedAnalysis, SUCCESS);
    SnapshotDto oldAnalysis = db.components().insertSnapshot(firstBranch, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from - 1L));
    insertActivity(project.uuid(), oldAnalysis, SUCCESS);
    SnapshotDto analysisOnSecondBranch = db.components().insertSnapshot(secondBranch, s -> s.setStatus(STATUS_PROCESSED).setCreatedAt(from));
    insertActivity(project.uuid(), analysisOnSecondBranch, SUCCESS);

    List<SnapshotDto> result = underTest.selectFinishedByComponentUuidsAndFromDates(dbSession, singletonList(project.uuid()), singletonList(from));

    assertThat(result).extracting(SnapshotDto::getUuid)
      .containsExactlyInAnyOrder(finishedAnalysis.getUuid(), otherFinishedAnalysis.getUuid(), analysisOnSecondBranch.getUuid());
  }

  @Test
  public void selectSnapshotBefore() {
    ComponentDto project = db.components().insertPrivateProject();
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
  public void insert() {
    ComponentDto project = db.components().insertPrivateProject();

    SnapshotDto dto = underTest.insert(db.getSession(), newAnalysis(project)
      .setStatus("P")
      .setLast(true)
      .setPurgeStatus(1)
      .setPeriodMode("days")
      .setPeriodParam("30")
      .setPeriodDate(1500000000001L)
      .setVersion("2.1-SNAPSHOT")
      .setBuildDate(1500000000006L)
      .setCreatedAt(1403042400000L));

    assertThat(dto.getId()).isNotNull();
    assertThat(dto.getUuid()).isNotNull();
    assertThat(dto.getComponentUuid()).isEqualTo(project.uuid());
    assertThat(dto.getStatus()).isEqualTo("P");
    assertThat(dto.getLast()).isTrue();
    assertThat(dto.getPurgeStatus()).isEqualTo(1);
    assertThat(dto.getPeriodMode()).isEqualTo("days");
    assertThat(dto.getPeriodModeParameter()).isEqualTo("30");
    assertThat(dto.getPeriodDate()).isEqualTo(1500000000001L);
    assertThat(dto.getBuildDate()).isEqualTo(1500000000006L);
    assertThat(dto.getCreatedAt()).isEqualTo(1403042400000L);
    assertThat(dto.getVersion()).isEqualTo("2.1-SNAPSHOT");

  }

  @Test
  public void insert_snapshots() {
    ComponentDto project = db.components().insertPrivateProject();

    underTest.insert(db.getSession(),
      newAnalysis(project).setLast(false).setUuid("u5"),
      newAnalysis(project).setLast(false).setUuid("u6"));
    db.getSession().commit();

    assertThat(db.countRowsOfTable("snapshots")).isEqualTo(2);
  }

  @Test
  public void is_last_snapshot_when_no_previous_snapshot() {
    SnapshotDto snapshot = defaultSnapshot();

    boolean isLast = SnapshotDao.isLast(snapshot, null);

    assertThat(isLast).isTrue();
  }

  @Test
  public void is_last_snapshot_when_previous_snapshot_is_older() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    SnapshotDto snapshot = defaultSnapshot().setCreatedAt(today.getTime());
    SnapshotDto previousLastSnapshot = defaultSnapshot().setCreatedAt(yesterday.getTime());

    boolean isLast = SnapshotDao.isLast(snapshot, previousLastSnapshot);

    assertThat(isLast).isTrue();
  }

  @Test
  public void is_not_last_snapshot_when_previous_snapshot_is_newer() {
    Date today = new Date();
    Date yesterday = DateUtils.addDays(today, -1);

    SnapshotDto snapshot = defaultSnapshot().setCreatedAt(yesterday.getTime());
    SnapshotDto previousLastSnapshot = defaultSnapshot().setCreatedAt(today.getTime());

    boolean isLast = SnapshotDao.isLast(snapshot, previousLastSnapshot);

    assertThat(isLast).isFalse();
  }

  @Test
  public void switchIsLastFlagAndSetProcessedStatus() {
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
  public void update() {
    SnapshotDto analysis = insertAnalysis("P1", "A1", STATUS_PROCESSED, false);
    db.commit();
    analysis
      .setComponentUuid("P42")
      .setVersion("5.6.3")
      .setStatus(STATUS_UNPROCESSED);

    underTest.update(dbSession, analysis);

    SnapshotDto result = underTest.selectByUuid(dbSession, "A1").get();
    assertThat(result.getVersion()).isEqualTo("5.6.3");
    assertThat(result.getStatus()).isEqualTo(STATUS_UNPROCESSED);
    assertThat(result.getComponentUuid()).isEqualTo("P1");
  }

  private SnapshotDto insertAnalysis(String projectUuid, String uuid, String status, boolean isLastFlag) {
    SnapshotDto snapshot = newAnalysis(newPrivateProjectDto(OrganizationTesting.newOrganizationDto(), projectUuid))
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
      .setComponentUuid("uuid_3")
      .setStatus("P")
      .setLast(true)
      .setPurgeStatus(1)
      .setVersion("2.1-SNAPSHOT")
      .setPeriodMode("days1")
      .setPeriodParam("30")
      .setPeriodDate(1_500_000_000_001L)
      .setBuildDate(1_500_000_000_006L);
  }

  private CeActivityDto insertActivity(String projectUuid, SnapshotDto analysis, CeActivityDto.Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setTaskType(CeTaskTypes.REPORT);
    queueDto.setComponentUuid(projectUuid);
    queueDto.setUuid(randomAlphanumeric(40));
    queueDto.setCreatedAt(nextLong());
    CeActivityDto activityDto = new CeActivityDto(queueDto);
    activityDto.setStatus(status);
    activityDto.setExecutionTimeMs(nextLong());
    activityDto.setExecutedAt(nextLong());
    activityDto.setAnalysisUuid(analysis.getUuid());
    db.getDbClient().ceActivityDao().insert(db.getSession(), activityDto);
    db.commit();
    return activityDto;
  }
}
