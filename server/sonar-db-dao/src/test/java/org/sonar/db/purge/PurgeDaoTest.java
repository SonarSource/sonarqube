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
package org.sonar.db.purge;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang.time.DateUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.resources.Scopes;
import org.sonar.api.utils.System2;
import org.sonar.core.util.CloseableIterator;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.core.util.Uuids;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.alm.ALM;
import org.sonar.db.ce.CeActivityDto;
import org.sonar.db.ce.CeQueueDto;
import org.sonar.db.ce.CeQueueDto.Status;
import org.sonar.db.ce.CeTaskCharacteristicDto;
import org.sonar.db.ce.CeTaskMessageDto;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.BranchType;
import org.sonar.db.component.ComponentDbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ComponentTesting;
import org.sonar.db.component.SnapshotDto;
import org.sonar.db.event.EventComponentChangeDto;
import org.sonar.db.event.EventDto;
import org.sonar.db.event.EventTesting;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.measure.LiveMeasureDto;
import org.sonar.db.measure.MeasureDto;
import org.sonar.db.measure.custom.CustomMeasureDto;
import org.sonar.db.metric.MetricDto;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.property.PropertyDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.source.FileSourceDto;
import org.sonar.db.webhook.WebhookDeliveryLiteDto;
import org.sonar.db.webhook.WebhookDto;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.db.ce.CeTaskTypes.REPORT;
import static org.sonar.db.component.ComponentTesting.newBranchDto;
import static org.sonar.db.component.ComponentTesting.newDirectory;
import static org.sonar.db.component.ComponentTesting.newFileDto;
import static org.sonar.db.component.ComponentTesting.newModuleDto;
import static org.sonar.db.component.ComponentTesting.newProjectCopy;
import static org.sonar.db.component.SnapshotDto.STATUS_PROCESSED;
import static org.sonar.db.component.SnapshotDto.STATUS_UNPROCESSED;
import static org.sonar.db.component.SnapshotTesting.newSnapshot;
import static org.sonar.db.event.EventDto.CATEGORY_VERSION;
import static org.sonar.db.webhook.WebhookDeliveryTesting.newDto;
import static org.sonar.db.webhook.WebhookDeliveryTesting.selectAllDeliveryUuids;

public class PurgeDaoTest {

  private static final String PROJECT_UUID = "P1";

  private System2 system2 = mock(System2.class);

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DbClient dbClient = db.getDbClient();
  private DbSession dbSession = db.getSession();
  private PurgeDao underTest = db.getDbClient().purgeDao();

