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
package org.sonar.db.purge;

import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.event.Level;
import org.sonar.api.issue.Issue;
import org.sonar.api.testfixtures.log.LogAndArguments;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.log.LoggerLevel;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbInputStream;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.setting.AlmSettingDto;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeQueueDto.Status;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.ce.CeTaskTypes;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentQualifiers;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.ProjectData;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.dialect.Dialect;
import org.sonar.db.dismissmessage.MessageType;
import org.sonar.db.event.EventComponentChangeDto;
import org.sonar.db.event.EventDto;
import org.sonar.db.event.EventTesting;
import org.sonar.db.issue.AnticipatedTransitionDto;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.ProjectMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.newcodeperiod.NewCodePeriodDto;
import org.sonar.db.newcodeperiod.NewCodePeriodType;
import org.sonar.db.portfolio.PortfolioDto;
import org.sonar.db.portfolio.PortfolioProjectDto;
import org.sonar.db.project.ProjectDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.report.ReportScheduleDto;
import org.sonar.db.report.ReportSubscriptionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.user.UserDismissedMessageDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.webhook.WebhookDeliveryLiteDto;
import org.sonar.db.webhook.WebhookDto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.ComponentTesting.newSubPortfolio;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotTesting.newSnapshot;
import static org.sonar.db.event.EventDto.CATEGORY_SQ_UPGRADE;
import static org.sonar.db.event.EventDto.CATEGORY_VERSION;
import static org.sonar.db.issue.IssueTesting.newCodeReferenceIssue;
import static org.sonar.db.portfolio.PortfolioDto.SelectionMode.MANUAL;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.db.webhook.WebhookDeliveryTesting.selectAllDeliveryUuids;

class PurgeDaoIT {

  private static final String PROJECT_UUID = "P1";

  private final Random random = new SecureRandom();

