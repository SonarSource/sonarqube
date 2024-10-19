/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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
package org.sonar.ce.queue;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.AlwaysIncreasingSystem2;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.config.ComputeEngineProperties;
import org.sonar.db.DbTester;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.ComponentDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.core.ce.CeTaskCharacteristics.BRANCH;
import static org.sonar.core.ce.CeTaskCharacteristics.PULL_REQUEST;
import static org.sonar.db.ce.CeQueueDto.Status.IN_PROGRESS;
import static org.sonar.db.ce.CeQueueDto.Status.PENDING;

public class NextPendingTaskPickerIT {

  private final System2 alwaysIncreasingSystem2 = new AlwaysIncreasingSystem2(1L, 1);

  private final Configuration config = mock(Configuration.class);

  @Rule
  public LogTester logTester = new LogTester();

  private NextPendingTaskPicker underTest;

  @Rule
  public DbTester db = DbTester.create(alwaysIncreasingSystem2);

  @Before
  public void before() {
    underTest = new NextPendingTaskPicker(config, db.getDbClient());
    when(config.getBoolean(ComputeEngineProperties.CE_PARALLEL_PROJECT_TASKS_ENABLED)).thenReturn(Optional.of(true));
  }

  @Test
  public void findPendingTask_whenNoTasksPending_returnsEmpty() {
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isEmpty();
  }

  @Test
  public void findPendingTask_whenTwoTasksPending_returnsTheOlderOne() {
    // both the 'eligibleTask' and 'parallelEligibleTask' will point to this one
    insertPending("1");
    insertPending("2");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("1");
  }

  @Test
  public void findPendingTask_whenTwoTasksPendingWithSameCreationDate_returnsLowestUuid() {
    insertPending("d", c -> c.setCreatedAt(1L).setUpdatedAt(1L));
    insertPending("c", c -> c.setCreatedAt(1L).setUpdatedAt(1L));
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("c");
  }

  @Test
  public void findPendingTask_givenBranchInProgressAndPropertySet_returnQueuedPR() {
    insertInProgress("1");
    insertPendingPullRequest("2");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("2");

    assertThat(logTester.logs()).contains("Task [uuid = " + ceQueueDto.get().getUuid() + "] will be run concurrently with other tasks for the same project");
  }

  @Test
  public void findPendingTask_givenBranchInProgressAndPropertyNotSet_dontReturnQueuedPR() {
    when(config.getBoolean(ComputeEngineProperties.CE_PARALLEL_PROJECT_TASKS_ENABLED)).thenReturn(Optional.of(false));
    insertInProgress("1");
    insertPendingPullRequest("2");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isEmpty();
  }

  @Test
  public void findPendingTask_given2PRsQueued_returnBothQueuedPR() {
    insertPendingPullRequest("1");
    insertPendingPullRequest("2");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);
    Optional<CeQueueDto> ceQueueDto2 = underTest.findPendingTask("workerUuid2", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto2).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("1");
    assertThat(ceQueueDto.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(ceQueueDto2.get().getStatus()).isEqualTo(IN_PROGRESS);
  }

  @Test
  public void findPendingTask_given1MainBranch_2PRsQueued_returnMainBranchAndPRs() {
    insertPending("1");
    insertPendingPullRequest("2");
    insertPendingPullRequest("3");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);
    Optional<CeQueueDto> ceQueueDto2 = underTest.findPendingTask("workerUuid2", db.getSession(), true);
    Optional<CeQueueDto> ceQueueDto3 = underTest.findPendingTask("workerUuid3", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto2).isPresent();
    assertThat(ceQueueDto3).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("1");
    assertThat(ceQueueDto.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(ceQueueDto2.get().getUuid()).isEqualTo("2");
    assertThat(ceQueueDto2.get().getStatus()).isEqualTo(IN_PROGRESS);
    assertThat(ceQueueDto3.get().getUuid()).isEqualTo("3");
    assertThat(ceQueueDto3.get().getStatus()).isEqualTo(IN_PROGRESS);
  }