  @Test
  public void purge_failed_ce_tasks() {
    db.prepareDbUnit(getClass(), "shouldDeleteAbortedBuilds.xml");

    underTest.purge(dbSession, newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldDeleteAbortedBuilds-result.xml", "snapshots");
  }

  @Test
  public void purge_history_of_project() {
    db.prepareDbUnit(getClass(), "shouldPurgeProject.xml");
    underTest.purge(dbSession, newConfigurationWith30Days(), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();
    db.assertDbUnit(getClass(), "shouldPurgeProject-result.xml", "projects", "snapshots");
  }

  @Test
  public void purge_inactive_short_living_branches() {
    when(system2.now()).thenReturn(new Date().getTime());
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto longBranch = db.components().insertProjectBranch(project);
    ComponentDto recentShortBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.SHORT));

    // short branch with other components and issues, updated 31 days ago
    when(system2.now()).thenReturn(DateUtils.addDays(new Date(), -31).getTime());
    ComponentDto shortBranch = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.SHORT));
    ComponentDto module = db.components().insertComponent(newModuleDto(shortBranch));
    ComponentDto subModule = db.components().insertComponent(newModuleDto(module));
    ComponentDto file = db.components().insertComponent(newFileDto(subModule));
    db.issues().insert(rule, shortBranch, file);
    db.issues().insert(rule, shortBranch, subModule);
    db.issues().insert(rule, shortBranch, module);

    // back to present
    when(system2.now()).thenReturn(new Date().getTime());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, project.uuid(), project.uuid()), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    assertThat(uuidsIn("projects")).containsOnly(project.uuid(), longBranch.uuid(), recentShortBranch.uuid());
  }

  @Test
  public void purge_inactive_pull_request() {
    when(system2.now()).thenReturn(new Date().getTime());
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto longBranch = db.components().insertProjectBranch(project);
    ComponentDto recentPullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));

    // pull request with other components and issues, updated 31 days ago
    when(system2.now()).thenReturn(DateUtils.addDays(new Date(), -31).getTime());
    ComponentDto pullRequest = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto module = db.components().insertComponent(newModuleDto(pullRequest));
    ComponentDto subModule = db.components().insertComponent(newModuleDto(module));
    ComponentDto file = db.components().insertComponent(newFileDto(subModule));
    db.issues().insert(rule, pullRequest, file);
    db.issues().insert(rule, pullRequest, subModule);
    db.issues().insert(rule, pullRequest, module);

    // back to present
    when(system2.now()).thenReturn(new Date().getTime());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, project.uuid(), project.uuid()), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    assertThat(uuidsIn("projects")).containsOnly(project.uuid(), longBranch.uuid(), recentPullRequest.uuid());
  }

  @Test
  public void purge_inactive_SLB_when_analyzing_non_main_branch() {
    when(system2.now()).thenReturn(new Date().getTime());
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto longBranch = db.components().insertProjectBranch(project);

    when(system2.now()).thenReturn(DateUtils.addDays(new Date(), -31).getTime());

    // SLB updated 31 days ago
    ComponentDto slb1 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.SHORT));

    // SLB with other components and issues, updated 31 days ago
    ComponentDto slb2 = db.components().insertProjectBranch(project, b -> b.setBranchType(BranchType.PULL_REQUEST));
    ComponentDto file = db.components().insertComponent(newFileDto(slb2));
    db.issues().insert(rule, slb2, file);

    // back to present
    when(system2.now()).thenReturn(new Date().getTime());
    // analysing slb1
    underTest.purge(dbSession, newConfigurationWith30Days(system2, slb1.uuid(), slb1.getMainBranchProjectUuid()), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    // slb1 wasn't deleted since it was being analyzed!
    assertThat(uuidsIn("projects")).containsOnly(project.uuid(), longBranch.uuid(), slb1.uuid());
  }

  @Test
  public void shouldDeleteHistoricalDataOfDirectoriesAndFiles() {
    db.prepareDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles.xml");
    PurgeConfiguration conf = new PurgeConfiguration("PROJECT_UUID", "PROJECT_UUID", asList(Scopes.DIRECTORY, Scopes.FILE),
      30, Optional.of(30), System2.INSTANCE, Collections.emptyList());

    underTest.purge(dbSession, conf, PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    db.assertDbUnit(getClass(), "shouldDeleteHistoricalDataOfDirectoriesAndFiles-result.xml", "projects", "snapshots", "project_measures");
  }

  @Test
  public void close_issues_clean_index_and_file_sources_of_disabled_components_specified_by_uuid_in_configuration() {
    // components and issues, updated 31 days ago
    when(system2.now()).thenReturn(DateUtils.addDays(new Date(), -31).getTime());
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch(p -> p.setEnabled(false));
    db.components().insertSnapshot(project);
    db.components().insertSnapshot(project);
    db.components().insertSnapshot(project, s -> s.setLast(false));

    ComponentDto module = db.components().insertComponent(newModuleDto(project).setEnabled(false));
    ComponentDto dir = db.components().insertComponent(newDirectory(module, "sub").setEnabled(false));
    ComponentDto srcFile = db.components().insertComponent(newFileDto(module, dir).setEnabled(false));
    ComponentDto testFile = db.components().insertComponent(newFileDto(module, dir).setEnabled(false));
    ComponentDto nonSelectedFile = db.components().insertComponent(newFileDto(module, dir).setEnabled(false));
    IssueDto openOnFile = db.issues().insert(rule, project, srcFile, issue -> issue.setStatus("OPEN"));
    IssueDto confirmOnFile = db.issues().insert(rule, project, srcFile, issue -> issue.setStatus("CONFIRM"));
    IssueDto openOnDir = db.issues().insert(rule, project, dir, issue -> issue.setStatus("OPEN"));
    IssueDto confirmOnDir = db.issues().insert(rule, project, dir, issue -> issue.setStatus("CONFIRM"));
    IssueDto openOnNonSelected = db.issues().insert(rule, project, nonSelectedFile, issue -> issue.setStatus("OPEN"));
    IssueDto confirmOnNonSelected = db.issues().insert(rule, project, nonSelectedFile, issue -> issue.setStatus("CONFIRM"));

    assertThat(db.countSql("select count(*) from snapshots where purge_status = 1")).isEqualTo(0);

    assertThat(db.countSql("select count(*) from issues where status = 'CLOSED'")).isEqualTo(0);
    assertThat(db.countSql("select count(*) from issues where resolution = 'REMOVED'")).isEqualTo(0);

    db.fileSources().insertFileSource(srcFile);
    FileSourceDto nonSelectedFileSource = db.fileSources().insertFileSource(nonSelectedFile);
    assertThat(db.countRowsOfTable("file_sources")).isEqualTo(2);

    MetricDto metric1 = db.measures().insertMetric();
    MetricDto metric2 = db.measures().insertMetric();
    LiveMeasureDto liveMeasureMetric1OnFile = db.measures().insertLiveMeasure(srcFile, metric1);
    LiveMeasureDto liveMeasureMetric2OnFile = db.measures().insertLiveMeasure(srcFile, metric2);
    LiveMeasureDto liveMeasureMetric1OnDir = db.measures().insertLiveMeasure(dir, metric1);
    LiveMeasureDto liveMeasureMetric2OnDir = db.measures().insertLiveMeasure(dir, metric2);
    LiveMeasureDto liveMeasureMetric1OnProject = db.measures().insertLiveMeasure(project, metric1);
    LiveMeasureDto liveMeasureMetric2OnProject = db.measures().insertLiveMeasure(project, metric2);
    LiveMeasureDto liveMeasureMetric1OnNonSelected = db.measures().insertLiveMeasure(nonSelectedFile, metric1);
    LiveMeasureDto liveMeasureMetric2OnNonSelected = db.measures().insertLiveMeasure(nonSelectedFile, metric2);
    assertThat(db.countRowsOfTable("live_measures")).isEqualTo(8);

    // back to present
    when(system2.now()).thenReturn(new Date().getTime());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, project.uuid(), project.uuid(), module.uuid(), dir.uuid(), srcFile.uuid(), testFile.uuid()), PurgeListener.EMPTY,
      new PurgeProfiler());
    dbSession.commit();

    // set purge_status=1 for non-last snapshot
    assertThat(db.countSql("select count(*) from snapshots where purge_status = 1")).isEqualTo(1);

    // close open issues of selected
    assertThat(db.countSql("select count(*) from issues where status = 'CLOSED'")).isEqualTo(4);
    for (IssueDto issue : Arrays.asList(openOnFile, confirmOnFile, openOnDir, confirmOnDir)) {
      assertThat(db.getDbClient().issueDao().selectByKey(dbSession, issue.getKey()).get())
        .extracting("status", "resolution")
        .containsExactlyInAnyOrder("CLOSED", "REMOVED");
    }
    for (IssueDto issue : Arrays.asList(openOnNonSelected, confirmOnNonSelected)) {
      assertThat(db.getDbClient().issueDao().selectByKey(dbSession, issue.getKey()).get())
        .extracting("status", "resolution")
        .containsExactlyInAnyOrder(issue.getStatus(), null);
    }

    // delete file sources of selected
    assertThat(db.countRowsOfTable("file_sources")).isEqualTo(1);
    assertThat(db.getDbClient().fileSourceDao().selectByFileUuid(dbSession, nonSelectedFileSource.getFileUuid())).isNotNull();

    // deletes live measure of selected
    assertThat(db.countRowsOfTable("live_measures")).isEqualTo(4);
    List<LiveMeasureDto> liveMeasureDtos = db.getDbClient().liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession,
      ImmutableSet.of(srcFile.uuid(), dir.uuid(), project.uuid(), nonSelectedFile.uuid()), ImmutableSet.of(metric1.getId(), metric2.getId()));
    assertThat(liveMeasureDtos)
      .extracting(LiveMeasureDto::getComponentUuid)
      .containsOnly(nonSelectedFile.uuid(), project.uuid());
    assertThat(liveMeasureDtos)
      .extracting(LiveMeasureDto::getMetricId)
      .containsOnly(metric1.getId(), metric2.getId());
  }

  @Test
  public void shouldDeleteAnalyses() {
    db.prepareDbUnit(getClass(), "shouldDeleteAnalyses.xml");

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(3, "u3")));
    db.assertDbUnit(getClass(), "shouldDeleteAnalyses-result.xml", "snapshots");
  }

  @Test
  public void deleteAnalyses_deletes_rows_in_events_and_event_component_changes() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project);
    SnapshotDto projectAnalysis1 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis2 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis3 = db.components().insertSnapshot(project);
    SnapshotDto projectAnalysis4 = db.components().insertSnapshot(project);
    EventDto projectEvent1 = db.events().insertEvent(projectAnalysis1);
    EventDto projectEvent2 = db.events().insertEvent(projectAnalysis2);
    EventDto projectEvent3 = db.events().insertEvent(projectAnalysis3);
    // note: projectAnalysis4 has no event
    ComponentDto referencedProjectA = db.components().insertPublicProject();
    ComponentDto referencedProjectB = db.components().insertPublicProject();
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectA, null);
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectB, null);
    BranchDto branchProjectA = newBranchDto(referencedProjectA);
    ComponentDto cptBranchProjectA = ComponentTesting.newProjectBranch(referencedProjectA, branchProjectA);
    db.events().insertEventComponentChanges(projectEvent2, projectAnalysis2, randomChangeCategory(), cptBranchProjectA, branchProjectA);
    // note: projectEvent3 has no component change

    // delete non existing analysis has no effect
    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(3, "foo")));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis1.getUuid(), projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(3);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent1.getUuid(), projectEvent2.getUuid(), projectEvent3.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(projectAnalysis1.getId(), projectAnalysis1.getUuid())));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(1);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent2.getUuid(), projectEvent3.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(projectAnalysis4.getId(), projectAnalysis4.getUuid())));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(1);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent2.getUuid(), projectEvent3.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(projectAnalysis3.getId(), projectAnalysis3.getUuid())));
    assertThat(uuidsIn("event_component_changes", "event_analysis_uuid"))
      .containsOnly(projectAnalysis2.getUuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(1);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent2.getUuid());

    underTest.deleteAnalyses(dbSession, new PurgeProfiler(), ImmutableList.of(new IdUuidPair(projectAnalysis2.getId(), projectAnalysis2.getUuid())));
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isZero();
    assertThat(db.countRowsOfTable("events"))
      .isZero();
  }

  @Test
  public void selectPurgeableAnalyses() {
    SnapshotDto[] analyses = new SnapshotDto[] {
      newSnapshot()
        .setUuid("u1")
        .setComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(true),
      // not processed -> exclude
      newSnapshot()
        .setUuid("u2")
        .setComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_UNPROCESSED)
        .setLast(false),
      // on other resource -> exclude
      newSnapshot()
        .setUuid("u3")
        .setComponentUuid("uuid_222")
        .setStatus(STATUS_PROCESSED)
        .setLast(true),
      // without event -> select
      newSnapshot()
        .setUuid("u4")
        .setComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(false),
      // with event -> select
      newSnapshot()
        .setUuid("u5")
        .setComponentUuid(PROJECT_UUID)
        .setStatus(STATUS_PROCESSED)
        .setLast(false)
        .setProjectVersion("V5")
    };
    db.components().insertSnapshots(analyses);
    db.events().insertEvent(EventTesting.newEvent(analyses[4])
      .setName("V5")
      .setCategory(CATEGORY_VERSION));

    List<PurgeableAnalysisDto> purgeableAnalyses = underTest.selectPurgeableAnalyses(PROJECT_UUID, dbSession);

    assertThat(purgeableAnalyses).hasSize(3);
    assertThat(getById(purgeableAnalyses, "u1").isLast()).isTrue();
    assertThat(getById(purgeableAnalyses, "u1").hasEvents()).isFalse();
    assertThat(getById(purgeableAnalyses, "u1").getVersion()).isNull();
    assertThat(getById(purgeableAnalyses, "u4").isLast()).isFalse();
    assertThat(getById(purgeableAnalyses, "u4").hasEvents()).isFalse();
    assertThat(getById(purgeableAnalyses, "u4").getVersion()).isNull();
    assertThat(getById(purgeableAnalyses, "u5").isLast()).isFalse();
    assertThat(getById(purgeableAnalyses, "u5").hasEvents()).isTrue();
    assertThat(getById(purgeableAnalyses, "u5").getVersion()).isEqualTo("V5");
  }

  @Test
  public void selectPurgeableAnalyses_does_not_return_the_baseline() {
    ComponentDto project1 = db.components().insertMainBranch(db.getDefaultOrganization(), "master");
    SnapshotDto analysis1 = db.components().insertSnapshot(newSnapshot()
      .setComponentUuid(project1.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    dbClient.branchDao().updateManualBaseline(dbSession, project1.uuid(), analysis1.getUuid());
    ComponentDto project2 = db.components().insertPrivateProject();
    SnapshotDto analysis2 = db.components().insertSnapshot(newSnapshot()
      .setComponentUuid(project2.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    db.components().insertProjectBranch(project2);

    assertThat(underTest.selectPurgeableAnalyses(project1.uuid(), dbSession)).isEmpty();
    assertThat(underTest.selectPurgeableAnalyses(project2.uuid(), dbSession))
      .extracting(PurgeableAnalysisDto::getAnalysisUuid)
      .containsOnly(analysis2.getUuid());
  }

  @Test
  public void selectPurgeableAnalyses_does_not_return_the_baseline_of_specific_branch() {
    ComponentDto project = db.components().insertMainBranch(db.getDefaultOrganization(), "master");
    SnapshotDto analysisProject = db.components().insertSnapshot(newSnapshot()
      .setComponentUuid(project.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    dbClient.branchDao().updateManualBaseline(dbSession, project.uuid(), analysisProject.getUuid());
    ComponentDto branch1 = db.components().insertProjectBranch(project);
    SnapshotDto analysisBranch1 = db.components().insertSnapshot(newSnapshot()
      .setComponentUuid(branch1.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    ComponentDto branch2 = db.components().insertProjectBranch(project);
    SnapshotDto analysisBranch2 = db.components().insertSnapshot(newSnapshot()
      .setComponentUuid(branch2.uuid())
      .setStatus(STATUS_PROCESSED)
      .setLast(false));
    dbClient.branchDao().updateManualBaseline(dbSession, branch2.uuid(), analysisBranch2.getUuid());
    dbSession.commit();

    assertThat(underTest.selectPurgeableAnalyses(project.uuid(), dbSession))
      .isEmpty();
    assertThat(underTest.selectPurgeableAnalyses(branch1.uuid(), dbSession))
      .extracting(PurgeableAnalysisDto::getAnalysisUuid)
      .containsOnly(analysisBranch1.getUuid());
    assertThat(underTest.selectPurgeableAnalyses(branch2.uuid(), dbSession))
      .isEmpty();
  }

  @Test
  public void delete_project_and_associated_data() {
    db.prepareDbUnit(getClass(), "shouldDeleteProject.xml");

    underTest.deleteProject(dbSession, "A");
    dbSession.commit();

    assertThat(db.countRowsOfTable("projects")).isZero();
    assertThat(db.countRowsOfTable("snapshots")).isZero();
    assertThat(db.countRowsOfTable("issues")).isZero();
    assertThat(db.countRowsOfTable("issue_changes")).isZero();
    assertThat(db.countRowsOfTable("file_sources")).isZero();
  }

  @Test
  public void delete_webhooks_from_project() {
    OrganizationDto organization = db.organizations().insert();
    ComponentDto project1 = db.components().insertPrivateProject(organization);
    WebhookDto webhook = db.webhooks().insertWebhook(project1);
    db.webhookDelivery().insert(webhook);
    ComponentDto projectNotToBeDeleted = db.components().insertPrivateProject(organization);
    WebhookDto webhookNotDeleted = db.webhooks().insertWebhook(projectNotToBeDeleted);
    WebhookDeliveryLiteDto webhookDeliveryNotDeleted = db.webhookDelivery().insert(webhookNotDeleted);

    underTest.deleteProject(dbSession, project1.uuid());

    assertThat(db.select(db.getSession(), "select uuid as \"uuid\" from webhooks"))
      .extracting(m -> m.get("uuid"))
      .containsExactlyInAnyOrder(webhookNotDeleted.getUuid());
    assertThat(db.select(db.getSession(), "select uuid as \"uuid\" from webhook_deliveries"))
      .extracting(m -> m.get("uuid"))
      .containsExactlyInAnyOrder(webhookDeliveryNotDeleted.getUuid());
  }

  private Stream<String> uuidsOfTable(String tableName) {
    return db.select("select uuid as \"UUID\" from " + tableName)
      .stream()
      .map(s -> (String) s.get("UUID"));
  }

  @Test
  public void delete_row_in_ce_activity_when_deleting_project() {
    ComponentDto projectToBeDeleted = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto anotherLivingProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, projectToBeDeleted, anotherLivingProject);

    // Insert 2 rows in CE_ACTIVITY : one for the project that will be deleted, and one on another project
    CeActivityDto toBeDeletedActivity = insertCeActivity(projectToBeDeleted);
    CeActivityDto notDeletedActivity = insertCeActivity(anotherLivingProject);
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(uuidsOfTable("ce_activity"))
      .containsOnly(notDeletedActivity.getUuid())
      .hasSize(1);
  }

  @Test
  public void delete_row_in_ce_task_input_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeActivityDto projectTask = insertCeActivity(project);
    insertCeTaskInput(projectTask.getUuid());
    CeActivityDto branchTask = insertCeActivity(branch);
    insertCeTaskInput(branchTask.getUuid());
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch);
    insertCeTaskInput(anotherBranchTask.getUuid());
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject);
    insertCeTaskInput(anotherProjectTask.getUuid());
    insertCeTaskInput("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_scanner_context_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeActivityDto projectTask = insertCeActivity(project);
    insertCeScannerContext(projectTask.getUuid());
    CeActivityDto branchTask = insertCeActivity(branch);
    insertCeScannerContext(branchTask.getUuid());
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch);
    insertCeScannerContext(anotherBranchTask.getUuid());
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject);
    insertCeScannerContext(anotherProjectTask.getUuid());
    insertCeScannerContext("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_task_characteristics_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeActivityDto projectTask = insertCeActivity(project);
    insertCeTaskCharacteristics(projectTask.getUuid(), 3);
    CeActivityDto branchTask = insertCeActivity(branch);
    insertCeTaskCharacteristics(branchTask.getUuid(), 2);
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch);
    insertCeTaskCharacteristics(anotherBranchTask.getUuid(), 6);
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject);
    insertCeTaskCharacteristics(anotherProjectTask.getUuid(), 2);
    insertCeTaskCharacteristics("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_task_message_referring_to_a_row_in_ce_activity_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeActivityDto projectTask = insertCeActivity(project);
    insertCeTaskMessages(projectTask.getUuid(), 3);
    CeActivityDto branchTask = insertCeActivity(branch);
    insertCeTaskMessages(branchTask.getUuid(), 2);
    CeActivityDto anotherBranchTask = insertCeActivity(anotherBranch);
    insertCeTaskMessages(anotherBranchTask.getUuid(), 6);
    CeActivityDto anotherProjectTask = insertCeActivity(anotherProject);
    insertCeTaskMessages(anotherProjectTask.getUuid(), 2);
    insertCeTaskMessages("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_activity")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_tasks_in_ce_queue_when_deleting_project() {
    ComponentDto projectToBeDeleted = db.components().insertPrivateProject();
    ComponentDto anotherLivingProject = db.components().insertPrivateProject();

    // Insert 3 rows in CE_QUEUE: two for the project that will be deleted (in order to check that status
    // is not involved in deletion), and one on another project
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(projectToBeDeleted, Status.PENDING));
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(projectToBeDeleted, Status.IN_PROGRESS));
    dbClient.ceQueueDao().insert(dbSession, createCeQueue(anotherLivingProject, Status.PENDING));
    dbSession.commit();

    underTest.deleteProject(dbSession, projectToBeDeleted.uuid());
    dbSession.commit();

    assertThat(db.countRowsOfTable("ce_queue")).isEqualTo(1);
    assertThat(db.countSql("select count(*) from ce_queue where main_component_uuid='" + projectToBeDeleted.uuid() + "'")).isEqualTo(0);
  }

  @Test
  public void delete_row_in_ce_task_input_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeTaskInput(projectTask.getUuid());
    CeQueueDto branchTask = insertCeQueue(branch);
    insertCeTaskInput(branchTask.getUuid());
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch);
    insertCeTaskInput(anotherBranchTask.getUuid());
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeTaskInput(anotherProjectTask.getUuid());
    insertCeTaskInput("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_input")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_scanner_context_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeScannerContext(projectTask.getUuid());
    CeQueueDto branchTask = insertCeQueue(branch);
    insertCeScannerContext(branchTask.getUuid());
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch);
    insertCeScannerContext(anotherBranchTask.getUuid());
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeScannerContext(anotherProjectTask.getUuid());
    insertCeScannerContext("non existing task");
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context"))
      .containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_scanner_context")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_task_characteristics_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeTaskCharacteristics(projectTask.getUuid(), 3);
    CeQueueDto branchTask = insertCeQueue(branch);
    insertCeTaskCharacteristics(branchTask.getUuid(), 1);
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch);
    insertCeTaskCharacteristics(anotherBranchTask.getUuid(), 5);
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeTaskCharacteristics(anotherProjectTask.getUuid(), 2);
    insertCeTaskCharacteristics("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics"))
      .containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_characteristics")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_ce_task_message_referring_to_a_row_in_ce_queue_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);

    CeQueueDto projectTask = insertCeQueue(project);
    insertCeTaskMessages(projectTask.getUuid(), 3);
    CeQueueDto branchTask = insertCeQueue(branch);
    insertCeTaskMessages(branchTask.getUuid(), 1);
    CeQueueDto anotherBranchTask = insertCeQueue(anotherBranch);
    insertCeTaskMessages(anotherBranchTask.getUuid(), 5);
    CeQueueDto anotherProjectTask = insertCeQueue(anotherProject);
    insertCeTaskMessages(anotherProjectTask.getUuid(), 2);
    insertCeTaskMessages("non existing task", 5);
    dbSession.commit();

    underTest.deleteProject(dbSession, branch.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message"))
      .containsOnly(projectTask.getUuid(), anotherBranchTask.getUuid(), anotherProjectTask.getUuid(), "non existing task");

    underTest.deleteProject(dbSession, project.uuid());
    dbSession.commit();

    assertThat(uuidsIn("ce_queue")).containsOnly(anotherProjectTask.getUuid());
    assertThat(taskUuidsIn("ce_task_message")).containsOnly(anotherProjectTask.getUuid(), "non existing task");
  }

  @Test
  public void delete_row_in_events_and_event_component_changes_when_deleting_project() {
    ComponentDto project = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    ComponentDto branch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherBranch = ComponentTesting.newProjectBranch(project, newBranchDto(project));
    ComponentDto anotherProject = ComponentTesting.newPrivateProjectDto(db.getDefaultOrganization());
    dbClient.componentDao().insert(dbSession, project, branch, anotherBranch, anotherProject);
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
    ComponentDto referencedProjectA = db.components().insertPublicProject();
    ComponentDto referencedProjectB = db.components().insertPublicProject();
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectA, null);
    db.events().insertEventComponentChanges(projectEvent1, projectAnalysis1, randomChangeCategory(), referencedProjectB, null);
    BranchDto branchProjectA = newBranchDto(referencedProjectA);
    ComponentDto cptBranchProjectA = ComponentTesting.newProjectBranch(referencedProjectA, branchProjectA);
    db.events().insertEventComponentChanges(projectEvent2, projectAnalysis2, randomChangeCategory(), cptBranchProjectA, branchProjectA);
    // note: projectEvent3 has no component change
    db.events().insertEventComponentChanges(branchEvent1, branchAnalysis1, randomChangeCategory(), referencedProjectB, null);
    db.events().insertEventComponentChanges(branchEvent2, branchAnalysis2, randomChangeCategory(), cptBranchProjectA, branchProjectA);
    db.events().insertEventComponentChanges(anotherBranchEvent, anotherBranchAnalysis, randomChangeCategory(), referencedProjectB, null);
    db.events().insertEventComponentChanges(anotherProjectEvent, anotherProjectAnalysis, randomChangeCategory(), referencedProjectB, null);

    // deleting referenced project does not delete any data
    underTest.deleteProject(dbSession, referencedProjectA.uuid());

    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(7);
    assertThat(db.countRowsOfTable("events"))
      .isEqualTo(7);

    underTest.deleteProject(dbSession, branch.uuid());
    assertThat(uuidsIn("event_component_changes", "event_component_uuid"))
      .containsOnly(project.uuid(), anotherBranch.uuid(), anotherProject.uuid());
    assertThat(db.countRowsOfTable("event_component_changes"))
      .isEqualTo(5);
    assertThat(uuidsIn("events"))
      .containsOnly(projectEvent1.getUuid(), projectEvent2.getUuid(), projectEvent3.getUuid(), anotherBranchEvent.getUuid(), anotherProjectEvent.getUuid());

    underTest.deleteProject(dbSession, project.uuid());
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

  private ComponentDto insertProjectWithBranchAndRelatedData() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();
    ComponentDto branch = db.components().insertProjectBranch(project);
    ComponentDto module = db.components().insertComponent(newModuleDto(branch));
    ComponentDto subModule = db.components().insertComponent(newModuleDto(module));
    ComponentDto file = db.components().insertComponent(newFileDto(subModule));
    db.issues().insert(rule, branch, file);
    db.issues().insert(rule, branch, subModule);
    db.issues().insert(rule, branch, module);
    return project;
  }

  @Test
  public void delete_branch_content_when_deleting_project() {
    ComponentDto anotherLivingProject = insertProjectWithBranchAndRelatedData();
    int projectEntryCount = db.countRowsOfTable("projects");
    int issueCount = db.countRowsOfTable("issues");
    int branchCount = db.countRowsOfTable("project_branches");

    ComponentDto projectToDelete = insertProjectWithBranchAndRelatedData();
    assertThat(db.countRowsOfTable("projects")).isGreaterThan(projectEntryCount);
    assertThat(db.countRowsOfTable("issues")).isGreaterThan(issueCount);
    assertThat(db.countRowsOfTable("project_branches")).isGreaterThan(branchCount);

    underTest.deleteProject(dbSession, projectToDelete.uuid());
    dbSession.commit();

    assertThat(db.countRowsOfTable("projects")).isEqualTo(projectEntryCount);
    assertThat(db.countRowsOfTable("issues")).isEqualTo(issueCount);
    assertThat(db.countRowsOfTable("project_branches")).isEqualTo(branchCount);
  }

  @Test
  public void delete_view_and_child() {
    db.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(db.countSql("select count(1) from projects where uuid='A'")).isZero();
    assertThat(db.countRowsOfTable("projects")).isZero();
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_module() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto module = db.components().insertComponent(ComponentTesting.newModuleDto(privateProject));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + module.uuid());

    underTest.deleteProject(dbSession, module.uuid());
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_directory() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto directory = db.components().insertComponent(newDirectory(privateProject, "A/B"));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + directory.uuid());

    underTest.deleteProject(dbSession, directory.uuid());
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_file() {
    ComponentDto privateProject = db.components().insertPrivateProject();
    ComponentDto file = db.components().insertComponent(newFileDto(privateProject));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + file.uuid());

    underTest.deleteProject(dbSession, file.uuid());
  }

  @Test
  public void deleteProject_fails_with_IAE_if_specified_component_is_subview() {
    ComponentDto view = db.components().insertView();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubView(view));

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Couldn't find root component with uuid " + subview.uuid());

    underTest.deleteProject(dbSession, subview.uuid());
  }

  @Test
  public void delete_view_sub_view_and_tech_project() {
    db.prepareDbUnit(getClass(), "view_sub_view_and_tech_project.xml");

    // view
    underTest.deleteProject(dbSession, "A");
    dbSession.commit();
    assertThat(db.countSql("select count(1) from projects where uuid='A'")).isZero();
  }

  @Test
  public void should_delete_old_closed_issues() {
    RuleDefinitionDto rule = db.rules().insert();
    ComponentDto project = db.components().insertMainBranch();

    ComponentDto module = db.components().insertComponent(newModuleDto(project));
    ComponentDto file = db.components().insertComponent(newFileDto(module));

    IssueDto oldClosed = db.issues().insert(rule, project, file, issue -> {
      issue.setStatus("CLOSED");
      issue.setIssueCloseDate(DateUtils.addDays(new Date(), -31));
    });

    IssueDto notOldEnoughClosed = db.issues().insert(rule, project, file, issue -> {
      issue.setStatus("CLOSED");
      issue.setIssueCloseDate(new Date());
    });
    IssueDto notClosed = db.issues().insert(rule, project, file);

    when(system2.now()).thenReturn(new Date().getTime());
    underTest.purge(dbSession, newConfigurationWith30Days(system2, project.uuid(), project.uuid()), PurgeListener.EMPTY, new PurgeProfiler());
    dbSession.commit();

    // old closed got deleted
    assertThat(db.getDbClient().issueDao().selectByKey(dbSession, oldClosed.getKey())).isEmpty();

    // others remain
    assertThat(db.countRowsOfTable("issues")).isEqualTo(2);
    assertThat(db.getDbClient().issueDao().selectByKey(dbSession, notOldEnoughClosed.getKey())).isNotEmpty();
    assertThat(db.getDbClient().issueDao().selectByKey(dbSession, notClosed.getKey())).isNotEmpty();
  }

  @Test
  public void deleteProject_deletes_webhook_deliveries() {
    ComponentDto project = db.components().insertPublicProject();
    dbClient.webhookDeliveryDao().insert(dbSession, newDto().setComponentUuid(project.uuid()).setUuid("D1").setDurationMs(1000).setWebhookUuid("webhook-uuid"));
    dbClient.webhookDeliveryDao().insert(dbSession, newDto().setComponentUuid("P2").setUuid("D2").setDurationMs(1000).setWebhookUuid("webhook-uuid"));

    underTest.deleteProject(dbSession, project.uuid());

    assertThat(selectAllDeliveryUuids(db, dbSession)).containsOnly("D2");
  }

  @Test
  public void deleteProject_deletes_project_mappings() {
    ComponentDto project = db.components().insertPublicProject();
    dbClient.projectMappingsDao().put(dbSession, "a.key.type", "a.key", project.uuid());
    dbClient.projectMappingsDao().put(dbSession, "a.key.type", "another.key", "D2");

    underTest.deleteProject(dbSession, project.uuid());

    assertThat(dbClient.projectMappingsDao().get(dbSession, "a.key.type", "a.key")).isEmpty();
    assertThat(dbClient.projectMappingsDao().get(dbSession, "a.key.type", "another.key")).isNotEmpty();
  }

  @Test
  public void deleteProject_deletes_project_alm_bindings() {
    ALM alm = ALM.GITHUB;
    String repoId = "123";
    String otherRepoId = repoId + "-foo";

    ComponentDto project = db.components().insertPublicProject();
    ComponentDto otherProject = db.components().insertPublicProject();
    dbClient.projectAlmBindingsDao().insertOrUpdate(dbSession, alm, repoId, project.uuid(), null, "foo");
    dbClient.projectAlmBindingsDao().insertOrUpdate(dbSession, alm, otherRepoId, otherProject.uuid(), null, "bar");

    underTest.deleteProject(dbSession, project.uuid());

    assertThat(dbClient.projectAlmBindingsDao().findProjectKey(dbSession, alm, repoId)).isEmpty();
    assertThat(dbClient.projectAlmBindingsDao().findProjectKey(dbSession, alm, otherRepoId)).isNotEmpty();
  }

  @Test
  public void deleteNonRootComponents_has_no_effect_when_parameter_is_empty() {
    DbSession dbSession = mock(DbSession.class);

    underTest.deleteNonRootComponentsInView(dbSession, Collections.emptyList());

    verifyZeroInteractions(dbSession);
  }

  @Test
  public void deleteNonRootComponents_has_no_effect_when_parameter_contains_only_projects_and_or_views() {
    ComponentDbTester componentDbTester = db.components();

    verifyNoEffect(componentDbTester.insertPrivateProject());
    verifyNoEffect(componentDbTester.insertPublicProject());
    verifyNoEffect(componentDbTester.insertView());
    verifyNoEffect(componentDbTester.insertView(), componentDbTester.insertPrivateProject(), componentDbTester.insertPublicProject());
  }

  @Test
  public void delete_live_measures_when_deleting_project() {
    MetricDto metric = db.measures().insertMetric();

    ComponentDto project1 = db.components().insertPublicProject();
    ComponentDto module1 = db.components().insertComponent(ComponentTesting.newModuleDto(project1));
    db.measures().insertLiveMeasure(project1, metric);
    db.measures().insertLiveMeasure(module1, metric);

    ComponentDto project2 = db.components().insertPublicProject();
    ComponentDto module2 = db.components().insertComponent(ComponentTesting.newModuleDto(project2));
    db.measures().insertLiveMeasure(project2, metric);
    db.measures().insertLiveMeasure(module2, metric);

    underTest.deleteProject(dbSession, project1.uuid());

    assertThat(dbClient.liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession, asList(project1.uuid(), module1.uuid()), asList(metric.getId()))).isEmpty();
    assertThat(dbClient.liveMeasureDao().selectByComponentUuidsAndMetricIds(dbSession, asList(project2.uuid(), module2.uuid()), asList(metric.getId()))).hasSize(2);
  }

  private void verifyNoEffect(ComponentDto firstRoot, ComponentDto... otherRoots) {
    DbSession dbSession = mock(DbSession.class);

    List<ComponentDto> componentDtos = Stream.concat(Stream.of(firstRoot), Arrays.stream(otherRoots)).collect(Collectors.toList());
    Collections.shuffle(componentDtos); // order of collection must not matter
    underTest.deleteNonRootComponentsInView(dbSession, componentDtos);

    verifyZeroInteractions(dbSession);
  }

  @Test
  public void deleteNonRootComponents_deletes_only_non_root_components_of_a_project_from_table_PROJECTS() {
    ComponentDto project = new Random().nextBoolean() ? db.components().insertPublicProject() : db.components().insertPrivateProject();
    ComponentDto module1 = db.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto module2 = db.components().insertComponent(ComponentTesting.newModuleDto(module1));
    ComponentDto dir1 = db.components().insertComponent(newDirectory(module1, "A/B"));

    List<ComponentDto> components = asList(
      project,
      module1,
      module2,
      dir1,
      db.components().insertComponent(newDirectory(module2, "A/C")),
      db.components().insertComponent(newFileDto(dir1)),
      db.components().insertComponent(newFileDto(module2)),
      db.components().insertComponent(newFileDto(project)));
    Collections.shuffle(components);

    underTest.deleteNonRootComponentsInView(dbSession, components);

    assertThat(uuidsIn("projects"))
      .containsOnly(project.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_only_non_root_components_of_a_view_from_table_PROJECTS() {
    ComponentDto[] projects = {
      db.components().insertPrivateProject(),
      db.components().insertPrivateProject(),
      db.components().insertPrivateProject()
    };

    ComponentDto view = db.components().insertView();
    ComponentDto subview1 = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = db.components().insertComponent(ComponentTesting.newSubView(subview1));
    List<ComponentDto> components = asList(
      view,
      subview1,
      subview2,
      db.components().insertComponent(newProjectCopy("a", projects[0], view)),
      db.components().insertComponent(newProjectCopy("b", projects[1], subview1)),
      db.components().insertComponent(newProjectCopy("c", projects[2], subview2)));
    Collections.shuffle(components);

    underTest.deleteNonRootComponentsInView(dbSession, components);

    assertThat(uuidsIn("projects"))
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_only_specified_non_root_components_of_a_project_from_table_PROJECTS() {
    ComponentDto project = new Random().nextBoolean() ? db.components().insertPublicProject() : db.components().insertPrivateProject();
    ComponentDto module1 = db.components().insertComponent(ComponentTesting.newModuleDto(project));
    ComponentDto module2 = db.components().insertComponent(ComponentTesting.newModuleDto(module1));
    ComponentDto dir1 = db.components().insertComponent(newDirectory(module1, "A/B"));
    ComponentDto dir2 = db.components().insertComponent(newDirectory(module2, "A/C"));
    ComponentDto file1 = db.components().insertComponent(newFileDto(dir1));
    ComponentDto file2 = db.components().insertComponent(newFileDto(module2));
    ComponentDto file3 = db.components().insertComponent(newFileDto(project));

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(file3));
    assertThat(uuidsIn("projects"))
      .containsOnly(project.uuid(), module1.uuid(), module2.uuid(), dir1.uuid(), dir2.uuid(), file1.uuid(), file2.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(module1, dir2, file1));
    assertThat(uuidsIn("projects"))
      .containsOnly(project.uuid(), module2.uuid(), dir1.uuid(), file2.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_only_specified_non_root_components_of_a_view_from_table_PROJECTS() {
    ComponentDto[] projects = {
      db.components().insertPrivateProject(),
      db.components().insertPrivateProject(),
      db.components().insertPrivateProject()
    };

    ComponentDto view = db.components().insertView();
    ComponentDto subview1 = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = db.components().insertComponent(ComponentTesting.newSubView(subview1));
    ComponentDto pc1 = db.components().insertComponent(newProjectCopy("a", projects[0], view));
    ComponentDto pc2 = db.components().insertComponent(newProjectCopy("b", projects[1], subview1));
    ComponentDto pc3 = db.components().insertComponent(newProjectCopy("c", projects[2], subview2));

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(pc3));
    assertThat(uuidsIn("projects"))
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid(),
        subview1.uuid(), subview2.uuid(), pc1.uuid(), pc2.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview1, pc2));
    assertThat(uuidsIn("projects"))
      .containsOnly(view.uuid(), projects[0].uuid(), projects[1].uuid(), projects[2].uuid(), subview2.uuid(), pc1.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_measures_of_any_non_root_component_of_a_view() {
    ComponentDto view = db.components().insertView();
    ComponentDto subview = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto pc = db.components().insertComponent(newProjectCopy("a", db.components().insertPrivateProject(), view));
    insertMeasureFor(view, subview, pc);
    assertThat(getComponentUuidsOfMeasures()).containsOnly(view.uuid(), subview.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(pc));
    assertThat(getComponentUuidsOfMeasures())
      .containsOnly(view.uuid(), subview.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview));
    assertThat(getComponentUuidsOfMeasures())
      .containsOnly(view.uuid());
  }

  @Test
  public void deleteNonRootComponents_deletes_properties_of_subviews_of_a_view() {
    ComponentDto view = db.components().insertView();
    ComponentDto subview1 = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = db.components().insertComponent(ComponentTesting.newSubView(subview1));
    ComponentDto subview3 = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto pc = db.components().insertComponent(newProjectCopy("a", db.components().insertPrivateProject(), view));
    insertPropertyFor(view, subview1, subview2, subview3, pc);
    assertThat(getResourceIdOfProperties()).containsOnly(view.getId(), subview1.getId(), subview2.getId(), subview3.getId(), pc.getId());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview1));
    assertThat(getResourceIdOfProperties())
      .containsOnly(view.getId(), subview2.getId(), subview3.getId(), pc.getId());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview2, subview3, pc));
    assertThat(getResourceIdOfProperties())
      .containsOnly(view.getId(), pc.getId());
  }

  @Test
  public void deleteNonRootComponentsInView_deletes_manual_measures_of_subviews_of_a_view() {
    ComponentDto view = db.components().insertView();
    ComponentDto subview1 = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto subview2 = db.components().insertComponent(ComponentTesting.newSubView(subview1));
    ComponentDto subview3 = db.components().insertComponent(ComponentTesting.newSubView(view));
    ComponentDto pc = db.components().insertComponent(newProjectCopy("a", db.components().insertPrivateProject(), view));
    insertManualMeasureFor(view, subview1, subview2, subview3, pc);
    assertThat(getComponentUuidsOfManualMeasures()).containsOnly(view.uuid(), subview1.uuid(), subview2.uuid(), subview3.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, singletonList(subview1));
    assertThat(getComponentUuidsOfManualMeasures())
      .containsOnly(view.uuid(), subview2.uuid(), subview3.uuid(), pc.uuid());

    underTest.deleteNonRootComponentsInView(dbSession, asList(subview2, subview3, pc));
    assertThat(getComponentUuidsOfManualMeasures())
      .containsOnly(view.uuid(), pc.uuid());
  }

  private void insertManualMeasureFor(ComponentDto... componentDtos) {
    Arrays.stream(componentDtos).forEach(componentDto -> dbClient.customMeasureDao().insert(dbSession, new CustomMeasureDto()
      .setComponentUuid(componentDto.uuid())
      .setMetricId(new Random().nextInt())));
    dbSession.commit();
  }

  private Stream<String> getComponentUuidsOfManualMeasures() {
    return db.select("select component_uuid as \"COMPONENT_UUID\" from manual_measures").stream()
      .map(row -> (String) row.get("COMPONENT_UUID"));
  }

  private Stream<Long> getResourceIdOfProperties() {
    return db.select("select resource_id as \"ID\" from properties").stream()
      .map(row -> (Long) row.get("ID"));
  }

  private void insertPropertyFor(ComponentDto... components) {
    Stream.of(components).forEach(componentDto -> db.properties().insertProperty(new PropertyDto()
      .setKey(randomAlphabetic(3))
      .setValue(randomAlphabetic(3))
      .setResourceId(componentDto.getId())));
  }

  private Stream<String> getComponentUuidsOfMeasures() {
    return db.select("select component_uuid as \"COMPONENT_UUID\" from project_measures").stream()
      .map(row -> (String) row.get("COMPONENT_UUID"));
  }

  private void insertMeasureFor(ComponentDto... components) {
    Arrays.stream(components).forEach(componentDto -> db.getDbClient().measureDao().insert(dbSession, new MeasureDto()
      .setMetricId(new Random().nextInt())
      .setComponentUuid(componentDto.uuid())
      .setAnalysisUuid(randomAlphabetic(3))));
    dbSession.commit();
  }

  private CeQueueDto createCeQueue(ComponentDto component, Status status) {
    CeQueueDto queueDto = new CeQueueDto();
    queueDto.setUuid(Uuids.create());
    queueDto.setTaskType(REPORT);
    queueDto.setComponentUuid(component.uuid());
    queueDto.setMainComponentUuid(firstNonNull(component.getMainBranchProjectUuid(), component.uuid()));
    queueDto.setSubmitterUuid("submitter uuid");
    queueDto.setCreatedAt(1_300_000_000_000L);
    queueDto.setStatus(status);
    return queueDto;
  }

  private CeActivityDto insertCeActivity(ComponentDto component) {
    Status unusedStatus = Status.values()[RandomUtils.nextInt(Status.values().length)];
    CeQueueDto queueDto = createCeQueue(component, unusedStatus);

    CeActivityDto dto = new CeActivityDto(queueDto);
    dto.setStatus(CeActivityDto.Status.SUCCESS);
    dto.setStartedAt(1_500_000_000_000L);
    dto.setExecutedAt(1_500_000_000_500L);
    dto.setExecutionTimeMs(500L);
    dbClient.ceActivityDao().insert(dbSession, dto);
    return dto;
  }

  private CeQueueDto insertCeQueue(ComponentDto component) {
    CeQueueDto res = new CeQueueDto()
      .setUuid(UuidFactoryFast.getInstance().create())
      .setTaskType("foo")
      .setComponentUuid(component.uuid())
      .setMainComponentUuid(firstNonNull(component.getMainBranchProjectUuid(), component.uuid()))
      .setStatus(Status.PENDING)
      .setCreatedAt(1_2323_222L)
      .setUpdatedAt(1_2323_222L);
    dbClient.ceQueueDao().insert(dbSession, res);
    dbSession.commit();
    return res;
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
      .collect(Collectors.toList());
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

  private Stream<String> taskUuidsIn(String tableName) {
    return uuidsIn(tableName, "task_uuid");
  }

  private Stream<String> uuidsIn(String tableName, String columnName) {
    return db.select("select " + columnName + " as \"UUID\" from " + tableName)
      .stream()
      .map(row -> (String) row.get("UUID"));
  }

  private static PurgeConfiguration newConfigurationWith30Days() {
    return new PurgeConfiguration(PROJECT_UUID, PROJECT_UUID, emptyList(), 30, Optional.of(30), System2.INSTANCE, Collections.emptyList());
  }

  private static PurgeConfiguration newConfigurationWith30Days(System2 system2, String rootUuid, String projectUuid, String... disabledComponentUuids) {
    return new PurgeConfiguration(rootUuid, projectUuid, emptyList(), 30, Optional.of(30), system2, asList(disabledComponentUuids));
  }

}