  private final System2 system2 = mock(System2.class);

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);
  @RegisterExtension
  private final LogTesterJUnit5 logTester = new LogTesterJUnit5();
  private final DbClient dbClient = db.getDbClient();
  private final DbSession dbSession = db.getSession();

  private final PurgeDao underTest = db.getDbClient().purgeDao();

  @Test
  void purge_failed_ce_tasks() {
    ProjectData project = db.components().insertPrivateProject();
    SnapshotDto pastAnalysis = db.components().insertSnapshot(project.getMainBranchComponent(),
      t -> t.setStatus(STATUS_PROCESSED).setLast(false));
    db.components().insertSnapshot(project.getMainBranchComponent(), t -> t.setStatus(STATUS_UNPROCESSED).setLast(false));
    SnapshotDto lastAnalysis = db.components().insertSnapshot(project.getMainBranchComponent(),
      t -> t.setStatus(STATUS_PROCESSED).setLast(true));

    underTest.purge(dbSession, newConfigurationWith30Days(System2.INSTANCE, project.getMainBranchComponent().uuid(),
      project.projectUuid()), PurgeListener.EMPTY,
      new PurgeProfiler());
    dbSession.commit();

    assertThat(uuidsOfAnalysesOfRoot(project.getMainBranchComponent())).containsOnly(pastAnalysis.getUuid(), lastAnalysis.getUuid());
  }

  /**
   * SONAR-17720
   */
  @Test
  void purge_inactive_branches_should_not_purge_newly_created_branches() {
    when(system2.now()).thenReturn(new Date().getTime());
    ProjectData project = db.components().insertPublicProject();

    // new branch without a snapshot (analysis not processed yet)
    BranchDto pr1 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.PULL_REQUEST));
    underTest.purge(dbSession, newConfigurationWith30Days(System2.INSTANCE, project.getMainBranchComponent().branchUuid(),
project.getProjectDto().getUuid()), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    // pr not purged
    assertThat(uuidsIn("components")).containsOnly(project.getMainBranchDto().getUuid(), pr1.getUuid());
    assertThat(uuidsIn("project_branches")).containsOnly(project.getMainBranchDto().getUuid(), pr1.getUuid());
  }

  @Test
  void purge_inactive_branches() {
    Date date31DaysAgo = DateUtils.addDays(new Date(), -31);
    when(system2.now()).thenReturn(new Date().getTime());
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPublicProject();
    BranchDto branch1 = db.components().insertProjectBranch(project.getProjectDto());
    BranchDto branch2 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH));
    BranchDto pr1 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.PULL_REQUEST));

    db.components().insertSnapshot(branch1);
    db.components().insertSnapshot(branch2);
    db.components().insertSnapshot(pr1);

    // branch with other components and issues, last analysed 31 days ago
    BranchDto branch3 = db.components().insertProjectBranch(project.getProjectDto(),
      b -> b.setBranchType(BranchType.BRANCH).setExcludeFromPurge(false));
    addComponentsSnapshotsAndIssuesToBranch(branch3, rule, 31);

    // branch with no analysis
    BranchDto branch4 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH));

    // branch last analysed 31 days ago but protected from purge
    BranchDto branch5 = db.components().insertProjectBranch(project.getProjectDto(),
      b -> b.setBranchType(BranchType.BRANCH).setExcludeFromPurge(true));
    db.components().insertSnapshot(branch5, dto -> dto.setCreatedAt(date31DaysAgo.getTime()));

    // pull request last analysed 100 days ago
    BranchDto pr2 = db.components().insertProjectBranch(project.getProjectDto(),
      b -> b.setBranchType(BranchType.PULL_REQUEST).setExcludeFromPurge(false));
    addComponentsSnapshotsAndIssuesToBranch(pr2, rule, 100);

    // pull request last analysed 100 days ago but marked as "excluded from purge" which should not work for pull requests
    BranchDto pr3 = db.components().insertProjectBranch(project.getProjectDto(),
      b -> b.setBranchType(BranchType.PULL_REQUEST).setExcludeFromPurge(true));
    addComponentsSnapshotsAndIssuesToBranch(pr3, rule, 100);

    updateBranchCreationDate(date31DaysAgo, branch1.getUuid(), branch2.getUuid(), branch3.getUuid(), branch4.getUuid(), branch5.getUuid()
      , pr1.getUuid(), pr2.getUuid(),
      pr3.getUuid());

    underTest.purge(dbSession, newConfigurationWith30Days(System2.INSTANCE, project.getMainBranchDto().getUuid(),
        project.getProjectDto().getUuid()), PurgeListener.EMPTY,
      new PurgeProfiler());
    dbSession.commit();

    assertThat(uuidsIn("components")).containsOnly(
      project.getMainBranchDto().getUuid(), branch1.getUuid(), branch2.getUuid(), branch5.getUuid(), pr1.getUuid());
    assertThat(uuidsIn("projects")).containsOnly(project.getProjectDto().getUuid());
  }

  private void updateBranchCreationDate(Date date, String... branchUuids) {
    for (String branchUuid : branchUuids) {
      db.executeUpdateSql("update project_branches set created_at = '" + date.getTime() + "' where uuid = '" + branchUuid + "'");
    }
    db.getSession().commit();
  }

  @Test
  void purge_inactive_pull_request() {
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPublicProject();
    BranchDto nonMainBranch = db.components().insertProjectBranch(project.getProjectDto());
    db.components().insertSnapshot(nonMainBranch);
    BranchDto recentPullRequest = db.components().insertProjectBranch(project.getProjectDto(),
      b -> b.setBranchType(BranchType.PULL_REQUEST));
    db.components().insertSnapshot(recentPullRequest);

    // pull request with other components and issues, updated 31 days ago
    BranchDto pullRequest = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.PULL_REQUEST));
    db.components().insertSnapshot(pullRequest, dto -> dto.setCreatedAt(DateUtils.addDays(new Date(), -31).getTime()));
    ComponentDto pullRequestComponent = db.components().getComponentDto(pullRequest);
    ComponentDto dir = db.components().insertComponent(newDirectory(pullRequestComponent, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(pullRequestComponent, dir));
    db.issues().insert(rule, pullRequestComponent, file);

    underTest.purge(dbSession, newConfigurationWith30Days(System2.INSTANCE, project.getMainBranchComponent().branchUuid(),
      project.getProjectDto().getUuid()), PurgeListener.EMPTY,
      new PurgeProfiler());
    dbSession.commit();

    assertThat(uuidsIn("components")).containsOnly(project.getMainBranchDto().getUuid(), nonMainBranch.getUuid(),
      recentPullRequest.getUuid());
    assertThat(uuidsIn("projects")).containsOnly(project.getProjectDto().getUuid());
  }

  @Test
  void purge_inactive_branches_when_analyzing_non_main_branch() {
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPublicProject();
    BranchDto nonMainBranch = db.components().insertProjectBranch(project.getProjectDto());
    db.components().insertSnapshot(nonMainBranch);

    // branch updated 31 days ago
    BranchDto branch1 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH));
    db.components().insertSnapshot(branch1, dto -> dto.setCreatedAt(DateUtils.addDays(new Date(), -31).getTime()));

    // branches with other components and issues, updated 31 days ago
    BranchDto branch2 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.PULL_REQUEST));
    db.components().insertSnapshot(branch2, dto -> dto.setCreatedAt(DateUtils.addDays(new Date(), -31).getTime()));

    ComponentDto branch2Component = db.components().getComponentDto(branch2);
    ComponentDto file = db.components().insertComponent(newFileDto(branch2Component));
    db.issues().insert(rule, branch2Component, file);

    BranchDto branch3 = db.components().insertProjectBranch(project.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH));
    db.components().insertSnapshot(branch3, dto -> dto.setCreatedAt(DateUtils.addDays(new Date(), -31).getTime()));

    // analysing branch1
    underTest.purge(dbSession, newConfigurationWith30Days(System2.INSTANCE, branch1.getUuid(), project.getProjectDto().getUuid()),
      PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    // branch1 wasn't deleted since it was being analyzed!
    assertThat(uuidsIn("components")).containsOnly(project.getMainBranchDto().getUuid(), nonMainBranch.getUuid(), branch1.getUuid());
    assertThat(uuidsIn("projects")).containsOnly(project.getProjectDto().getUuid());
  }

  @Test
  void close_issues_clean_index_and_file_sources_of_disabled_components_specified_by_uuid_in_configuration() {
    RuleDto rule = db.rules().insert();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    db.components().insertSnapshot(mainBranch);
    db.components().insertSnapshot(mainBranch);
    db.components().insertSnapshot(mainBranch, s -> s.setLast(false));

    ComponentDto dir = db.components().insertComponent(newDirectory(mainBranch, "sub").setEnabled(false));
    ComponentDto srcFile = db.components().insertComponent(newFileDto(mainBranch, dir).setEnabled(false));
    ComponentDto testFile = db.components().insertComponent(newFileDto(mainBranch, dir).setEnabled(false));
    ComponentDto enabledFile = db.components().insertComponent(newFileDto(mainBranch, dir).setEnabled(true));
    IssueDto openOnFile = db.issues().insert(rule, mainBranch, srcFile, issue -> issue.setStatus("OPEN"));
    IssueDto confirmOnFile = db.issues().insert(rule, mainBranch, srcFile, issue -> issue.setStatus("CONFIRM"));
    IssueDto openOnDir = db.issues().insert(rule, mainBranch, dir, issue -> issue.setStatus("OPEN"));
    IssueDto confirmOnDir = db.issues().insert(rule, mainBranch, dir, issue -> issue.setStatus("CONFIRM"));
    IssueDto openOnEnabledComponent = db.issues().insert(rule, mainBranch, enabledFile, issue -> issue.setStatus("OPEN"));
    IssueDto confirmOnEnabledComponent = db.issues().insert(rule, mainBranch, enabledFile, issue -> issue.setStatus("CONFIRM"));

    assertThat(countPurgedSnapshots()).isZero();

    assertThat(db.countSql("select count(*) from issues where status = 'CLOSED'")).isZero();
    assertThat(db.countSql("select count(*) from issues where resolution = 'REMOVED'")).isZero();

    db.fileSources().insertFileSource(srcFile);
    FileSourceDto nonSelectedFileSource = db.fileSources().insertFileSource(enabledFile);
    assertThat(db.countRowsOfTable("file_sources")).isEqualTo(2);

    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();

    db.measures().insertMeasure(srcFile,
      m -> m.addValue(metric1.getKey(), RandomUtils.nextInt(50)).addValue(metric2.getKey(), RandomUtils.nextInt(50)));
    db.measures().insertMeasure(dir,
      m -> m.addValue(metric1.getKey(), RandomUtils.nextInt(50)).addValue(metric2.getKey(), RandomUtils.nextInt(50)));
    db.measures().insertMeasure(mainBranch,
      m -> m.addValue(metric1.getKey(), RandomUtils.nextInt(50)).addValue(metric2.getKey(), RandomUtils.nextInt(50)));
    db.measures().insertMeasure(enabledFile,
      m -> m.addValue(metric1.getKey(), RandomUtils.nextInt(50)).addValue(metric2.getKey(), RandomUtils.nextInt(50)));
    assertThat(db.countRowsOfTable("measures")).isEqualTo(4);

    // back to present
    Set<String> selectedComponentUuids = Set.of(srcFile.uuid(), testFile.uuid());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, mainBranch.uuid(), projectData.projectUuid(), selectedComponentUuids),
      mock(PurgeListener.class), new PurgeProfiler());
    dbSession.commit();

    // set purged=true for non-last snapshot
    assertThat(countPurgedSnapshots()).isOne();

    // close open issues of selected
    assertThat(db.countSql("select count(*) from issues where status = 'CLOSED'")).isEqualTo(4);
    for (IssueDto issue : Arrays.asList(openOnFile, confirmOnFile, openOnDir, confirmOnDir)) {
      assertThat(db.getDbClient().issueDao().selectOrFailByKey(dbSession, issue.getKey()))
        .extracting(IssueDto::getStatus, IssueDto::getResolution)
        .containsExactlyInAnyOrder("CLOSED", "REMOVED");
    }
    for (IssueDto issue : Arrays.asList(openOnEnabledComponent, confirmOnEnabledComponent)) {
      assertThat(db.getDbClient().issueDao().selectByKey(dbSession, issue.getKey()).get())
        .extracting("status", "resolution")
        .containsExactlyInAnyOrder(issue.getStatus(), null);
    }

    // delete file sources of selected
    assertThat(db.countRowsOfTable("file_sources")).isOne();
    assertThat(db.getDbClient().fileSourceDao().selectByFileUuid(dbSession, nonSelectedFileSource.getFileUuid())).isNotNull();

    // delete measures of selected
    assertThat(db.countRowsOfTable("measures")).isEqualTo(2);
    List<MeasureDto> measureDtos = Set.of(srcFile.uuid(), dir.uuid(), mainBranch.uuid(), enabledFile.uuid()).stream()
      .map(component -> db.getDbClient().measureDao().selectByComponentUuid(dbSession, component))
      .filter(Optional::isPresent).map(Optional::get).toList();
    assertThat(measureDtos)
      .extracting(MeasureDto::getComponentUuid)
      .containsOnly(enabledFile.uuid(), mainBranch.uuid());
    assertThat(measureDtos)
      .allSatisfy(dto -> assertThat(dto.getMetricValues())
        .containsOnlyKeys(metric1.getKey(), metric2.getKey()));
  }

  @Test
  void shouldDeleteAnalyses() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis1 = db.components().insertSnapshot(project);
    SnapshotDto analysis2 = db.components().insertSnapshot(project);
    SnapshotDto analysis3 = db.components().insertSnapshot(project);
    ComponentDto otherProject = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto otherAnalysis1 = db.components().insertSnapshot(otherProject);

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), singletonList(analysis1.getUuid()));

    assertThat(uuidsIn("snapshots")).containsOnly(analysis2.getUuid(), analysis3.getUuid(), otherAnalysis1.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), asList(analysis1.getUuid(), analysis3.getUuid(), otherAnalysis1.getUuid()));

    assertThat(uuidsIn("snapshots")).containsOnly(analysis2.getUuid());
  }

  @Test
  void purge_should_log_profiling_in_debug() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    logTester.setLevel(Level.DEBUG);
    underTest.deleteProject(db.getSession(), project.uuid(), ComponentQualifiers.PROJECT, project.name(), project.getKey());

    assertThat(logTester.getLogs(LoggerLevel.DEBUG).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.joining()))
      .contains("Profiling for project deletion");
  }

  @Test
  void purge_should_not_log_profiling_in_info() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();

    logTester.setLevel(Level.INFO);
    underTest.deleteProject(db.getSession(), project.uuid(), ComponentQualifiers.PROJECT, project.name(), project.getKey());

    assertThat(logTester.getLogs(LoggerLevel.DEBUG).stream()
      .map(LogAndArguments::getFormattedMsg)
      .collect(Collectors.joining()))
      .doesNotContain("Profiling for project deletion");
  }

  @Test
  void deleteAnalyses_deletes_rows_in_events_and_event_component_changes() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    dbClient.componentDao().insert(dbSession, project, true);
    SnapshotDto projectAnalysis1 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis2 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis3 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis4 = db.components().insertSnapshot(project);
    EventDto projectEvent1 = db.events().insertEvent(projectAnalysis1);
    EventDto projectEvent2 = db.events().insertEvent(projectAnalysis2);
    EventDto projectEvent3 = db.events().insertEvent(projectAnalysis3);
    // note: projectAnalysis4 has no event
    ComponentDto referencedProjectA = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto referencedProjectB = db.components().insertPublicProject().getMainBranchComponent();
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectA, null);
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectB, null);
    BranchDto branchProjectA = newBranchDto(referencedProjectA);
    ComponentDto cptBranchProjectA = ComponentTesting.newBranchComponent(referencedProjectA, branchProjectA);
    db.events().insertEventComponentChanges(projectEvent2, projectAnalysis2, randomChangeCategory(), cptBranchProjectA, branchProjectA);
    // note: projectEvent3 has no component change

    // delete non existing analysis has no effect
    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), singletonList("foo"));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis1.getUuid(), projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(3);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent1.getUuid(), projectEvent2.getUuid(), projectEvent3.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), singletonList(projectAnalysis1.getUuid()));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isOne();
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent2.getUuid(), projectEvent3.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), singletonList(projectAnalysis4.getUuid()));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isOne();
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent2.getUuid(), projectEvent3.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), singletonList(projectAnalysis3.getUuid()));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isOne();
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent2.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), singletonList(projectAnalysis2.getUuid()));
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isZero();
    assertThat(db.countRowsOfTable("events"))
      .isZero();
  }

  @Test
  void selectPurgeableAnalyses() {
    SnapshotDto[] analyses = new SnapshotDto[]{
      newSnapshot()
        .setUuid("u1")
        .setRootComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(true),
      // not processed -> exclude
      newSnapshot()
        .setUuid("u2")
        .setRootComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_UNPROCESSED)
        .setLast(false),
      // on other resource -> exclude
      newSnapshot()
        .setUuid("u3")
        .setRootComponentUuid("uuid_222")
        .setStatus(STATUS_PROCESSED)
        .setLast(true),
      // without event -> select
      newSnapshot()
        .setUuid("u4")
        .setRootComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(false),
      // with event -> select
      newSnapshot()
        .setUuid("u5")
        .setRootComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(false)
        .setProjectVersion("V5"),
      // with multiple events -> select
      newSnapshot()
        .setUuid("u6")
        .setRootComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(false)
        .setProjectVersion("V6"),
      // with singe event different from Version -> select
      newSnapshot()
        .setUuid("u7")
        .setRootComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(false)
        .setProjectVersion("V7")
    };
    db.components().insertSnapshots(analyses);
    db.events().insertEvent(EventTesting.newEvent(analyses[4])
      .setName("V5")
      .setCategory(CATEGORY_VERSION));

    db.events().insertEvent(EventTesting.newEvent(analyses[5])
      .setName("V6")
      .setCategory(CATEGORY_VERSION));
    db.events().insertEvent(EventTesting.newEvent(analyses[5])
      .setName("10.3")
      .setCategory(CATEGORY_SQ_UPGRADE));

    db.events().insertEvent(EventTesting.newEvent(analyses[6])
      .setName("10.3")
      .setCategory(CATEGORY_SQ_UPGRADE));

    List<PurgeableAnalysisDto> purgeableAnalyses = underTest.selectProcessedAnalysisByComponentUuid(PROJECT_UUID, dbSession);

    assertThat(purgeableAnalyses).hasSize(5);
    assertThat(getById(purgeableAnalyses, "u1").isLast()).isTrue();
    assertThat(getById(purgeableAnalyses, "u1").hasEvents()).isFalse();
    assertThat(getById(purgeableAnalyses, "u1").getVersion()).isNull();
    assertThat(getById(purgeableAnalyses, "u4").isLast()).isFalse();
    assertThat(getById(purgeableAnalyses, "u4").hasEvents()).isFalse();
    assertThat(getById(purgeableAnalyses, "u4").getVersion()).isNull();
    assertThat(getById(purgeableAnalyses, "u5").isLast()).isFalse();
    assertThat(getById(purgeableAnalyses, "u5").hasEvents()).isTrue();
    assertThat(getById(purgeableAnalyses, "u5").getVersion()).isEqualTo("V5");
    assertThat(getById(purgeableAnalyses, "u6").isLast()).isFalse();
    assertThat(getById(purgeableAnalyses, "u6").hasEvents()).isTrue();
    assertThat(getById(purgeableAnalyses, "u6").getVersion()).isEqualTo("V6");
    assertThat(getById(purgeableAnalyses, "u7").isLast()).isFalse();
    assertThat(getById(purgeableAnalyses, "u7").hasEvents()).isTrue();
    assertThat(getById(purgeableAnalyses, "u7").getVersion()).isNull();
  }

  @Test
  void selectPurgeableAnalyses_does_not_return_the_baseline() {
    ComponentDto project1 = db.components().insertPublicProject("master").getMainBranchComponent();
    SnapshotDto analysis1 = db.components().insertSnapshot(newSnapshot()
      .setRootComponentUuid(project1.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    dbClient.newCodePeriodDao().insert(dbSession,
      new NewCodePeriodDto()
        .setProjectUuid(project1.uuid())
        .setBranchUuid(project1.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue(analysis1.getUuid()));
    ComponentDto project2 = db.components().insertPrivateProject().getMainBranchComponent();
    SnapshotDto analysis2 = db.components().insertSnapshot(newSnapshot()
      .setRootComponentUuid(project2.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    db.components().insertProjectBranch(project2);

    assertThat(underTest.selectProcessedAnalysisByComponentUuid(project1.uuid(), dbSession)).isEmpty();
    assertThat(underTest.selectProcessedAnalysisByComponentUuid(project2.uuid(), dbSession))
      .extracting(PurgeableAnalysisDto::getAnalysisUuid)
      .containsOnly(analysis2.getUuid());
  }

  @Test
  void selectPurgeableAnalyses_does_not_return_the_baseline_of_specific_branch() {
    ComponentDto project = db.components().insertPublicProject("master").getMainBranchComponent();
    SnapshotDto analysisProject = db.components().insertSnapshot(newSnapshot()
      .setRootComponentUuid(project.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    dbClient.newCodePeriodDao().insert(dbSession,
      new NewCodePeriodDto()
        .setProjectUuid(project.uuid())
        .setBranchUuid(project.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue(analysisProject.getUuid()));
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    SnapshotDto analysisBranch1 = db.components().insertSnapshot(newSnapshot()
      .setRootComponentUuid(branch1.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    ComponentDto branch2 = db.components().insertProjectBranch(project);
    SnapshotDto analysisBranch2 = db.components().insertSnapshot(newSnapshot()
      .setRootComponentUuid(branch2.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    dbClient.newCodePeriodDao().insert(dbSession,
      new NewCodePeriodDto()
        .setProjectUuid(project.uuid())
        .setBranchUuid(branch2.uuid())
        .setType(NewCodePeriodType.SPECIFIC_ANALYSIS)
        .setValue(analysisBranch2.getUuid()));
    dbSession.commit();

    assertThat(underTest.selectProcessedAnalysisByComponentUuid(project.uuid(), dbSession))
      .isEmpty();
    assertThat(underTest.selectProcessedAnalysisByComponentUuid(branch1.uuid(), dbSession))
      .extracting(PurgeableAnalysisDto::getAnalysisUuid)
      .containsOnly(analysisBranch1.getUuid());
    assertThat(underTest.selectProcessedAnalysisByComponentUuid(branch2.uuid(), dbSession))
      .isEmpty();
  }

  @Test
  void delete_project_and_associated_data() {
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(project.getMainBranchComponent(), "a/b"));
    ComponentDto file = db.components().insertComponent(newFileDto(directory));
    SnapshotDto analysis = db.components().insertSnapshot(project.getMainBranchDto());
    IssueDto issue1 = db.issues().insert(rule, project.getMainBranchComponent(), file);
    IssueChangeDto issueChange1 = db.issues().insertChange(issue1);
    IssueDto issue2 = db.issues().insert(rule, project.getMainBranchComponent(), file);
    FileSourceDto fileSource = db.fileSources().insertFileSource(file);
    db.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(issue1));

    ProjectData otherProject = db.components().insertPrivateProject();
    ComponentDto otherDirectory = db.components().insertComponent(newDirectory(otherProject.getMainBranchComponent(), "a/b"));
    ComponentDto otherFile = db.components().insertComponent(newFileDto(otherDirectory));
    SnapshotDto otherAnalysis = db.components().insertSnapshot(otherProject.getMainBranchDto());
    IssueDto otherIssue1 = db.issues().insert(rule, otherProject.getMainBranchComponent(), otherFile);
    IssueChangeDto otherIssueChange1 = db.issues().insertChange(otherIssue1);
    IssueDto otherIssue2 = db.issues().insert(rule, otherProject.getMainBranchComponent(), otherFile);
    FileSourceDto otherFileSource = db.fileSources().insertFileSource(otherFile);
    db.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(otherIssue1));

    ProjectDto projectDto = project.getProjectDto();
    underTest.deleteProject(dbSession, projectDto.getUuid(), projectDto.getQualifier(), projectDto.getName(), projectDto.getKey());
    dbSession.commit();

    assertThat(uuidsIn("components")).containsOnly(otherProject.getMainBranchDto().getUuid(), otherDirectory.uuid(), otherFile.uuid());
    assertThat(uuidsIn("projects")).containsOnly(otherProject.getProjectDto().getUuid());
    assertThat(uuidsIn("snapshots")).containsOnly(otherAnalysis.getUuid());
    assertThat(uuidsIn("issues", "kee")).containsOnly(otherIssue1.getKey(), otherIssue2.getKey());
    assertThat(uuidsIn("issue_changes", "kee")).containsOnly(otherIssueChange1.getKey());
    assertThat(uuidsIn("new_code_reference_issues", "issue_key")).containsOnly(otherIssue1.getKey());
    assertThat(uuidsIn("file_sources", "file_uuid")).containsOnly(otherFileSource.getFileUuid());
  }

  @Test
  void delete_application() {
    MetricDto metric = db.measures().insertMetric();
    ProjectData project = db.components().insertPrivateProject();
    BranchDto mainBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getMainBranchDto().getUuid()).get();
    db.rules().insert();

    ProjectData app = db.components().insertPrivateApplication();
    BranchDto appBranch = db.components().insertProjectBranch(app.getProjectDto());
    ProjectData otherApp = db.components().insertPrivateApplication();
    BranchDto otherAppBranch = db.components().insertProjectBranch(otherApp.getProjectDto());

    SnapshotDto appAnalysis = db.components().insertSnapshot(app.getProjectDto());
    SnapshotDto appBranchAnalysis = db.components().insertSnapshot(appBranch);
    SnapshotDto otherAppAnalysis = db.components().insertSnapshot(otherApp.getProjectDto());
    SnapshotDto otherAppBranchAnalysis = db.components().insertSnapshot(otherAppBranch);

    db.measures().insertProjectMeasure(app.getMainBranchComponent(), appAnalysis, metric);
    ComponentDto appBranchComponent = db.components().getComponentDto(appBranch);
    db.measures().insertProjectMeasure(appBranchComponent, appBranchAnalysis, metric);
    ProjectMeasureDto otherAppMeasure = db.measures().insertProjectMeasure(otherApp.getMainBranchComponent(), otherAppAnalysis, metric);
    ComponentDto otherAppBranchComponent = db.components().getComponentDto(otherAppBranch);
    ProjectMeasureDto otherAppBranchMeasure = db.measures().insertProjectMeasure(otherAppBranchComponent, otherAppBranchAnalysis, metric);

    db.components().addApplicationProject(app.getProjectDto(), project.getProjectDto());
    db.components().addApplicationProject(otherApp.getProjectDto(), project.getProjectDto());
    db.components().addProjectBranchToApplicationBranch(dbClient.branchDao().selectByUuid(dbSession, appBranch.getUuid()).get(),
mainBranch);
    db.components().addProjectBranchToApplicationBranch(dbClient.branchDao().selectByUuid(dbSession, otherAppBranch.getUuid()).get(),
      mainBranch);

    ProjectDto appDto = app.getProjectDto();
    underTest.deleteProject(dbSession, appDto.getUuid(), appDto.getQualifier(), appDto.getName(), appDto.getKey());
    dbSession.commit();

    assertThat(uuidsIn("components")).containsOnly(project.getMainBranchDto().getUuid(), otherApp.getMainBranchDto().getUuid(),
     otherAppBranch.getUuid());
    assertThat(uuidsIn("projects")).containsOnly(project.getProjectDto().getUuid(), otherApp.getProjectDto().getUuid());
    assertThat(uuidsIn("snapshots")).containsOnly(otherAppAnalysis.getUuid(), otherAppBranchAnalysis.getUuid());
    assertThat(uuidsIn("project_branches")).containsOnly(project.getMainBranchDto().getUuid(), otherApp.getMainBranchDto().getUuid(),
      otherAppBranch.getUuid());
    assertThat(uuidsIn("project_measures")).containsOnly(otherAppMeasure.getUuid(), otherAppBranchMeasure.getUuid());
    assertThat(uuidsIn("app_projects", "application_uuid")).containsOnly(otherApp.getProjectDto().getUuid());
    assertThat(uuidsIn("app_branch_project_branch", "application_branch_uuid")).containsOnly(otherAppBranch.getUuid());
  }

  @Test
  void delete_application_branch() {
    MetricDto metric = db.measures().insertMetric();
    ProjectData project = db.components().insertPrivateProject();
    BranchDto projectBranch = db.getDbClient().branchDao().selectByUuid(db.getSession(), project.getMainBranchDto().getUuid()).get();
    db.rules().insert();

    ProjectData app = db.components().insertPrivateApplication();
    BranchDto appBranch = db.components().insertProjectBranch(app.getProjectDto());
    ProjectData otherApp = db.components().insertPrivateApplication();
    BranchDto otherAppBranch = db.components().insertProjectBranch(otherApp.getProjectDto());

    SnapshotDto appAnalysis = db.components().insertSnapshot(app.getProjectDto());
    SnapshotDto appBranchAnalysis = db.components().insertSnapshot(appBranch);
    SnapshotDto otherAppAnalysis = db.components().insertSnapshot(otherApp.getProjectDto());
    SnapshotDto otherAppBranchAnalysis = db.components().insertSnapshot(otherAppBranch);

    ProjectMeasureDto appMeasure = db.measures().insertProjectMeasure(app.getMainBranchComponent(), appAnalysis, metric);
    ComponentDto appBranchComponent = db.components().getComponentDto(appBranch);
    db.measures().insertProjectMeasure(appBranchComponent, appBranchAnalysis, metric);
    ProjectMeasureDto otherAppMeasure = db.measures().insertProjectMeasure(otherApp.getMainBranchComponent(), otherAppAnalysis, metric);
    ComponentDto otherAppBranchComponent = db.components().getComponentDto(otherAppBranch);
    ProjectMeasureDto otherAppBranchMeasure = db.measures().insertProjectMeasure(otherAppBranchComponent, otherAppBranchAnalysis, metric);

    db.components().addApplicationProject(app.getProjectDto(), project.getProjectDto());
    db.components().addApplicationProject(otherApp.getProjectDto(), project.getProjectDto());
    db.components().addProjectBranchToApplicationBranch(dbClient.branchDao().selectByUuid(dbSession, appBranch.getUuid()).get(),
projectBranch);
    db.components().addProjectBranchToApplicationBranch(dbClient.branchDao().selectByUuid(dbSession, otherAppBranch.getUuid()).get(),
projectBranch);

    // properties exist only for entities, not branches
    insertPropertyFor(app.getProjectDto());
    insertPropertyFor(otherApp.getProjectDto());

    insertReportScheduleAndSubscriptionForBranch(appBranch.getUuid(), dbSession);

    underTest.deleteBranch(dbSession, appBranch.getUuid());
    dbSession.commit();

    assertThat(uuidsIn("components")).containsOnly(project.getMainBranchDto().getUuid(), app.getMainBranchDto().getUuid(),
      otherApp.getMainBranchDto().getUuid(),
      otherAppBranch.getUuid());
    assertThat(uuidsIn("projects")).containsOnly(project.getProjectDto().getUuid(), app.getProjectDto().getUuid(),
otherApp.getProjectDto().getUuid());
    assertThat(uuidsIn("snapshots")).containsOnly(otherAppAnalysis.getUuid(), appAnalysis.getUuid(), otherAppBranchAnalysis.getUuid());
    assertThat(uuidsIn("project_branches")).containsOnly(project.getMainBranchDto().getUuid(), app.getMainBranchDto().getUuid(),
      otherApp.getMainBranchDto().getUuid(),
      otherAppBranch.getUuid());
    assertThat(uuidsIn("project_measures")).containsOnly(appMeasure.getUuid(), otherAppMeasure.getUuid(), otherAppBranchMeasure.getUuid());
    assertThat(uuidsIn("app_projects", "application_uuid")).containsOnly(app.getProjectDto().getUuid(), otherApp.getProjectDto().getUuid());
    assertThat(uuidsIn("app_branch_project_branch", "application_branch_uuid")).containsOnly(otherAppBranch.getUuid());
    //properties should not change as we are deleting a branch, not application
    assertThat(componentUuidsIn("properties")).containsOnly(otherApp.projectUuid(), app.projectUuid());
    assertThat(uuidsIn("report_schedules", "branch_uuid")).isEmpty();
    assertThat(uuidsIn("report_subscriptions", "branch_uuid")).isEmpty();

  }

  private void insertReportScheduleAndSubscriptionForBranch(String branchUuid, DbSession dbSession) {
    db.getDbClient().reportSubscriptionDao().insert(dbSession, new ReportSubscriptionDto().setUuid("uuid")
      .setUserUuid("userUuid")
      .setBranchUuid(branchUuid));

    db.getDbClient().reportScheduleDao().upsert(dbSession, new ReportScheduleDto().setUuid("uuid")
      .setBranchUuid(branchUuid)
      .setLastSendTimeInMs(2));
  }

  private void insertReportScheduleAndSubscriptionForPortfolio(String uuid, String portfolioUuid, DbSession dbSession) {
    db.getDbClient().reportSubscriptionDao().insert(dbSession, new ReportSubscriptionDto().setUuid(uuid)
      .setUserUuid("userUuid")
      .setPortfolioUuid(portfolioUuid));

    db.getDbClient().reportScheduleDao().upsert(dbSession, new ReportScheduleDto().setUuid(uuid)
      .setPortfolioUuid(portfolioUuid)
      .setLastSendTimeInMs(2));
  }

  @Test
  void delete_webhooks_from_project() {
    ProjectDto project1 = db.components().insertPrivateProject().getProjectDto();
    WebhookDto webhook = db.webhooks().insertWebhook(project1);
    db.webhookDelivery().insert(webhook);
    ProjectDto projectNotToBeDeleted = db.components().insertPrivateProject().getProjectDto();
    WebhookDto webhookNotDeleted = db.webhooks().insertWebhook(projectNotToBeDeleted);
    WebhookDeliveryLiteDto webhookDeliveryNotDeleted = db.webhookDelivery().insert(webhookNotDeleted);

    underTest.deleteProject(dbSession, project1.getUuid(), project1.getQualifier(), project1.getName(), project1.getKey());

    assertThat(uuidsIn("webhooks")).containsOnly(webhookNotDeleted.getUuid());
    assertThat(uuidsIn("webhook_deliveries")).containsOnly(webhookDeliveryNotDeleted.getUuid());
  }

  private Stream<String> uuidsOfTable(String tableName) {
    return db.select("select uuid as \"UUID\" from " + tableName)
      .stream()
      .map(s -> (String) s.get("UUID"));
  }

  @Test
  void delete_row_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto();
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto();
    insertComponents(List.of(anotherLivingProject), List.of(projectToBeDeleted));
    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeActivityDto toBeDeletedActivity = insertCeActivity(projectToBeDeleted, projectToBeDeleted.uuid());
    CeActivityDto notDeletedActivity = insertCeActivity(anotherLivingProject, anotherLivingProject.uuid());
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid(), projectToBeDeleted.qualifier(), projectToBeDeleted.name(),
      projectToBeDeleted.getKey());
    dbSession.commit();

    assertThat(uuidsOfTable("ce_activity"))
      .containsOnly(notDeletedActivity.getUuid())
      .hasSize(1);
  }

  @Test
  void delete_row_in_ce_task_input_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto anotherBranch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ProjectData anotherProject = db.components().insertPrivateProject();

    CeActivityDto projectTask = insertCeActivity(project.getMainBranchComponent(), project.getProjectDto().getUuid());
    insertCeTaskInput(projectTask.getUuid());
    CeActivityDto branchTask = insertCeActivity(branch, project.getProjectDto().getUuid());
    insertCeTaskInput(branchTask.getUuid());
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch, project.getProjectDto().getUuid());
    insertCeTaskInput(anotherBranchTask.getUuid());
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject.getMainBranchComponent(), anotherProject.getProjectDto().getUuid());
    insertCeTaskInput(anotherProjectTask.getUuid());
    insertCeTaskInput("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.getProjectDto().getName(),
      project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(),
anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.getProjectDto().getUuid(), project.getProjectDto().getQualifier(),
     project.getProjectDto().getName(),
      project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_row_in_ce_scanner_context_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto anotherBranch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ProjectData anotherProject = db.components().insertPrivateProject();

    CeActivityDto projectTask = insertCeActivity(project.getMainBranchComponent(), project.getProjectDto().getUuid());
    insertCeScannerContext(projectTask.getUuid());
    CeActivityDto branchTask = insertCeActivity(branch, project.getProjectDto().getUuid());
    insertCeScannerContext(branchTask.getUuid());
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch, project.getProjectDto().getUuid());
    insertCeScannerContext(anotherBranchTask.getUuid());
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject.getMainBranchComponent(), anotherProject.getProjectDto().getUuid());
    insertCeScannerContext(anotherProjectTask.getUuid());
    insertCeScannerContext("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.getProjectDto().getName(),
      project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(),
      anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.getProjectDto().getUuid(), project.getProjectDto().getQualifier(),
      project.getProjectDto().getName(),
      project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_row_in_ce_task_characteristics_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto anotherBranch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ProjectData anotherProject = db.components().insertPrivateProject();

    CeActivityDto projectTask = insertCeActivity(project.getMainBranchComponent(), project.projectUuid());
    insertCeTaskCharacteristics(projectTask.getUuid(), 3);
    CeActivityDto branchTask = insertCeActivity(branch, project.projectUuid());
    insertCeTaskCharacteristics(branchTask.getUuid(), 2);
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch, project.projectUuid());
    insertCeTaskCharacteristics(anotherBranchTask.getUuid(), 6);
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject.getMainBranchComponent(), anotherProject.projectUuid());
    insertCeTaskCharacteristics(anotherProjectTask.getUuid(), 2);
    insertCeTaskCharacteristics("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.getProjectDto().getName(),
project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(),
     anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.getProjectDto().getUuid(), project.getProjectDto().getQualifier(),
     project.getProjectDto().getName(),
      project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_row_in_ce_task_message_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ComponentDto anotherBranch = db.components().insertProjectBranch(project.getMainBranchComponent());
    ProjectData anotherProject = db.components().insertPrivateProject();

    CeActivityDto projectTask = insertCeActivity(project.getMainBranchComponent(), project.getProjectDto().getUuid());
    insertCeTaskMessages(projectTask.getUuid(), 3);
    CeActivityDto branchTask = insertCeActivity(branch, project.getProjectDto().getUuid());
    insertCeTaskMessages(branchTask.getUuid(), 2);
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch, project.getProjectDto().getUuid());
    insertCeTaskMessages(anotherBranchTask.getUuid(), 6);
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject.getMainBranchComponent(), anotherProject.getProjectDto().getUuid());
    insertCeTaskMessages(anotherProjectTask.getUuid(), 2);
    insertCeTaskMessages("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.getProjectDto().getName(),
     project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(),
     anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.getProjectDto().getUuid(), project.getProjectDto().getQualifier(),
      project.getProjectDto().getName(),
      project.getProjectDto().getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_rows_in_user_dismissed_messages_when_deleting_project() {
    UserDto user1 = db.users().insertUser();
    UserDto user2 = db.users().insertUser();
    ProjectDto project = db.components().insertPrivateProject().getProjectDto();
    ProjectDto anotherProject = db.components().insertPrivateProject().getProjectDto();

    UserDismissedMessageDto msg1 = db.users().insertUserDismissedMessageOnProject(user1, project,
      MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto msg2 = db.users().insertUserDismissedMessageOnProject(user2, project,
      MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);
    UserDismissedMessageDto msg3 = db.users().insertUserDismissedMessageOnProject(user1, anotherProject,
     MessageType.SUGGEST_DEVELOPER_EDITION_UPGRADE);

    assertThat(uuidsIn("user_dismissed_messages")).containsOnly(msg1.getUuid(), msg2.getUuid(), msg3.getUuid());

    underTest.deleteProject(dbSession, project.getUuid(), project.getQualifier(), project.getName(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("user_dismissed_messages")).containsOnly(msg3.getUuid());
  }

  @Test
  void delete_tasks_in_ce_queue_when_deleting_project() {
    ProjectData projectToBeDeleted = db.components().insertPrivateProject();
    ProjectData anotherLivingProject = db.components().insertPrivateProject();

    // Insert 3 rows in CE_QUEUE: two for the project that will be deleted (in order to check that status
    // is not involved in deletion), and one on another project
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(projectToBeDeleted.getMainBranchComponent(), projectToBeDeleted.projectUuid(),
      Status.PENDING));
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(projectToBeDeleted.getMainBranchComponent(), projectToBeDeleted.projectUuid(),
     Status.IN_PROGRESS));
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(anotherLivingProject.getMainBranchComponent(),
      anotherLivingProject.projectUuid(), Status.PENDING));
    dbSession.commit();

    ProjectDto projectDto = projectToBeDeleted.getProjectDto();
    underTest.deleteProject(dbSession, projectDto.getUuid(),
      projectDto.getQualifier(), projectDto.getName(), projectDto.getKey());
    dbSession.commit();

    assertThat(db.countRowsOfTable("ce_queue")).isOne();
    assertThat(db.countSql("select count(*) from ce_queue where entity_uuid='" + projectToBeDeleted.projectUuid() + "'")).isZero();
  }

  @Test
  void delete_row_in_ce_task_input_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto branch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto();

    insertComponents(List.of(project, anotherProject), List.of(branch, anotherBranch));

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeTaskInput(projectTask.getUuid());
    CeQueueDto branchTask = insertCeQueue(branch, project.uuid());
    insertCeTaskInput(branchTask.getUuid());
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch, project.uuid());
    insertCeTaskInput(anotherBranchTask.getUuid());
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeTaskInput(anotherProjectTask.getUuid());
    insertCeTaskInput("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(),
      anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_row_in_ce_scanner_context_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto branch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto();

    insertComponents(List.of(project, anotherProject), List.of(branch, anotherBranch));

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeScannerContext(projectTask.getUuid());
    CeQueueDto branchTask = insertCeQueue(branch, project.uuid());
    insertCeScannerContext(branchTask.getUuid());
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch, project.uuid());
    insertCeScannerContext(anotherBranchTask.getUuid());
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeScannerContext(anotherProjectTask.getUuid());
    insertCeScannerContext("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context"))
      .containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  private void insertComponents(List<ComponentDto> componentsOnMainBranch, List<ComponentDto> componentsNotOnMainBranch) {
    componentsOnMainBranch.forEach(c -> dbClient.componentDao().insert(dbSession, c, true));
    componentsNotOnMainBranch.forEach(c -> dbClient.componentDao().insert(dbSession, c, false));
  }

  @Test
  void delete_row_in_ce_task_characteristics_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto branch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto();

    insertComponents(List.of(project, anotherProject), List.of(branch, anotherBranch));

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeTaskCharacteristics(projectTask.getUuid(), 3);
    CeQueueDto branchTask = insertCeQueue(branch, project.uuid());
    insertCeTaskCharacteristics(branchTask.getUuid(), 1);
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch, project.uuid());
    insertCeTaskCharacteristics(anotherBranchTask.getUuid(), 5);
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeTaskCharacteristics(anotherProjectTask.getUuid(), 2);
    insertCeTaskCharacteristics("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics"))
      .containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_row_in_ce_task_message_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto branch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto();

    insertComponents(List.of(project, anotherProject), List.of(branch, anotherBranch));

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeTaskMessages(projectTask.getUuid(), 3);
    CeQueueDto branchTask = insertCeQueue(branch, project.uuid());
    insertCeTaskMessages(branchTask.getUuid(), 1);
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch, project.uuid());
    insertCeTaskMessages(anotherBranchTask.getUuid(), 5);
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeTaskMessages(anotherProjectTask.getUuid(), 2);
    insertCeTaskMessages("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message"))
      .containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  void delete_row_in_events_and_event_component_changes_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto();
    ComponentDto branch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newBranchComponent(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto();

    insertComponents(List.of(project, anotherProject), List.of(branch, anotherBranch));

    SnapshotDto projectAnalysis1 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis2 = db.components().insertSnapshot(project);
    EventDto projectEvent1 = db.events().insertEvent(projectAnalysis1);
    EventDto projectEvent2 = db.events().insertEvent(projectAnalysis2);
    EventDto projectEvent3 = db.events().insertEvent(db.components().insertSnapshot(project));
    SnapshotDto branchAnalysis1 = db.components().insertSnapshot(branch);
    SnapshotDto branchAnalysis2 = db.components().insertSnapshot(branch);
    EventDto branchEvent1 = db.events().insertEvent(branchAnalysis1);
    EventDto branchEvent2 = db.events().insertEvent(branchAnalysis2);
    SnapshotDto anotherBranchAnalysis = db.components().insertSnapshot(anotherBranch);
    EventDto anotherBranchEvent = db.events().insertEvent(anotherBranchAnalysis);
    SnapshotDto anotherProjectAnalysis = db.components().insertSnapshot(anotherProject);
    EventDto anotherProjectEvent = db.events().insertEvent(anotherProjectAnalysis);
    ComponentDto referencedProjectA = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto referencedProjectB = db.components().insertPublicProject().getMainBranchComponent();
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectA, null);
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectB, null);
    BranchDto branchProjectA = newBranchDto(referencedProjectA);
    ComponentDto cptBranchProjectA = ComponentTesting.newBranchComponent(referencedProjectA, branchProjectA);
    db.events().insertEventComponentChanges(projectEvent2, projectAnalysis2, randomChangeCategory(), cptBranchProjectA, branchProjectA);
    // note: projectEvent3 has no component change
    db.events().insertEventComponentChanges(branchEvent1, branchAnalysis1, randomChangeCategory(), referencedProjectB, null);
    db.events().insertEventComponentChanges(branchEvent2, branchAnalysis2, randomChangeCategory(), cptBranchProjectA, branchProjectA);
    db.events().insertEventComponentChanges(anotherBranchEvent, anotherBranchAnalysis, randomChangeCategory(), referencedProjectB, null);
    db.events().insertEventComponentChanges(anotherProjectEvent, anotherProjectAnalysis, randomChangeCategory(), referencedProjectB, null);

    // deleting referenced project does not delete any data
    underTest.deleteProject(dbSession, referencedProjectA.uuid(), referencedProjectA.qualifier(), project.name(), project.getKey());

    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(7);
    assertThat(db.countRowsOfTable("events"))
      .isEqualTo(7);

    underTest.deleteProject(dbSession, branch.uuid(), branch.qualifier(), project.name(), project.getKey());
    assertThat(uuidsIn("event_component_changes", "event_component_uuid"))
      .containsOnly(project.uuid(), anotherBranch.uuid(), anotherProject.uuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(5);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent1.getUuid(), projectEvent2.getUuid(), projectEvent3.getUuid(), anotherBranchEvent.getUuid(),
        anotherProjectEvent.getUuid());

    underTest.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());
    assertThat(uuidsIn("event_component_changes", "event_component_uuid"))
      .containsOnly(anotherBranch.uuid(), anotherProject.uuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(2);
    assertThat(uuidsIn("events"))
      .containsOnly(anotherBranchEvent.getUuid(), anotherProjectEvent.getUuid());
  }

  private static EventComponentChangeDto.ChangeCategory randomChangeCategory() {
    return EventComponentChangeDto.ChangeCategory.values()[new Random().nextInt(EventComponentChangeDto.ChangeCategory.values().length)];
  }

  private ProjectDto insertProjectWithBranchAndRelatedData() {
    RuleDto rule = db.rules().insert();
    ProjectData project = db.components().insertPublicProject();
    BranchDto branch = db.components().insertProjectBranch(project.getProjectDto());
    ComponentDto branchComponent = db.components().getComponentDto(branch);
    ComponentDto dir = db.components().insertComponent(newDirectory(branchComponent, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(branchComponent, dir));
    db.issues().insert(rule, branchComponent, file);
    db.issues().insert(rule, branchComponent, dir);
    return project.getProjectDto();
  }

  @Test
  void delete_branch_content_when_deleting_project() {
    ProjectDto anotherLivingProject = insertProjectWithBranchAndRelatedData();
    int projectEntryCount = db.countRowsOfTable("components");
    int issueCount = db.countRowsOfTable("issues");
    int branchCount = db.countRowsOfTable("project_branches");
    insertPropertyFor(anotherLivingProject);

    assertThat(db.countRowsOfTable("properties")).isEqualTo(1);

    ProjectDto projectToDelete = insertProjectWithBranchAndRelatedData();
    insertPropertyFor(projectToDelete);

    assertThat(db.countRowsOfTable("properties")).isEqualTo(2);
    assertThat(db.countRowsOfTable("components")).isGreaterThan(projectEntryCount);
    assertThat(db.countRowsOfTable("issues")).isGreaterThan(issueCount);
    assertThat(db.countRowsOfTable("project_branches")).isGreaterThan(branchCount);

    underTest.deleteProject(dbSession, projectToDelete.getUuid(), projectToDelete.getQualifier(), projectToDelete.getName(),
      projectToDelete.getKey());
    dbSession.commit();

    assertThat(db.countRowsOfTable("components")).isEqualTo(projectEntryCount);
    assertThat(db.countRowsOfTable("issues")).isEqualTo(issueCount);
    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(branchCount);
    assertThat(db.countRowsOfTable("properties")).isEqualTo(1);
  }

  @Test
  void delete_portfolio_branch_when_deleting_branch() {
    ComponentDto root = db.components()
      .insertPrivatePortfolio(component -> component.setKey("ROOT"), portfolio -> portfolio.setSelectionMode(MANUAL));
    PortfolioDto rootPortfolio = db.components().getPortfolioDto(root);
    var project1 = db.components().insertPrivateProject(project -> project.setKey("project:one").setName("Project One")).getProjectDto();

    db.components().addPortfolioProject(rootPortfolio, project1);
    BranchDto mainBranch = db.getDbClient().branchDao().selectMainBranchByProjectUuid(db.getSession(), project1.getUuid()).orElseThrow();
    db.components().addPortfolioProjectBranch(rootPortfolio, project1, mainBranch.getUuid());

    var featureBranch = db.components().insertProjectBranch(project1, b -> b.setKey("feature-branch"));
    db.components().addPortfolioProjectBranch(rootPortfolio, project1, featureBranch.getUuid());
    db.commit();

    db.getDbClient().portfolioDao().selectPortfolioProjects(dbSession, rootPortfolio.getUuid())
      .forEach(pp -> assertThat(pp.getBranchUuids()).containsExactlyInAnyOrder(featureBranch.getUuid(), mainBranch.getUuid()));

    underTest.deleteBranch(dbSession, featureBranch.getUuid());
    dbSession.commit();

    db.getDbClient().portfolioDao().selectPortfolioProjects(dbSession, rootPortfolio.getUuid())
      .forEach(pp -> assertThat(pp.getBranchUuids()).containsOnly(mainBranch.getUuid()));
  }

  @Test
  void delete_view_and_child() {
    ComponentDto project = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subView = db.components().insertComponent(newSubPortfolio(view));
    ComponentDto projectCopy = db.components().insertComponent(newProjectCopy(project, subView));
    ComponentDto otherView = db.components().insertPrivatePortfolio();
    ComponentDto otherSubView = db.components().insertComponent(newSubPortfolio(otherView));
    ComponentDto otherProjectCopy = db.components().insertComponent(newProjectCopy(project, otherSubView));

    underTest.deleteProject(dbSession, view.uuid(), view.qualifier(), project.name(), project.getKey());
    dbSession.commit();

    assertThat(uuidsIn("components"))
      .containsOnly(project.uuid(), otherView.uuid(), otherSubView.uuid(), otherProjectCopy.uuid());
  }

  @Test
  void purge_shouldDeleteOrphanIssues() {
    RuleDto rule = db.rules().insert();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto otherProject = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto dir = db.components().insertComponent(newDirectory(mainBranch, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch, dir));

    IssueDto issue1 = db.issues().insert(rule, mainBranch, file, issue -> issue.replaceAllImpacts(Collections.emptyList()));
    IssueDto orphanIssue = db.issues().insert(rule, mainBranch, file, issue -> issue.setComponentUuid("nonExisting"));
    IssueChangeDto orphanIssueChange = db.issues().insertChange(orphanIssue);
    db.issues().insertNewCodeReferenceIssue(orphanIssue);

    underTest.purge(dbSession, newConfigurationWith30Days(system2, mainBranch.uuid(), projectData.projectUuid()), PurgeListener.EMPTY,
      new PurgeProfiler());
    db.commit();

    assertThat(db.countRowsOfTable("issue_changes")).isZero();
    assertThat(db.countRowsOfTable("new_code_reference_issues")).isZero();
    assertThat(db.countRowsOfTable("issues_impacts")).isZero();
    assertThat(db.select("select kee as \"KEE\" from issues")).extracting(i -> i.get("KEE")).containsOnly(issue1.getKey());
  }

  @Test
  void should_delete_old_closed_issues() {
    PurgeListener listener = mock(PurgeListener.class);
    RuleDto rule = db.rules().insert();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch = projectData.getMainBranchComponent();

    ComponentDto dir = db.components().insertComponent(newDirectory(mainBranch, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(mainBranch, dir));

    IssueDto oldClosed = db.issues().insert(rule, mainBranch, file, issue -> {
      issue.setStatus("CLOSED");
      issue.setIssueCloseDate(DateUtils.addDays(new Date(), -31));
    });
    db.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(oldClosed));

    IssueDto notOldEnoughClosed = db.issues().insert(rule, mainBranch, file, issue -> {
      issue.setStatus("CLOSED");
      issue.setIssueCloseDate(new Date());
    });
    db.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(notOldEnoughClosed));
    IssueDto notClosed = db.issues().insert(rule, mainBranch, file);
    db.issues().insertNewCodeReferenceIssue(newCodeReferenceIssue(notClosed));

    when(system2.now()).thenReturn(new Date().getTime());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, mainBranch.uuid(), projectData.projectUuid()), listener,
     new PurgeProfiler());
    dbSession.commit();

    // old closed got deleted
    assertThat(db.getDbClient().issueDao().selectByKey(dbSession, oldClosed.getKey())).isEmpty();

    // others remain
    assertThat(db.countRowsOfTable("issues")).isEqualTo(2);

    Optional<IssueDto> notOldEnoughClosedFromQuery = db.getDbClient().issueDao().selectByKey(dbSession, notOldEnoughClosed.getKey());
    assertThat(notOldEnoughClosedFromQuery).isNotEmpty();
    assertThat(notOldEnoughClosedFromQuery.get().isNewCodeReferenceIssue()).isTrue();

    Optional<IssueDto> notClosedFromQuery = db.getDbClient().issueDao().selectByKey(dbSession, notClosed.getKey());
    assertThat(notClosedFromQuery).isNotEmpty();
    assertThat(notClosedFromQuery.get().isNewCodeReferenceIssue()).isTrue();

    Optional<IssueDto> oldClosedFromQuery = db.getDbClient().issueDao().selectByKey(dbSession, oldClosed.getKey());
    assertThat(oldClosedFromQuery).isEmpty();

    verify(listener, only()).onIssuesRemoval(projectData.projectUuid(), List.of(oldClosed.getKee()));
  }

  @Test
  void delete_disabled_components_without_issues() {
    ProjectData projectData = db.components().insertPublicProject(p -> p.setEnabled(true));
    ComponentDto mainBranch = projectData.getMainBranchComponent();
    ComponentDto enabledFileWithIssues = db.components().insertComponent(newFileDto(mainBranch).setEnabled(true));
    ComponentDto disabledFileWithIssues = db.components().insertComponent(newFileDto(mainBranch).setEnabled(false));
    ComponentDto enabledFileWithoutIssues = db.components().insertComponent(newFileDto(mainBranch).setEnabled(true));
    ComponentDto disabledFileWithoutIssues = db.components().insertComponent(newFileDto(mainBranch).setEnabled(false));

    RuleDto rule = db.rules().insert();
    IssueDto closed1 = db.issues().insert(rule, mainBranch, enabledFileWithIssues, issue -> {
      issue.setStatus("CLOSED");
      issue.setResolution(Issue.RESOLUTION_FIXED);
      issue.setIssueCloseDate(new Date());
    });
    IssueDto closed2 = db.issues().insert(rule, mainBranch, disabledFileWithIssues, issue -> {
      issue.setStatus("CLOSED");
      issue.setResolution(Issue.RESOLUTION_FIXED);
      issue.setIssueCloseDate(new Date());
    });
    PurgeListener purgeListener = mock(PurgeListener.class);

    Set<String> disabledComponentUuids = ImmutableSet.of(disabledFileWithIssues.uuid(), disabledFileWithoutIssues.uuid());
    underTest.purge(dbSession,
      newConfigurationWith30Days(System2.INSTANCE, mainBranch.uuid(), projectData.projectUuid(), disabledComponentUuids),
      purgeListener, new PurgeProfiler());

    assertThat(db.getDbClient().componentDao().selectByBranchUuid(mainBranch.uuid(), dbSession))
      .extracting("uuid")
      .containsOnly(mainBranch.uuid(), enabledFileWithIssues.uuid(), disabledFileWithIssues.uuid(),
        enabledFileWithoutIssues.uuid());
  }

  @Test
  void delete_ce_analysis_older_than_180_and_scanner_context_older_than_40_days_of_specified_project_when_purging_project() {
    LocalDateTime now = LocalDateTime.now();
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    Consumer<CeQueueDto> belongsToProject1 = t -> t.setEntityUuid(projectData1.projectUuid()).setComponentUuid(mainBranch1.uuid());
    ProjectData projectData2 = db.components().insertPublicProject();
    ComponentDto project2 = projectData2.getMainBranchComponent();
    Consumer<CeQueueDto> belongsToProject2 = t -> t.setEntityUuid(projectData2.projectUuid()).setComponentUuid(project2.uuid());

    insertCeActivityAndChildDataWithDate("VERY_OLD_1", now.minusDays(180).minusMonths(10), belongsToProject1);
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH_1", now.minusDays(180).minusDays(1), belongsToProject1);
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH_1", now.minusDays(180), belongsToProject1);
    insertCeActivityAndChildDataWithDate("RECENT_1", now.minusDays(1), belongsToProject1);
    insertCeActivityAndChildDataWithDate("VERY_OLD_2", now.minusDays(180).minusMonths(10), belongsToProject2);
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH_2", now.minusDays(180).minusDays(1), belongsToProject2);
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH_2", now.minusDays(180), belongsToProject2);
    insertCeActivityAndChildDataWithDate("RECENT_2", now.minusDays(1), belongsToProject2);

    when(system2.now()).thenReturn(now.toInstant(ZoneOffset.UTC).toEpochMilli());
    underTest.purge(db.getSession(), newConfigurationWith30Days(System2.INSTANCE, mainBranch1.uuid(), projectData1.projectUuid()),
      PurgeListener.EMPTY, new PurgeProfiler());

    assertThat(selectActivity("VERY_OLD_1")).isEmpty();
    assertThat(selectTaskInput("VERY_OLD_1")).isEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD_1")).isEmpty();
    assertThat(scannerContextExists("VERY_OLD_1")).isFalse();
    assertThat(selectActivity("VERY_OLD_2")).isNotEmpty();
    assertThat(selectTaskInput("VERY_OLD_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD_2")).hasSize(1);
    assertThat(scannerContextExists("VERY_OLD_2")).isTrue();

    assertThat(selectActivity("JUST_OLD_ENOUGH_1")).isEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH_1")).isEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH_1")).isEmpty();
    assertThat(scannerContextExists("JUST_OLD_ENOUGH_1")).isFalse();
    assertThat(selectActivity("JUST_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH_2")).hasSize(1);
    assertThat(scannerContextExists("JUST_OLD_ENOUGH_2")).isTrue();

    assertThat(selectActivity("NOT_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH_1")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH_1")).isFalse(); // because more than 4 weeks old
    assertThat(selectActivity("NOT_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH_2")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH_2")).isTrue();

    assertThat(selectActivity("RECENT_1")).isNotEmpty();
    assertThat(selectTaskInput("RECENT_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT_1")).hasSize(1);
    assertThat(scannerContextExists("RECENT_1")).isTrue();
    assertThat(selectActivity("RECENT_2")).isNotEmpty();
    assertThat(selectTaskInput("RECENT_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT_2")).hasSize(1);
    assertThat(scannerContextExists("RECENT_2")).isTrue();
  }

  @Test
  void delete_ce_analysis_older_than_180_and_scanner_context_older_than_40_days_of_project_and_branches_when_purging_project() {
    LocalDateTime now = LocalDateTime.now();
    ProjectData projectData = db.components().insertPublicProject();
    ComponentDto mainBranch1 = projectData.getMainBranchComponent();
    ComponentDto branch1 = db.components().insertProjectBranch(mainBranch1, b -> b.setExcludeFromPurge(true));
    Consumer<CeQueueDto> belongsToMainBranch1 = t -> t.setEntityUuid(projectData.projectUuid()).setComponentUuid(mainBranch1.uuid());
    Consumer<CeQueueDto> belongsToBranch1 = t -> t.setEntityUuid(projectData.projectUuid()).setComponentUuid(branch1.uuid());

    insertCeActivityAndChildDataWithDate("VERY_OLD_1", now.minusDays(180).minusMonths(10), belongsToMainBranch1);
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH_1", now.minusDays(180).minusDays(1), belongsToMainBranch1);
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH_1", now.minusDays(180), belongsToMainBranch1);
    insertCeActivityAndChildDataWithDate("RECENT_1", now.minusDays(1), belongsToMainBranch1);
    insertCeActivityAndChildDataWithDate("VERY_OLD_2", now.minusDays(180).minusMonths(10), belongsToBranch1);
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH_2", now.minusDays(180).minusDays(1), belongsToBranch1);
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH_2", now.minusDays(180), belongsToBranch1);
    insertCeActivityAndChildDataWithDate("RECENT_2", now.minusDays(1), belongsToBranch1);

    when(system2.now()).thenReturn(now.toInstant(ZoneOffset.UTC).toEpochMilli());
    underTest.purge(db.getSession(), newConfigurationWith30Days(System2.INSTANCE, mainBranch1.uuid(), projectData.projectUuid()),
      PurgeListener.EMPTY, new PurgeProfiler());

    assertThat(selectActivity("VERY_OLD_1")).isEmpty();
    assertThat(selectTaskInput("VERY_OLD_1")).isEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD_1")).isEmpty();
    assertThat(scannerContextExists("VERY_OLD_1")).isFalse();
    assertThat(selectActivity("VERY_OLD_2")).isEmpty();
    assertThat(selectTaskInput("VERY_OLD_2")).isEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD_2")).isEmpty();
    assertThat(scannerContextExists("VERY_OLD_2")).isFalse();

    assertThat(selectActivity("JUST_OLD_ENOUGH_1")).isEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH_1")).isEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH_1")).isEmpty();
    assertThat(scannerContextExists("JUST_OLD_ENOUGH_1")).isFalse();
    assertThat(selectActivity("JUST_OLD_ENOUGH_2")).isEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH_2")).isEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH_2")).isEmpty();
    assertThat(scannerContextExists("JUST_OLD_ENOUGH_2")).isFalse();

    assertThat(selectActivity("NOT_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH_1")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH_1")).isFalse(); // because more than 4 weeks old
    assertThat(selectActivity("NOT_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH_2")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH_2")).isFalse(); // because more than 4 weeks old

    assertThat(selectActivity("RECENT_1")).isNotEmpty();
    assertThat(selectTaskInput("RECENT_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT_1")).hasSize(1);
    assertThat(scannerContextExists("RECENT_1")).isTrue();
    assertThat(selectActivity("RECENT_2")).isNotEmpty();
    assertThat(selectTaskInput("RECENT_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT_2")).hasSize(1);
    assertThat(scannerContextExists("RECENT_2")).isTrue();
  }

  @Test
  void delete_ce_analysis_of_branch_older_than_180_and_scanner_context_older_than_40_days_when_purging_branch() {
    LocalDateTime now = LocalDateTime.now();
    ProjectData projectData1 = db.components().insertPublicProject();
    ComponentDto mainBranch1 = projectData1.getMainBranchComponent();
    ComponentDto branch1 = db.components().insertProjectBranch(mainBranch1);
    Consumer<CeQueueDto> belongsToProject1 = t -> t.setEntityUuid(mainBranch1.uuid()).setComponentUuid(mainBranch1.uuid());
    Consumer<CeQueueDto> belongsToBranch1 = t -> t.setEntityUuid(mainBranch1.uuid()).setComponentUuid(branch1.uuid());

    insertCeActivityAndChildDataWithDate("VERY_OLD_1", now.minusDays(180).minusMonths(10), belongsToProject1);
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH_1", now.minusDays(180).minusDays(1), belongsToProject1);
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH_1", now.minusDays(180), belongsToProject1);
    insertCeActivityAndChildDataWithDate("RECENT_1", now.minusDays(1), belongsToProject1);
    insertCeActivityAndChildDataWithDate("VERY_OLD_2", now.minusDays(180).minusMonths(10), belongsToBranch1);
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH_2", now.minusDays(180).minusDays(1), belongsToBranch1);
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH_2", now.minusDays(180), belongsToBranch1);
    insertCeActivityAndChildDataWithDate("RECENT_2", now.minusDays(1), belongsToBranch1);

    when(system2.now()).thenReturn(now.toInstant(ZoneOffset.UTC).toEpochMilli());
    underTest.purge(db.getSession(), newConfigurationWith30Days(System2.INSTANCE, branch1.uuid(), projectData1.projectUuid()),
      PurgeListener.EMPTY, new PurgeProfiler());

    assertThat(selectActivity("VERY_OLD_1")).isNotEmpty();
    assertThat(selectTaskInput("VERY_OLD_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD_1")).hasSize(1);
    assertThat(scannerContextExists("VERY_OLD_1")).isTrue();
    assertThat(selectActivity("VERY_OLD_2")).isEmpty();
    assertThat(selectTaskInput("VERY_OLD_2")).isEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD_2")).isEmpty();
    assertThat(scannerContextExists("VERY_OLD_2")).isFalse();

    assertThat(selectActivity("JUST_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH_1")).hasSize(1);
    assertThat(scannerContextExists("JUST_OLD_ENOUGH_1")).isTrue();
    assertThat(selectActivity("JUST_OLD_ENOUGH_2")).isEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH_2")).isEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH_2")).isEmpty();
    assertThat(scannerContextExists("JUST_OLD_ENOUGH_2")).isFalse();

    assertThat(selectActivity("NOT_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH_1")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH_1")).isTrue();
    assertThat(selectActivity("NOT_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH_2")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH_2")).isFalse(); // because more than 4 weeks old

    assertThat(selectActivity("RECENT_1")).isNotEmpty();
    assertThat(selectTaskInput("RECENT_1")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT_1")).hasSize(1);
    assertThat(scannerContextExists("RECENT_1")).isTrue();
    assertThat(selectActivity("RECENT_2")).isNotEmpty();
    assertThat(selectTaskInput("RECENT_2")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT_2")).hasSize(1);
    assertThat(scannerContextExists("RECENT_2")).isTrue();
  }

  @Test
  void deleteProject_deletes_webhook_deliveries() {
    ComponentDto project = db.components().insertPublicProject().getMainBranchComponent();
    dbClient.webhookDeliveryDao().insert(dbSession,
      newDto().setProjectUuid(project.uuid()).setUuid("D1").setDurationMs(1000).setWebhookUuid("webhook-uuid"));
    dbClient.webhookDeliveryDao().insert(dbSession, newDto().setProjectUuid("P2").setUuid("D2").setDurationMs(1000).setWebhookUuid(
      "webhook-uuid"));

    underTest.deleteProject(dbSession, project.uuid(), project.qualifier(), project.name(), project.getKey());

    assertThat(selectAllDeliveryUuids(db, dbSession)).containsOnly("D2");
  }

  @Test
  void deleteProject_deletes_project_alm_settings() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    AlmSettingDto almSettingDto = db.almSettings().insertGitlabAlmSetting();
    db.almSettings().insertGitlabProjectAlmSetting(almSettingDto, project);
    db.almSettings().insertGitlabProjectAlmSetting(almSettingDto, otherProject);

    underTest.deleteProject(dbSession, project.getUuid(), project.getQualifier(), project.getName(), project.getKey());

    assertThat(dbClient.projectAlmSettingDao().selectByProject(dbSession, project)).isEmpty();
    assertThat(dbClient.projectAlmSettingDao().selectByProject(dbSession, otherProject)).isNotEmpty();
  }

  @Test
  void deleteBranch_deletes_scanner_cache() throws IOException {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project);
    dbClient.scannerAnalysisCacheDao().insert(dbSession, branch.getUuid(), IOUtils.toInputStream("test1", UTF_8));
    dbClient.scannerAnalysisCacheDao().insert(dbSession, project.getUuid(), IOUtils.toInputStream("test2", UTF_8));

    underTest.deleteBranch(dbSession, branch.getUuid());

    assertThat(dbClient.scannerAnalysisCacheDao().selectData(dbSession, branch.getUuid())).isNull();
    DbInputStream mainBranchCache = dbClient.scannerAnalysisCacheDao().selectData(dbSession, project.getUuid());

    assertThat(IOUtils.toString(mainBranchCache, UTF_8)).isEqualTo("test2");
  }

  @Test
  void deleteProject_deletes_scanner_cache() throws IOException {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch = db.components().insertProjectBranch(project);
    dbClient.scannerAnalysisCacheDao().insert(dbSession, branch.getUuid(), IOUtils.toInputStream("test1", UTF_8));
    dbClient.scannerAnalysisCacheDao().insert(dbSession, project.getUuid(), IOUtils.toInputStream("test2", UTF_8));

    underTest.deleteProject(dbSession, project.getUuid(), ComponentQualifiers.PROJECT, "project", "project");

    assertThat(dbClient.scannerAnalysisCacheDao().selectData(dbSession, project.getUuid())).isNull();
    assertThat(dbClient.scannerAnalysisCacheDao().selectData(dbSession, branch.getUuid())).isNull();
  }

  @Test
  void deleteProject_deletes_project_badge_tokens() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();
    dbClient.projectBadgeTokenDao().insert(dbSession, "token_to_delete", project, "user-uuid", "user-login");
    dbClient.projectBadgeTokenDao().insert(dbSession, "token_to_not_delete", otherProject, "user-uuid", "user-login");

    underTest.deleteProject(dbSession, project.getUuid(), project.getQualifier(), project.getName(), project.getKey());

    assertThat(dbClient.projectBadgeTokenDao().selectTokenByProject(dbSession, project)).isNull();
    assertThat(dbClient.projectBadgeTokenDao().selectTokenByProject(dbSession, otherProject)).isNotNull();
  }

  @Test
  void deleteProject_deletes_portfolio_projects() {
    ComponentDto portfolio1 = db.components().insertPrivatePortfolio();
    ComponentDto portfolio2 = db.components().insertPrivatePortfolio();

    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();

    db.components().addPortfolioProject(portfolio1, project.getUuid(), otherProject.getUuid());
    db.components().addPortfolioProject(portfolio2, project.getUuid());

    underTest.deleteProject(dbSession, project.getUuid(), project.getQualifier(), project.getName(), project.getKey());

    assertThat(dbClient.portfolioDao().selectAllPortfolioProjects(dbSession))
      .extracting(PortfolioProjectDto::getPortfolioUuid, PortfolioProjectDto::getProjectUuid)
      .containsExactlyInAnyOrder(tuple(portfolio1.uuid(), otherProject.getUuid()));
  }

  @Test
  void deleteProject_deletes_app_projects() {
    ProjectDto app = db.components().insertPrivateApplication().getProjectDto();
    BranchDto appBranch = db.components().insertProjectBranch(app);

    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto projectBranch = db.components().insertProjectBranch(project);

    ProjectDto otherProject = db.components().insertPublicProject().getProjectDto();

    db.components().addApplicationProject(app, project, otherProject);
    db.components().addProjectBranchToApplicationBranch(appBranch, projectBranch);

    assertThat(db.countRowsOfTable("app_branch_project_branch")).isOne();

    underTest.deleteProject(dbSession, project.getUuid(), project.getQualifier(), project.getName(), project.getKey());

    assertThat(dbClient.applicationProjectsDao().selectProjects(dbSession, app.getUuid()))
      .extracting(ProjectDto::getUuid)
      .containsExactlyInAnyOrder(otherProject.getUuid());
    assertThat(db.countRowsOfTable("app_branch_project_branch")).isZero();
  }

  @Test
  void deleteNonRootComponents_has_no_effect_when_parameter_is_empty() {
    DbSession dbSession = mock(DbSession.class);

    underTest.deleteNonRootComponentsInView(dbSession, Collections.emptyList());

    verifyNoInteractions(dbSession);
  }

  @Test
  void deleteNonRootComponents_has_no_effect_when_parameter_contains_only_projects_and_or_views() {
    ComponentDbTester componentDbTester = db.components();

    verifyNoEffect(componentDbTester.insertPrivateProject().getMainBranchComponent());
    verifyNoEffect(componentDbTester.insertPublicProject().getMainBranchComponent());
    verifyNoEffect(componentDbTester.insertPrivatePortfolio());
    verifyNoEffect(componentDbTester.insertPrivatePortfolio(), componentDbTester.insertPrivateProject().getMainBranchComponent(),
      componentDbTester.insertPublicProject().getMainBranchComponent());
  }

  @Test
  void delete_measures_when_deleting_project() {
    MetricDto metric = db.measures().insertMetric();

    ComponentDto project1 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto dir1 = db.components().insertComponent(newDirectory(project1, "path"));
    db.measures().insertMeasure(project1, m -> m.addValue(metric.getKey(), RandomUtils.nextInt(50)));
    db.measures().insertMeasure(dir1, m -> m.addValue(metric.getKey(), RandomUtils.nextInt(50)));

    ComponentDto project2 = db.components().insertPublicProject().getMainBranchComponent();
    ComponentDto dir2 = db.components().insertComponent(newDirectory(project2, "path"));
    db.measures().insertMeasure(project2, m -> m.addValue(metric.getKey(), RandomUtils.nextInt(50)));
    db.measures().insertMeasure(dir2, m -> m.addValue(metric.getKey(), RandomUtils.nextInt(50)));

    underTest.deleteProject(dbSession, project1.uuid(), project1.qualifier(), project1.name(), project1.getKey());

    assertThat(dbClient.measureDao().selectByComponentUuid(dbSession, project1.uuid())).isEmpty();
    assertThat(dbClient.measureDao().selectByComponentUuid(dbSession, dir1.uuid())).isEmpty();
    assertThat(dbClient.measureDao().selectByComponentUuid(dbSession, project2.uuid())).isNotEmpty();
    assertThat(dbClient.measureDao().selectByComponentUuid(dbSession, dir2.uuid())).isNotEmpty();
  }

  private void verifyNoEffect(ComponentDto firstRoot, ComponentDto... otherRoots) {
    DbSession dbSession = mock(DbSession.class);

    List<ComponentDto> componentDtos =
      Stream.concat(Stream.of(firstRoot), Arrays.stream(otherRoots)).collect(Collectors.toCollection(ArrayList::new));
    Collections.shuffle(componentDtos); // order of collection must not matter
    underTest.deleteNonRootComponentsInView(dbSession, componentDtos);

    verifyNoInteractions(dbSession);
  }

  @Test
  void deleteNonRootComponents_deletes_only_non_root_components_of_a_project_from_table_components() {
    ComponentDto project = new Random().nextBoolean() ? db.components().insertPublicProject().getMainBranchComponent()
      : db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir1 = db.components().insertComponent(newDirectory(project, "A/B"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(project, dir1));

    List<ComponentDto> components = asList(
      project,
      dir1,
      file1,
      db.components().insertComponent(newFileDto(dir1)),
      db.components().insertComponent(newFileDto(file1)),
      db.components().insertComponent(newFileDto(project)));
    Collections.shuffle(components);

    underTest.deleteNonRootComponentsInView(dbSession, components);

    assertThat(uuidsIn(" components"))
      .containsOnly(project.uuid());
  }

  @Test
  void deleteNonRootComponents_deletes_only_non_root_components_of_a_view_from_table_components() {
    ComponentDto[] projects = {
      db.components().insertPrivateProject().getMainBranchComponent(),
      db.components().insertPrivateProject().getMainBranchComponent(),
      db.components().insertPrivateProject().getMainBranchComponent()
    };

    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview1 = db.components().insertComponent(newSubPortfolio(view));
    ComponentDto subview2 = db.components().insertComponent(newSubPortfolio(subview1));
    List<ComponentDto> components = asList(
      view,
      subview1,
      subview2,
      db.components().insertComponent(newProjectCopy("a", projects[0], view)),
      db.components().insertComponent(newProjectCopy("b", projects[1], subview1)),
      db.components().insertComponent(newProjectCopy("c", projects[2], subview2)));
    Collections.shuffle(components);

    underTest.deleteNonRootComponentsInView(dbSession, components);

    assertThat(uuidsIn(" components"))
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid());
  }

  @Test
  void deleteNonRootComponents_deletes_only_specified_non_root_components_of_a_project_from_table_components() {
    ComponentDto project = new Random().nextBoolean() ? db.components().insertPublicProject().getMainBranchComponent()
      : db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto dir1 = db.components().insertComponent(newDirectory(project, "A/B"));
    ComponentDto dir2 = db.components().insertComponent(newDirectory(project, "A/C"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(project, dir1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(project));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project));

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(file3));
    assertThat(uuidsIn("components"))
      .containsOnly(project.uuid(), dir1.uuid(), dir2.uuid(), file1.uuid(), file2.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(dir2, file1));
    assertThat(uuidsIn("components"))
      .containsOnly(project.uuid(), dir1.uuid(), file2.uuid());
  }

  @Test
  void deleteNonRootComponents_deletes_only_specified_non_root_components_of_a_view_from_table_components() {
    ProjectData[] projects = {
      db.components().insertPrivateProject(),
      db.components().insertPrivateProject(),
      db.components().insertPrivateProject()
    };

    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview1 = db.components().insertComponent(newSubPortfolio(view));
    ComponentDto subview2 = db.components().insertComponent(newSubPortfolio(subview1));
    ComponentDto pc1 = db.components().insertComponent(newProjectCopy("a", projects[0].getMainBranchComponent(), view));
    ComponentDto pc2 = db.components().insertComponent(newProjectCopy("b", projects[1].getMainBranchComponent(), subview1));
    ComponentDto pc3 = db.components().insertComponent(newProjectCopy("c", projects[2].getMainBranchComponent(), subview2));

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(pc3));
    assertThat(uuidsIn("components"))
      .containsOnly(view.uuid(), projects[0].getMainBranchComponent().uuid(), projects[1].getMainBranchComponent().uuid(),
        projects[2].getMainBranchComponent().uuid(),
        subview1.uuid(), subview2.uuid(), pc1.uuid(), pc2.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview1, pc2));
    assertThat(uuidsIn("components"))
      .containsOnly(view.uuid(), projects[0].getMainBranchComponent().uuid(), projects[1].getMainBranchComponent().uuid(),
projects[2].getMainBranchComponent().uuid(),
        subview2.uuid(), pc1.uuid());
    assertThat(uuidsIn("projects")).containsOnly(projects[0].getProjectDto().getUuid(), projects[1].getProjectDto().getUuid(),
     projects[2].getProjectDto().getUuid());
  }

  @Test
  void deleteNonRootComponents_deletes_measures_of_any_non_root_component_of_a_view() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview = db.components().insertComponent(newSubPortfolio(view));
    ComponentDto pc = db.components().insertComponent(newProjectCopy("a", db.components().insertPrivateProject().getMainBranchComponent()
, view));
    insertProjectMeasureFor(view, subview, pc);
    assertThat(getComponentUuidsOfProjectMeasures()).containsOnly(view.uuid(), subview.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(pc));
    assertThat(getComponentUuidsOfProjectMeasures())
      .containsOnly(view.uuid(), subview.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview));
    assertThat(getComponentUuidsOfProjectMeasures())
      .containsOnly(view.uuid());
  }

  @Test
  void deleteNonRootComponents_deletes_properties_of_subviews_of_a_view() {
    ComponentDto view = db.components().insertPrivatePortfolio();
    ComponentDto subview1 = db.components().insertComponent(newSubPortfolio(view));
    ComponentDto subview2 = db.components().insertComponent(newSubPortfolio(subview1));
    ComponentDto subview3 = db.components().insertComponent(newSubPortfolio(view));
    ComponentDto pc = db.components().insertComponent(newProjectCopy("a", db.components().insertPrivateProject().getMainBranchComponent()
, view));
    insertPropertyFor(view, subview1, subview2, subview3, pc);

    insertReportScheduleAndSubscriptionForPortfolio("uuid", view.uuid(), dbSession);
    insertReportScheduleAndSubscriptionForPortfolio("uuid2", subview1.uuid(), dbSession);
    insertReportScheduleAndSubscriptionForPortfolio("uuid3", subview3.uuid(), dbSession);

    assertThat(getResourceIdOfProperties()).containsOnly(view.uuid(), subview1.uuid(), subview2.uuid(), subview3.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview1));
    assertThat(getResourceIdOfProperties())
      .containsOnly(view.uuid(), subview2.uuid(), subview3.uuid(), pc.uuid());
    assertThat(uuidsIn("report_schedules", "portfolio_uuid")).containsOnly(view.uuid(), subview3.uuid());
    assertThat(uuidsIn("report_subscriptions", "portfolio_uuid")).containsOnly(view.uuid(), subview3.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview2, subview3, pc));
    assertThat(getResourceIdOfProperties())
      .containsOnly(view.uuid(), pc.uuid());

    assertThat(uuidsIn("report_schedules", "portfolio_uuid")).containsOnly(view.uuid());
    assertThat(uuidsIn("report_subscriptions", "portfolio_uuid")).containsOnly(view.uuid());

  }

  @Test
  void purgeCeActivities_deletes_activity_older_than_180_days_and_their_scanner_context() {
    LocalDateTime now = LocalDateTime.now();
    insertCeActivityAndChildDataWithDate("VERY_OLD", now.minusDays(180).minusMonths(10));
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH", now.minusDays(180).minusDays(1));
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH", now.minusDays(180));
    insertCeActivityAndChildDataWithDate("RECENT", now.minusDays(1));
    when(system2.now()).thenReturn(now.toInstant(ZoneOffset.UTC).toEpochMilli());

    underTest.purgeCeActivities(db.getSession(), new PurgeProfiler());

    assertThat(selectActivity("VERY_OLD")).isEmpty();
    assertThat(selectTaskInput("VERY_OLD")).isEmpty();
    assertThat(selectTaskCharacteristic("VERY_OLD")).isEmpty();
    assertThat(scannerContextExists("VERY_OLD")).isFalse();

    assertThat(selectActivity("JUST_OLD_ENOUGH")).isEmpty();
    assertThat(selectTaskInput("JUST_OLD_ENOUGH")).isEmpty();
    assertThat(selectTaskCharacteristic("JUST_OLD_ENOUGH")).isEmpty();
    assertThat(scannerContextExists("JUST_OLD_ENOUGH")).isFalse();

    assertThat(selectActivity("NOT_OLD_ENOUGH")).isNotEmpty();
    assertThat(selectTaskInput("NOT_OLD_ENOUGH")).isNotEmpty();
    assertThat(selectTaskCharacteristic("NOT_OLD_ENOUGH")).hasSize(1);
    assertThat(scannerContextExists("NOT_OLD_ENOUGH")).isTrue();

    assertThat(selectActivity("RECENT")).isNotEmpty();
    assertThat(selectTaskInput("RECENT")).isNotEmpty();
    assertThat(selectTaskCharacteristic("RECENT")).hasSize(1);
    assertThat(scannerContextExists("RECENT")).isTrue();
  }

  @Test
  void purgeCeScannerContexts_deletes_ce_scanner_context_older_than_28_days() {
    LocalDateTime now = LocalDateTime.now();
    insertCeActivityAndChildDataWithDate("VERY_OLD", now.minusDays(28).minusMonths(12));
    insertCeActivityAndChildDataWithDate("JUST_OLD_ENOUGH", now.minusDays(28).minusDays(1));
    insertCeActivityAndChildDataWithDate("NOT_OLD_ENOUGH", now.minusDays(28));
    insertCeActivityAndChildDataWithDate("RECENT", now.minusDays(1));
    when(system2.now()).thenReturn(now.toInstant(ZoneOffset.UTC).toEpochMilli());

    underTest.purgeCeScannerContexts(db.getSession(), new PurgeProfiler());

    assertThat(scannerContextExists("VERY_OLD")).isFalse();
    assertThat(scannerContextExists("JUST_OLD_ENOUGH")).isFalse();
    assertThat(scannerContextExists("NOT_OLD_ENOUGH")).isTrue();
    assertThat(scannerContextExists("RECENT")).isTrue();
  }

  @Test
  void purge_old_anticipated_transitions() {
    ProjectData project = db.components().insertPrivateProject();

    //dates at the boundary of the deletion threshold set to 30 days
    Date okCreationDate = DateUtils.addDays(new Date(), -29);
    Date oldCreationDate = DateUtils.addDays(new Date(), -31);

    dbClient.anticipatedTransitionDao().insert(dbSession, getAnticipatedTransitionsDto("okTransition", project.projectUuid(),
      okCreationDate));
    dbClient.anticipatedTransitionDao().insert(dbSession, getAnticipatedTransitionsDto("oldTransition", project.projectUuid(),
oldCreationDate));

    underTest.purge(dbSession, newConfigurationWith30Days(System2.INSTANCE, project.getMainBranchComponent().uuid(),
     project.projectUuid()), PurgeListener.EMPTY,
      new PurgeProfiler());
    dbSession.commit();

    List<AnticipatedTransitionDto> anticipatedTransitionDtos = dbClient.anticipatedTransitionDao().selectByProjectUuid(dbSession,
     project.projectUuid());

    assertThat(anticipatedTransitionDtos).hasSize(1);
    assertThat(anticipatedTransitionDtos.get(0).getUuid()).isEqualTo("okTransition");
  }

  @Test
  void deleteBranch_shouldNotRemoveOldProjectWithSameUuidAsBranch() {
    ComponentDto componentDto = ComponentTesting.newPrivateProjectDto().setUuid("uuid");
    ProjectData projectData = db.components().insertPrivateProject("uuid", componentDto);
    BranchDto branch = db.components().insertProjectBranch(projectData.getProjectDto(), b -> b.setBranchType(BranchType.BRANCH));

    insertPropertyFor(projectData.getProjectDto());

    underTest.deleteBranch(dbSession, "uuid");

    assertThat(componentUuidsIn("properties")).containsOnly("uuid");
    assertThat(uuidsIn("project_branches")).containsOnly(branch.getUuid());
    assertThat(uuidsIn("projects")).containsOnly("uuid");

  }

  private <K, V> Map<K, V> merge(Map<? extends K, ? extends V> map1, Map<? extends K, ? extends V> map2) {
    Map<K, V> result = new HashMap<>(map1);
    result.putAll(map2);
    return result;
  }

  // the SCA mappers are in an extension, so we have to use direct sql here.
  // A cleaner approach would be to allow extensions to add purge logic on branch
  // deletion and remove SCA knowledge from the core PurgeMapper.
  private void insertScaData(String branch1Uuid, String branch2Uuid) {
    var releaseBase = Map.of("package_url", "purl1",
      "package_manager", "MAVEN",
      "package_name", "whatever",
      "version", "1.0",
      "license_expression", "MIT",
      "known", true,
      "known_package", true,
      "is_new", false,
      "created_at", 0L, "updated_at", 0L);
    db.executeInsert("sca_releases", merge(releaseBase, Map.of("uuid", "release-uuid1", "component_uuid", branch1Uuid)));
    db.executeInsert("sca_releases", merge(releaseBase, Map.of("uuid", "release-uuid2", "component_uuid", branch2Uuid)));
    assertThat(db.countRowsOfTable(dbSession, "sca_releases")).isEqualTo(2);

    var dependencyBase = Map.of("created_at", 0L, "updated_at", 0L,
      "direct", true, "scope", "compile", "is_new", true);
    db.executeInsert("sca_dependencies", merge(dependencyBase, Map.of("uuid", "dependency-uuid1", "sca_release_uuid", "release-uuid1")));
    db.executeInsert("sca_dependencies", merge(dependencyBase, Map.of("uuid", "dependency-uuid2", "sca_release_uuid", "release-uuid2")));
    assertThat(db.countRowsOfTable(dbSession, "sca_dependencies")).isEqualTo(2);

    // the issue uuids here don't even exist but doesn't matter, we don't delete issues so not testing that
    var issueReleaseBase = Map.of("created_at", 0L, "updated_at", 0L,
      "severity", "INFO", "original_severity", "INFO", "manual_severity", false,
      "severity_sort_key", 42, "status", "TO_REVIEW");
    db.executeInsert("sca_issues_releases", merge(issueReleaseBase, Map.of("uuid", "issue-release-uuid1",
      "sca_issue_uuid", "issue-uuid1", "sca_release_uuid", "release-uuid1")));
    db.executeInsert("sca_issues_releases", merge(issueReleaseBase, Map.of("uuid", "issue-release-uuid2",
      "sca_issue_uuid", "issue-uuid2", "sca_release_uuid", "release-uuid2")));

    assertThat(db.countRowsOfTable(dbSession, "sca_issues_releases")).isEqualTo(2);

    var issueReleaseChangeBase = Map.of("created_at", 0L, "updated_at", 0L);
    db.executeInsert("sca_issue_rels_changes", merge(issueReleaseChangeBase, Map.of("uuid", "issue-release-change-uuid1",
      "sca_issues_releases_uuid", "issue-release-uuid1")));
    db.executeInsert("sca_issue_rels_changes", merge(issueReleaseChangeBase, Map.of("uuid", "issue-release-change-uuid2",
      "sca_issues_releases_uuid", "issue-release-uuid2")));

    assertThat(db.countRowsOfTable(dbSession, "sca_issue_rels_changes")).isEqualTo(2);

    var analysisBase = Map.of(
      "created_at", 0L,
      "updated_at", 0L,
      "status", "COMPLETED",
      "errors", "[]",
      "parsed_files", "[]",
      "failed_reason", "something");
    db.executeInsert("sca_analyses", merge(analysisBase, Map.of(
      "uuid", "analysis-uuid1",
      "component_uuid", branch1Uuid)));
    db.executeInsert("sca_analyses", merge(analysisBase, Map.of(
      "uuid", "analysis-uuid2",
      "component_uuid", branch2Uuid)));
    assertThat(db.countRowsOfTable(dbSession, "sca_analyses")).isEqualTo(2);
  }

  @Test
  void deleteBranch_purgesScaActivity() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(project);
    BranchDto branch2 = db.components().insertProjectBranch(project);

    insertScaData(branch1.getUuid(), branch2.getUuid());

    underTest.deleteBranch(dbSession, branch1.getUuid());

    assertThat(db.countRowsOfTable(dbSession, "sca_releases")).isEqualTo(1);
    assertThat(db.countRowsOfTable(dbSession, "sca_dependencies")).isEqualTo(1);
    assertThat(db.countRowsOfTable(dbSession, "sca_issues_releases")).isEqualTo(1);
    assertThat(db.countRowsOfTable(dbSession, "sca_issue_rels_changes")).isEqualTo(1);
    assertThat(db.countRowsOfTable(dbSession, "sca_analyses")).isEqualTo(1);
  }

  @Test
  void deleteProject_purgesScaLicenseProfiles() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();

    var scaLicenseProfileProjectBase = Map.of(
      "sca_license_profile_uuid", "sca-license-profile-uuid1",
      "created_at", 0L,
      "updated_at", 0L);

    db.executeInsert("sca_lic_prof_projects", merge(scaLicenseProfileProjectBase, Map.of(
      "uuid", "sca-lic-prof-project-uuid1",
      "project_uuid", project.getUuid())));

    db.executeInsert("sca_lic_prof_projects", merge(scaLicenseProfileProjectBase, Map.of(
      "uuid", "sca-lic-prof-project-uuid2",
      "project_uuid", "other-project-uuid")));

    assertThat(db.countRowsOfTable(dbSession, "sca_lic_prof_projects")).isEqualTo(2);

    underTest.deleteProject(dbSession, project.getUuid(), project.getQualifier(), project.getName(), project.getKey());

    assertThat(db.countRowsOfTable(dbSession, "sca_lic_prof_projects")).isEqualTo(1);
  }

  @Test
  void whenDeleteBranch_thenPurgeArchitectureGraphs() {
    ProjectDto project = db.components().insertPublicProject().getProjectDto();
    BranchDto branch1 = db.components().insertProjectBranch(project);
    BranchDto branch2 = db.components().insertProjectBranch(project);

    db.executeInsert("architecture_graphs", Map.of("uuid", "12345", "branch_uuid", branch1.getUuid(), "ecosystem", "xoo", "type", "file_graph", "graph_data", "{}"));
    db.executeInsert("architecture_graphs", Map.of("uuid", "123456", "branch_uuid", branch1.getUuid(), "ecosystem", "xoo", "type", "class_graph", "graph_data", "{}"));
    db.executeInsert("architecture_graphs", Map.of("uuid", "1234567", "branch_uuid", branch2.getUuid(), "ecosystem", "xoo", "type", "file_graph", "graph_data", "{}"));

    assertThat(db.countRowsOfTable(dbSession, "architecture_graphs")).isEqualTo(3);
    underTest.deleteBranch(dbSession, branch1.getUuid());
    assertThat(db.countRowsOfTable(dbSession, "architecture_graphs")).isEqualTo(1);
    underTest.deleteBranch(dbSession, branch2.getUuid());
    assertThat(db.countRowsOfTable(dbSession, "architecture_graphs")).isZero();
  }

  private AnticipatedTransitionDto getAnticipatedTransitionsDto(String uuid, String projectUuid, Date creationDate) {
    return new AnticipatedTransitionDto(uuid, projectUuid, "userUuid", "transition", null, null, null, null, "rule:key", "filepath",
      creationDate.getTime());
  }

  private Optional<CeActivityDto> selectActivity(String taskUuid) {
    return db.getDbClient().ceActivityDao().selectByUuid(db.getSession(), taskUuid);
  }

  private List<CeTaskCharacteristicDto> selectTaskCharacteristic(String taskUuid) {
    return db.getDbClient().ceTaskCharacteristicsDao().selectByTaskUuids(db.getSession(), Collections.singletonList(taskUuid));
  }

  private Optional<DbInputStream> selectTaskInput(String taskUuid) {
    return db.getDbClient().ceTaskInputDao().selectData(db.getSession(), taskUuid);
  }

  private boolean scannerContextExists(String uuid) {
    return db.countSql("select count(1) from ce_scanner_context where task_uuid = '" + uuid + "'") == 1;
  }

  @SafeVarargs
  private final void insertCeActivityAndChildDataWithDate(String ceActivityUuid, LocalDateTime dateTime,
    Consumer<CeQueueDto>... queueDtoConsumers) {
    long date = dateTime.toInstant(UTC).toEpochMilli();
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(ceActivityUuid);
    queueDto.setTaskType(CeTaskTypes.REPORT);
    Arrays.stream(queueDtoConsumers).forEach(t -> t.accept(queueDto));
    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);

    when(system2.now()).thenReturn(date);
    insertCeTaskInput(dto.getUuid());
    insertCeTaskCharacteristics(dto.getUuid(), 1);
    insertCeScannerContext(dto.getUuid());
    insertCeTaskMessages(dto.getUuid(), 2);
    db.getDbClient().ceActivityDao().insert(db.getSession(), dto);
    db.getSession().commit();
  }

  private Stream<String> getResourceIdOfProperties() {
    return db.select("select entity_uuid as \"uuid\" from properties").stream()
      .map(row -> (String) row.get("uuid"));
  }

  private void insertPropertyFor(ComponentDto... components) {
    Stream.of(components).forEach(componentDto -> db.properties().insertProperty(new PropertyDto()
        .setKey(secure().nextAlphabetic(3))
        .setValue(secure().nextAlphabetic(3))
        .setEntityUuid(componentDto.uuid()),
      componentDto.getKey(), componentDto.name(), componentDto.qualifier(), null));
  }

  private void insertPropertyFor(ProjectDto project) {
    db.properties().insertProperty(new PropertyDto()
        .setKey(secure().nextAlphabetic(3))
        .setValue(secure().nextAlphabetic(3))
        .setEntityUuid(project.getUuid()),
      null, project.getKey(), null, null);
  }

  private Stream<String> getComponentUuidsOfProjectMeasures() {
    return db.select("select component_uuid as \"COMPONENT_UUID\" from project_measures").stream()
      .map(row -> (String) row.get("COMPONENT_UUID"));
  }

  private void insertProjectMeasureFor(ComponentDto... components) {
    Arrays.stream(components).forEach(componentDto -> db.getDbClient().projectMeasureDao().insert(dbSession, new ProjectMeasureDto()
      .setMetricUuid(secure().nextAlphabetic(3))
      .setComponentUuid(componentDto.uuid())
      .setAnalysisUuid(secure().nextAlphabetic(3))));
    dbSession.commit();
  }

  private CeQueueDto createCeQueue(ComponentDto component, String entityUuid, Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(Uuids.create());
    queueDto.setTaskType(REPORT);
    queueDto.setComponentUuid(component.uuid());
    queueDto.setEntityUuid(entityUuid);
    queueDto.setSubmitterUuid("submitter uuid");
    queueDto.setCreatedAt(1_300_000_000_000L);
    queueDto.setStatus(status);
    return queueDto;
  }

  private CeActivityDto insertCeActivity(ComponentDto component, String entityUuid) {
    Status unusedStatus = Status.values()[random.nextInt(Status.values().length)];
    CeQueueDto queueDto = createCeQueue(component, entityUuid, unusedStatus);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    dto.setStartedAt(1_500_000_000_000L);
    dto.setExecutedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
    dbClient.ceActivityDao().insert(dbSession, dto);
    return dto;
  }

  private CeQueueDto insertCeQueue(ComponentDto component, String mainBranch) {
    CeQueueDto res = new CeQueueDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setTaskType("foo")
      .setComponentUuid(component.uuid())
      .setEntityUuid(mainBranch)
      .setStatus(Status.PENDING)
      .setCreatedAt(1_2323_222L)
      .setUpdatedAt(1_2323_222L);
    dbClient.ceQueueDao().insert(dbSession, res);
    dbSession.commit();
    return res;
  }

  private CeQueueDto insertCeQueue(ComponentDto component) {
    return insertCeQueue(component, component.uuid());
  }

  private void insertCeScannerContext(String uuid) {
    dbClient.ceScannerContextDao().insert(dbSession, uuid, CloseableIterator.from(Arrays.asList("a", "b", "c").iterator()));
    dbSession.commit();
  }

  private void insertCeTaskCharacteristics(String uuid, int count) {
    List<CeTaskCharacteristicDto> dtos = IntStream.range(0, count)
      .mapToObj(i -> new CeTaskCharacteristicDto()
        .setUuid(UuidFactoryFast.getInstance().create())
        .setTaskUuid(uuid)
        .setKey("key_" + uuid.hashCode() + i)
        .setValue("value_" + uuid.hashCode() + i))
      .toList();
    dbClient.ceTaskCharacteristicsDao().insert(dbSession, dtos);
    dbSession.commit();
  }

  private void insertCeTaskInput(String uuid) {
    dbClient.ceTaskInputDao().insert(dbSession, uuid, new ByteArrayInputStream("some content man!".getBytes()));
    dbSession.commit();
  }

  private void insertCeTaskMessages(String uuid, int count) {
    IntStream.range(0, count)
      .mapToObj(i -> new CeTaskMessageDto()
        .setUuid(UuidFactoryFast.getInstance().create())
        .setTaskUuid(uuid)
        .setMessage("key_" + uuid.hashCode() + i)
        .setType(MessageType.GENERIC)
        .setCreatedAt(2_333_444L + i))
      .forEach(dto -> dbClient.ceTaskMessageDao().insert(dbSession, dto));
    dbSession.commit();
  }

  private static PurgeableAnalysisDto getById(List<PurgeableAnalysisDto> snapshots, String uuid) {
    return snapshots.stream()
      .filter(snapshot -> uuid.equals(snapshot.getAnalysisUuid()))
      .findFirst()
      .orElse(null);
  }

  private Stream<String> uuidsIn(String tableName) {
    return uuidsIn(tableName, "uuid");
  }

  private Stream<String> componentUuidsIn(String tableName) {
    return uuidsIn(tableName, "entity_uuid");
  }

  private Stream<String> taskUuidsIn(String tableName) {
    return uuidsIn(tableName, "task_uuid");
  }

  private Stream<String> uuidsIn(String tableName, String columnName) {
    return db.select("select " + columnName + " as \"UUID\" from " + tableName)
      .stream()
      .map(row -> (String) row.get("UUID"));
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2, String rootUuid, String projectUuid) {
    return newConfigurationWith30Days(system2, rootUuid, projectUuid, Collections.emptySet());
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2, String rootUuid, String projectUuid,
Set<String> disabledComponentUuids) {
    return new PurgeConfiguration(rootUuid, projectUuid, 30, Optional.of(30), system2, disabledComponentUuids, 30);
  }

  private Stream<String> uuidsOfAnalysesOfRoot(ComponentDto rootComponent) {
    return db.select("select uuid as \"UUID\" from snapshots where root_component_uuid='" + rootComponent.uuid() + "'")
      .stream()
      .map(t -> (String) t.get("UUID"));
  }

  private void addComponentsSnapshotsAndIssuesToBranch(ComponentDto branch, RuleDto rule, int branchAge) {
    db.components().insertSnapshot(branch, dto -> dto.setCreatedAt(DateUtils.addDays(new Date(), -branchAge).getTime()));
    ComponentDto dir = db.components().insertComponent(newDirectory(branch, "path"));
    ComponentDto file = db.components().insertComponent(newFileDto(branch, dir));
    db.issues().insert(rule, branch, file);
  }

  private void addComponentsSnapshotsAndIssuesToBranch(BranchDto branch, RuleDto rule, int branchAge) {
    ComponentDto componentDto = db.components().getComponentDto(branch);
    addComponentsSnapshotsAndIssuesToBranch(componentDto, rule, branchAge);
  }

  private int countPurgedSnapshots() {
    Dialect dialect = db.getDbClient().getDatabase().getDialect();
    String bool = dialect.getTrueSqlValue();
    return db.countSql("select count(*) from snapshots where purged = " + bool);
  }

}