  @Test
  public void findPendingTask_given1MainBranch_2BranchesQueued_returnOnyMainBranch() {
    insertPending("1", null);
    insertPendingBranch("2");
    insertPendingBranch("3");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);
    Optional<CeQueueDto> ceQueueDto2 = underTest.findPendingTask("workerUuid2", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto2).isEmpty();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("1");
    assertThat(ceQueueDto.get().getStatus()).isEqualTo(IN_PROGRESS);
  }

  @Test
  public void findPendingTask_given2BranchesQueued_returnOnlyFirstQueuedBranch() {
    insertPending("1");
    insertPendingBranch("2");
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);
    Optional<CeQueueDto> ceQueueDto2 = underTest.findPendingTask("workerUuid2", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto2).isEmpty();
    assertThat(ceQueueDto.get().getStatus()).isEqualTo(IN_PROGRESS);
  }

  @Test
  public void findPendingTask_given2SamePRsQueued_returnOnlyFirstQueuedPR() {
    insertPendingPullRequest("1", c -> c.setComponentUuid("pr1"));
    insertPendingPullRequest("2", c -> c.setComponentUuid("pr1"));
    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);
    Optional<CeQueueDto> ceQueueDto2 = underTest.findPendingTask("workerUuid2", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto2).isEmpty();
    assertThat(ceQueueDto.get().getStatus()).isEqualTo(IN_PROGRESS);
  }

  @Test
  public void findPendingTask_givenBranchInTheQueueOlderThanPrInTheQueue_dontJumpAheadOfBranch() {
    // we have branch task in progress. Next branch task needs to wait for this one to finish. We dont allow PRs to jump ahead of this branch
    insertInProgress("1");
    insertPending("2");
    insertPendingPullRequest("3");

    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isEmpty();
  }

  @Test
  public void findPendingTask_givenDifferentProjectAndPrInTheQueue_dontJumpAheadOfDifferentProject() {
    // we have branch task in progress.
    insertInProgress("1");
    // The PR can run in parallel, but needs to wait for this other project to finish. We dont allow PRs to jump ahead
    insertPending("2", c -> c.setEntityUuid("different project"));
    insertPendingPullRequest("3");

    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("2");
  }

  @Test
  public void findPendingTask_givenDifferentProjectAndPrInTheQueue_prCanRunFirst() {
    // we have branch task in progress.
    insertInProgress("1");
    // The PR can run in parallel and is ahead of the other project
    insertPendingPullRequest("2");
    insertPending("3", c -> c.setEntityUuid("different project"));

    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("2");
  }

  @Test
  public void findPendingTask_givenFivePrsInProgress_branchCanBeScheduled() {
    insertInProgressPullRequest("1");
    insertInProgressPullRequest("2");
    insertInProgressPullRequest("3");
    insertInProgressPullRequest("4");
    insertInProgressPullRequest("5");
    insertPending("6");

    Optional<CeQueueDto> ceQueueDto = underTest.findPendingTask("workerUuid", db.getSession(), true);

    assertThat(ceQueueDto).isPresent();
    assertThat(ceQueueDto.get().getUuid()).isEqualTo("6");
  }

  @Test
  public void findPendingTask_excludingViewPickUpOrphanBranches() {
    insertPending("1", dto -> dto
      .setComponentUuid("1")
      .setEntityUuid("non-existing-uuid")
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.BRANCH_ISSUE_SYNC)
      .setCreatedAt(100_000L));

    Optional<CeQueueDto> peek = underTest.findPendingTask("1", db.getSession(), false);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo("1");
  }

  @Test
  public void exclude_portfolios_computation_when_indexing_issues() {
    String taskUuid1 = "1", taskUuid2 = "2";
    String branchUuid = "1";
    insertBranch(branchUuid);
    insertPending(taskUuid1, dto -> dto
      .setComponentUuid(branchUuid)
      .setEntityUuid("entity_uuid")
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.BRANCH_ISSUE_SYNC)
      .setCreatedAt(100_000L));

    String view_uuid = "view_uuid";
    insertView(view_uuid);
    insertPending(taskUuid2, dto -> dto
      .setComponentUuid(view_uuid)
      .setEntityUuid(view_uuid)
      .setStatus(PENDING)
      .setTaskType(CeTaskTypes.REPORT)
      .setCreatedAt(100_000L));

    Optional<CeQueueDto> peek = underTest.findPendingTask("1", db.getSession(), false);
    assertThat(peek).isPresent();
    assertThat(peek.get().getUuid()).isEqualTo(taskUuid1);

    Optional<CeQueueDto> peek2 = underTest.findPendingTask("1", db.getSession(), false);
    assertThat(peek2).isPresent();
    assertThat(peek2.get().getUuid()).isEqualTo(taskUuid2);
  }

  private CeQueueDto insertPending(String uuid) {
    return insertPending(uuid, null);
  }

  private CeQueueDto insertPendingBranch(String uuid) {
    CeQueueDto queue = insertPending(uuid, null);
    insertCharacteristics(queue.getUuid(), BRANCH);
    return queue;
  }

  private CeQueueDto insertPendingPullRequest(String uuid) {
    return insertPendingPullRequest(uuid, null);
  }

  private CeQueueDto insertPendingPullRequest(String uuid, @Nullable Consumer<CeQueueDto> ceQueueDto) {
    CeQueueDto queue = insertPending(uuid, ceQueueDto);
    insertCharacteristics(queue.getUuid(), PULL_REQUEST);
    return queue;
  }

  private CeQueueDto insertInProgressPullRequest(String uuid) {
    CeQueueDto queue = insertInProgress(uuid, null);
    insertCharacteristics(queue.getUuid(), PULL_REQUEST);
    return queue;
  }

  private CeQueueDto insertInProgress(String uuid) {
    return insertInProgress(uuid, null);
  }

  private CeQueueDto insertInProgress(String uuid, @Nullable Consumer<CeQueueDto> ceQueueDto) {
    return insertTask(uuid, IN_PROGRESS, ceQueueDto);
  }

  private CeQueueDto insertPending(String uuid, @Nullable Consumer<CeQueueDto> ceQueueDto) {
    return insertTask(uuid, PENDING, ceQueueDto);
  }

  private CeTaskCharacteristicDto insertCharacteristics(String taskUuid, String branchType) {
    var ctcDto = new CeTaskCharacteristicDto();
    ctcDto.setUuid(UUID.randomUUID().toString());
    ctcDto.setTaskUuid(taskUuid);
    ctcDto.setKey(branchType);
    ctcDto.setValue("value");
    db.getDbClient().ceTaskCharacteristicsDao().insert(db.getSession(), ctcDto);
    db.getSession().commit();
    return ctcDto;
  }

  private CeQueueDto insertTask(String uuid, CeQueueDto.Status status, @Nullable Consumer<CeQueueDto> ceQueueDtoConsumer) {
    CeQueueDto dto = createCeQueue(uuid, status, ceQueueDtoConsumer);
    db.getDbClient().ceQueueDao().insert(db.getSession(), dto);
    db.getSession().commit();
    return dto;
  }

  @NotNull
  private static CeQueueDto createCeQueue(String uuid, CeQueueDto.Status status, @Nullable Consumer<CeQueueDto> ceQueueDtoConsumer) {
    CeQueueDto dto = new CeQueueDto();
    dto.setUuid(uuid);
    dto.setTaskType(CeTaskTypes.REPORT);
    dto.setStatus(status);
    dto.setSubmitterUuid("henri");
    dto.setComponentUuid(UUID.randomUUID().toString());
    dto.setEntityUuid("1");
    if (ceQueueDtoConsumer != null) {
      ceQueueDtoConsumer.accept(dto);
    }
    return dto;
  }

  private void insertView(String view_uuid) {
    ComponentDto view = new ComponentDto();
    view.setQualifier("VW");
    view.setKey(view_uuid + "_key");
    view.setUuid(view_uuid);
    view.setPrivate(false);
    view.setUuidPath("uuid_path");
    view.setBranchUuid(view_uuid);
    db.components().insertPortfolioAndSnapshot(view);
    db.commit();
  }

  private void insertBranch(String uuid) {
    ComponentDto branch = new ComponentDto();
    branch.setQualifier("TRK");
    branch.setKey(uuid + "_key");
    branch.setUuid(uuid);
    branch.setPrivate(false);
    branch.setUuidPath("uuid_path");
    branch.setBranchUuid(uuid);
    db.components().insertComponent(branch);
    db.commit();
  }
}
