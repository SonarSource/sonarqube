/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.server.issue.index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.event.Level;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.ce.queue.CeQueue;
import org.sonar.ce.queue.CeTaskSubmit;
import org.sonar.core.ce.CeTaskCharacteristics;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.core.util.UuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeActivityDto.Status;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.project.ProjectDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH_TYPE;
import static org.sonar.db.ce.CeTaskTypes.BRANCH_ISSUE_SYNC;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.db.component.BranchType.PULL_REQUEST;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;

public class AsyncIssueIndexingImplTest {

  @Rule
  public DbTester dbTester = DbTester.create(System2.INSTANCE);
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = dbTester.getDbClient();
  private final CeQueue ceQueue = mock(CeQueue.class);
  private final UuidFactory uuidFactory = new SequenceUuidFactory();
  private final AsyncIssueIndexCreationTelemetry asyncIssueIndexCreationTelemetry = mock();

  private final AsyncIssueIndexingImpl underTest = new AsyncIssueIndexingImpl(ceQueue, dbClient, asyncIssueIndexCreationTelemetry);

  @Before
  public void before() {
    when(ceQueue.prepareSubmit()).thenReturn(new CeTaskSubmit.Builder(uuidFactory.create()));
  }

  @Test
  public void triggerOnIndexCreation() {
    BranchDto dto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branchName")
      .setUuid("branch_uuid")
      .setProjectUuid("project_uuid")
      .setIsMain(false);
    dbClient.branchDao().insert(dbTester.getSession(), dto);
    dbTester.commit();

    underTest.triggerOnIndexCreation();

    Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbTester.getSession(), "branch_uuid");
    assertThat(branch).isPresent();
    assertThat(branch.get().isNeedIssueSync()).isTrue();
    verify(ceQueue, times(1)).prepareSubmit();
    verify(ceQueue, times(1)).massSubmit(anyCollection());
    assertThat(logTester.logs(Level.INFO))
      .contains("1 branch found in need of issue sync.");
    verify(asyncIssueIndexCreationTelemetry, times(1)).startIndexCreationMonitoringToSendTelemetry(1);
  }

  @Test
  public void triggerForProject() {
    ProjectDto projectDto = dbTester.components().insertPrivateProject().getProjectDto();
    BranchDto dto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branchName")
      .setUuid("branch_uuid")
      .setProjectUuid(projectDto.getUuid())
      .setIsMain(true);
    dbTester.components().insertProjectBranch(projectDto, dto);

    underTest.triggerForProject(projectDto.getUuid());

    Optional<BranchDto> branch = dbClient.branchDao().selectByUuid(dbTester.getSession(), "branch_uuid");
    assertThat(branch).isPresent();
    assertThat(branch.get().isNeedIssueSync()).isTrue();
    verify(ceQueue, times(2)).prepareSubmit();
    verify(ceQueue, times(1)).massSubmit(anyCollection());
    assertThat(logTester.logs(Level.INFO))
      .contains("2 branch(es) found in need of issue sync for project.");

    verifyNoInteractions(asyncIssueIndexCreationTelemetry);
  }

  @Test
  public void triggerOnIndexCreation_no_branch() {
    underTest.triggerOnIndexCreation();

    assertThat(logTester.logs(Level.INFO)).contains("0 branch found in need of issue sync.");
    verifyNoInteractions(asyncIssueIndexCreationTelemetry);
  }

  @Test
  public void triggerForProject_no_branch() {
    underTest.triggerForProject("some-random-uuid");
    assertThat(logTester.logs(Level.INFO)).contains("0 branch(es) found in need of issue sync for project.");
    verifyNoInteractions(asyncIssueIndexCreationTelemetry);
  }

  @Test
  public void remove_existing_indexation_task() {
    String reportTaskUuid = persistReportTasks();

    CeQueueDto task = new CeQueueDto();
    task.setUuid("uuid_2");
    task.setTaskType(BRANCH_ISSUE_SYNC);
    dbClient.ceQueueDao().insert(dbTester.getSession(), task);
    CeActivityDto activityDto = new CeActivityDto(task);
    activityDto.setStatus(Status.SUCCESS);
    dbClient.ceActivityDao().insert(dbTester.getSession(), activityDto);
    dbTester.commit();

    underTest.triggerOnIndexCreation();

    assertThat(dbClient.ceQueueDao().selectAllInAscOrder(dbTester.getSession())).extracting("uuid").containsExactly(reportTaskUuid);
    assertThat(dbClient.ceActivityDao().selectByTaskType(dbTester.getSession(), BRANCH_ISSUE_SYNC)).isEmpty();
    assertThat(dbClient.ceActivityDao().selectByTaskType(dbTester.getSession(), REPORT)).hasSize(1);
    assertThat(dbClient.ceTaskCharacteristicsDao().selectByTaskUuids(dbTester.getSession(), new HashSet<>(List.of("uuid_2")))).isEmpty();

    assertThat(logTester.logs(Level.INFO))
      .contains(
        "1 pending indexing task found to be deleted...",
        "1 completed indexing task found to be deleted...",
        "Indexing task deletion complete.",
        "Deleting tasks characteristics...",
        "Tasks characteristics deletion complete.");
  }

  @Test
  public void remove_existing_indexation_for_project_task() {
    String reportTaskUuid = persistReportTasks();

    ProjectDto projectDto = dbTester.components().insertPrivateProject().getProjectDto();
    String branchUuid = "branch_uuid";
    dbTester.components().insertProjectBranch(projectDto, b -> b.setBranchType(BRANCH).setUuid(branchUuid));

    CeQueueDto mainBranchTask = new CeQueueDto().setUuid("uuid_2").setTaskType(BRANCH_ISSUE_SYNC)
      .setEntityUuid(projectDto.getUuid()).setComponentUuid(projectDto.getUuid());
    dbClient.ceQueueDao().insert(dbTester.getSession(), mainBranchTask);

    CeQueueDto branchTask = new CeQueueDto().setUuid("uuid_3").setTaskType(BRANCH_ISSUE_SYNC)
      .setEntityUuid(projectDto.getUuid()).setComponentUuid(branchUuid);
    dbClient.ceQueueDao().insert(dbTester.getSession(), branchTask);

    ProjectDto anotherProjectDto = dbTester.components().insertPrivateProject().getProjectDto();
    CeQueueDto taskOnAnotherProject = new CeQueueDto().setUuid("uuid_4").setTaskType(BRANCH_ISSUE_SYNC)
      .setEntityUuid(anotherProjectDto.getUuid()).setComponentUuid("another-branchUuid");
    CeActivityDto canceledTaskOnAnotherProject = new CeActivityDto(taskOnAnotherProject).setStatus(Status.CANCELED);
    dbClient.ceActivityDao().insert(dbTester.getSession(), canceledTaskOnAnotherProject);

    dbTester.commit();

    underTest.triggerForProject(projectDto.getUuid());

    assertThat(dbClient.ceQueueDao().selectAllInAscOrder(dbTester.getSession())).extracting("uuid")
      .containsExactly(reportTaskUuid);
    assertThat(dbClient.ceActivityDao().selectByTaskType(dbTester.getSession(), REPORT)).hasSize(1);
    assertThat(dbClient.ceTaskCharacteristicsDao().selectByTaskUuids(dbTester.getSession(), new HashSet<>(List.of("uuid_2")))).isEmpty();

    // verify that the canceled tasks on anotherProject is still here, and was not removed by the project reindexing
    assertThat(dbClient.ceActivityDao().selectByTaskType(dbTester.getSession(), BRANCH_ISSUE_SYNC))
      .hasSize(1)
      .extracting(CeActivityDto::getEntityUuid)
      .containsExactly(anotherProjectDto.getUuid());

    assertThat(logTester.logs(Level.INFO))
      .contains(
        "2 pending indexing task found to be deleted...",
        "2 completed indexing task found to be deleted...",
        "Indexing task deletion complete.",
        "Deleting tasks characteristics...",
        "Tasks characteristics deletion complete.",
        "Tasks characteristics deletion complete.",
        "2 branch(es) found in need of issue sync for project.");
  }

  @Test
  public void order_by_last_analysis_date() {
    BranchDto dto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branch_1")
      .setUuid("branch_uuid1")
      .setProjectUuid("project_uuid1")
      .setIsMain(false);
    dbClient.branchDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
    insertSnapshot("analysis_1", "project_uuid1", 1);

    BranchDto dto2 = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branch_2")
      .setUuid("branch_uuid2")
      .setProjectUuid("project_uuid2")
      .setIsMain(false);
    dbClient.branchDao().insert(dbTester.getSession(), dto2);
    dbTester.commit();
    insertSnapshot("analysis_2", "project_uuid2", 2);

    underTest.triggerOnIndexCreation();

    verify(ceQueue, times(2)).prepareSubmit();

    ArgumentCaptor<Collection<CeTaskSubmit>> captor = ArgumentCaptor.forClass(Collection.class);

    verify(ceQueue, times(1)).massSubmit(captor.capture());
    List<Collection<CeTaskSubmit>> captures = captor.getAllValues();
    assertThat(captures).hasSize(1);
    Collection<CeTaskSubmit> tasks = captures.get(0);
    assertThat(tasks).hasSize(2);
    assertThat(tasks)
      .extracting(p -> p.getComponent().get().getUuid())
      .containsExactly("branch_uuid2", "branch_uuid1");

    assertThat(logTester.logs(Level.INFO))
      .contains("2 projects found in need of issue sync.");
  }

  @Test
  public void characteristics_are_defined() {
    BranchDto dto = new BranchDto()
      .setBranchType(BRANCH)
      .setKey("branch_1")
      .setUuid("branch_uuid1")
      .setProjectUuid("project_uuid1")
      .setIsMain(false);
    dbClient.branchDao().insert(dbTester.getSession(), dto);
    dbTester.commit();
    insertSnapshot("analysis_1", "project_uuid1", 1);

    BranchDto dto2 = new BranchDto()
      .setBranchType(PULL_REQUEST)
      .setKey("pr_1")
      .setUuid("pr_uuid_1")
      .setProjectUuid("project_uuid2")
      .setIsMain(false);
    dbClient.branchDao().insert(dbTester.getSession(), dto2);
    dbTester.commit();
    insertSnapshot("analysis_2", "project_uuid2", 2);

    underTest.triggerOnIndexCreation();

    ArgumentCaptor<Collection<CeTaskSubmit>> captor = ArgumentCaptor.forClass(Collection.class);
    verify(ceQueue, times(1)).massSubmit(captor.capture());
    List<Collection<CeTaskSubmit>> captures = captor.getAllValues();
    assertThat(captures).hasSize(1);
    Collection<CeTaskSubmit> tasks = captures.get(0);
    assertThat(tasks).hasSize(2);

    assertThat(tasks)
      .extracting(p -> p.getCharacteristics().get(BRANCH_TYPE),
        p -> p.getCharacteristics().get(CeTaskCharacteristics.BRANCH),
        p -> p.getCharacteristics().get(CeTaskCharacteristics.PULL_REQUEST))
      .containsExactlyInAnyOrder(
        tuple("BRANCH", "branch_1", null),
        tuple("PULL_REQUEST", null, "pr_1"));
  }

  @Test
  public void verify_comparator_transitivity() {
    Map<String, SnapshotDto> map = new HashMap<>();
    map.put("A", new SnapshotDto().setCreatedAt(1L));
    map.put("B", new SnapshotDto().setCreatedAt(2L));
    map.put("C", new SnapshotDto().setCreatedAt(-1L));
    List<String> uuids = new ArrayList<>(map.keySet());
    uuids.add("D");
    Comparators.verifyTransitivity(AsyncIssueIndexingImpl.compareBySnapshot(map), uuids);
  }

  @Test
  public void trigger_with_lot_of_not_analyzed_project_should_not_raise_exception() {
    for (int i = 0; i < 100; i++) {
      BranchDto dto = new BranchDto()
        .setBranchType(BRANCH)
        .setKey("branch_" + i)
        .setUuid("branch_uuid" + i)
        .setProjectUuid("project_uuid" + i)
        .setIsMain(false);
      dbClient.branchDao().insert(dbTester.getSession(), dto);
      dbTester.commit();
      insertSnapshot("analysis_" + i, "project_uuid" + i, 1);
    }

    for (int i = 100; i < 200; i++) {
      BranchDto dto = new BranchDto()
        .setBranchType(BRANCH)
        .setKey("branch_" + i)
        .setUuid("branch_uuid" + i)
        .setProjectUuid("project_uuid" + i)
        .setIsMain(false);
      dbClient.branchDao().insert(dbTester.getSession(), dto);
      dbTester.commit();
    }

    assertThatCode(underTest::triggerOnIndexCreation).doesNotThrowAnyException();
  }

  private SnapshotDto insertSnapshot(String analysisUuid, String projectUuid, long createdAt) {
    SnapshotDto snapshot = new SnapshotDto()
      .setUuid(analysisUuid)
      .setRootComponentUuid(projectUuid)
      .setStatus(STATUS_PROCESSED)
      .setCreatedAt(createdAt)
      .setLast(true);
    dbTester.getDbClient().snapshotDao().insert(dbTester.getSession(), snapshot);
    dbTester.commit();
    return snapshot;
  }

  private String persistReportTasks() {
    CeQueueDto reportTask = new CeQueueDto();
    reportTask.setUuid("uuid_1");
    reportTask.setTaskType(REPORT);
    dbClient.ceQueueDao().insert(dbTester.getSession(), reportTask);

    CeActivityDto reportActivity = new CeActivityDto(reportTask);
    reportActivity.setStatus(Status.SUCCESS);
    dbClient.ceActivityDao().insert(dbTester.getSession(), reportActivity);
    return reportTask.getUuid();
  }

}
