/*
 * SonarQube
 * Copyright (C) SonarSource Sàrl
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
package org.sonar.server.hotspot.ws;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.hotspot.ws.HotspotsToIssuesMigrator.MigrationResult;
import org.sonar.server.hotspot.ws.HotspotsToIssuesMigrator.ProjectMigrationResult;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueUpdatedTelemetryPublisher;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.telemetry.core.event.AnalyticsEventPublisher;
import org.sonar.telemetry.core.event.workflow.IssueUpdatedBatchEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;

@SuppressWarnings("deprecation") // legacy STATUS_*/RESOLUTION_* constants are the persisted column values
public class HotspotsToIssuesMigratorIT {

  private final System2 system2 = new TestSystem2().setNow(1_500_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();
  @Rule
  public LogTester logTester = new LogTester();

  private final DbClient dbClient = db.getDbClient();
  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  // Indexer mocked: verifies DB/changelog/scope behaviour, not ES indexing (avoids an ES dependency).
  private final IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private final AnalyticsEventPublisher analyticsEventPublisher = mock(AnalyticsEventPublisher.class);
  private final IssueUpdatedTelemetryPublisher issueUpdatedTelemetryPublisher = new IssueUpdatedTelemetryPublisher(dbClient, analyticsEventPublisher);
  private final WebIssueStorage issueStorage = new WebIssueStorage(system2, dbClient,
    new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, uuidFactory, issueUpdatedTelemetryPublisher);
  private final TestIssueChangePostProcessor postProcessor = new TestIssueChangePostProcessor();
  private final MigrationBatchWriter batchWriter = new MigrationBatchWriter(dbClient, issueStorage, postProcessor,
    issueIndexer, uuidFactory, system2, issueUpdatedTelemetryPublisher);

  private final HotspotsToIssuesMigrator underTest = new HotspotsToIssuesMigrator(dbClient, issueFieldsSetter, batchWriter,
    system2, userSession);

  @org.junit.Before
  public void stubIndexer() {
    when(issueIndexer.enqueueForIndexing(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
      .thenReturn(java.util.List.of());
  }

  @Test
  public void migrate_shouldSetRuleTargetTypeAndFormerHotspotTag() {
    when(analyticsEventPublisher.isTelemetryEnabled()).thenReturn(true);
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    RuleDto codeSmellRule = db.rules().insert(r -> r.setType(RuleType.CODE_SMELL));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto onVuln = insertHotspot(vulnerabilityRule, branch, file, i -> {});
    IssueDto onCodeSmell = insertHotspot(codeSmellRule, branch, file, i -> {});

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.dryRun()).isFalse();
    assertThat(result.projects()).extracting(ProjectMigrationResult::projectKey, ProjectMigrationResult::migrated, ProjectMigrationResult::skipped)
      .containsExactly(tuple(project.getProjectDto().getKey(), 2, 0));
    assertThat(reload(onVuln).getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(reload(onCodeSmell).getType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(reload(onVuln).getTags()).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    assertThat(postProcessor.wasCalled()).isTrue();
    verify(analyticsEventPublisher).publishAll(eq(IssueUpdatedBatchEvent.TYPE), any());
  }

  @Test
  public void migrate_shouldMapToReviewToOpen() {
    assertStatusMapping(Issue.STATUS_TO_REVIEW, null, Issue.STATUS_OPEN, null);
  }

  @Test
  public void migrate_shouldMapReviewedAcknowledgedToConfirmed() {
    assertStatusMapping(Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED, Issue.STATUS_CONFIRMED, null);
  }

  @Test
  public void migrate_shouldMapReviewedFixedToClosedFixed() {
    assertStatusMapping(Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED);
  }

  @Test
  public void migrate_shouldMapReviewedSafeToClosedWontFix() {
    assertStatusMapping(Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX);
  }

  @Test
  public void migrate_shouldReportPerProject() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData projectA = db.components().insertPrivateProject();
    ComponentDto branchA = projectA.getMainBranchComponent();
    insertHotspot(vulnerabilityRule, branchA, db.components().insertComponent(newFileDto(branchA)), i -> {});
    insertHotspot(vulnerabilityRule, branchA, db.components().insertComponent(newFileDto(branchA)), i -> {});
    ProjectData projectB = db.components().insertPrivateProject();
    ComponentDto branchB = projectB.getMainBranchComponent();
    insertHotspot(vulnerabilityRule, branchB, db.components().insertComponent(newFileDto(branchB)), i -> {});

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::projectKey, ProjectMigrationResult::migrated)
      .containsExactlyInAnyOrder(
        tuple(projectA.getProjectDto().getKey(), 2),
        tuple(projectB.getProjectDto().getKey(), 1));
  }

  @Test
  public void migrate_shouldMigrateEveryFindingAcrossBranchesInSingleCall() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto mainBranch = project.getMainBranchComponent();
    ComponentDto mainFile = db.components().insertComponent(newFileDto(mainBranch));
    ComponentDto featureBranch = db.components().insertProjectBranch(mainBranch);
    ComponentDto featureFile = db.components().insertComponent(newFileDto(featureBranch));
    IssueDto mainHotspot1 = insertHotspot(vulnerabilityRule, mainBranch, mainFile, i -> {});
    IssueDto mainHotspot2 = insertHotspot(vulnerabilityRule, mainBranch, mainFile, i -> {});
    IssueDto featureHotspot1 = insertHotspot(vulnerabilityRule, featureBranch, featureFile, i -> {});
    IssueDto featureHotspot2 = insertHotspot(vulnerabilityRule, featureBranch, featureFile, i -> {});

    // A single call must fully drain the project. The migration commits a batch at every branch boundary; a
    // regression (SONAR-31061) held the finding cursor open across those commits, so the commit truncated it and
    // later branches were silently left un-migrated. Keyset paging re-queries per page and is immune.
    MigrationResult result = underTest.migrate(project.getProjectDto().getKey(), false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated, ProjectMigrationResult::skipped)
      .containsExactly(tuple(4, 0));
    assertThat(List.of(mainHotspot1, mainHotspot2, featureHotspot1, featureHotspot2))
      .allSatisfy(hotspot -> assertThat(reload(hotspot).getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant()));
  }

  @Test
  public void migrate_shouldMigrateAllFindingsAcrossMultipleKeysetPagesInSingleCall() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    List<IssueDto> hotspots = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      hotspots.add(insertHotspot(vulnerabilityRule, branch, file, h -> {}));
    }
    // Force several keyset pages (2, 2, 1). The keyset must advance across pages so a single call migrates them all;
    // the previous cursor implementation (SONAR-31061) dropped every finding past the first mid-run commit.
    underTest.setPageSize(2);

    MigrationResult result = underTest.migrate(project.getProjectDto().getKey(), false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated, ProjectMigrationResult::skipped)
      .containsExactly(tuple(5, 0));
    assertThat(hotspots)
      .allSatisfy(hotspot -> assertThat(reload(hotspot).getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant()));
  }

  @Test
  public void migrate_dryRun_shouldCountButNotWrite() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    IssueDto hotspot = insertHotspot(vulnerabilityRule, branch, db.components().insertComponent(newFileDto(branch)), i -> {});

    MigrationResult result = underTest.migrate(null, true);

    assertThat(result.dryRun()).isTrue();
    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated).containsExactly(1);
    assertThat(reload(hotspot).getType()).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
    assertThat(postProcessor.wasCalled()).isFalse();
  }

  @Test
  public void migrate_shouldLogProgressStartCommittedBatchAndFinish() {
    logInAdmin();
    logTester.setLevel(Level.INFO);
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    insertHotspot(vulnerabilityRule, branch, file, h -> {});
    insertHotspot(vulnerabilityRule, branch, file, h -> {});

    underTest.migrate(project.getProjectDto().getKey(), false);

    assertThat(logTester.logs(Level.INFO))
      .anyMatch(l -> l.contains("Hotspots-to-issues migration started") && l.contains("project " + project.getProjectDto().getKey()))
      .anyMatch(l -> l.contains("Committed migration batch of 2 hotspots"))
      .anyMatch(l -> l.contains("migration finished") && l.contains("2 migrated") && l.contains("0 skipped"));
  }

  @Test
  public void migrate_shouldKeepClosedHotspotClosedWithoutWarning() {
    logInAdmin();
    logTester.setLevel(Level.WARN);
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    // A hotspot removed from code is CLOSED with resolution REMOVED — a valid shared status, not an inconsistency.
    IssueDto closedHotspot = insertHotspot(vulnerabilityRule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_REMOVED));

    MigrationResult result = underTest.migrate(null, false);

    // Counted as migrated (type + tag applied) but status/resolution left untouched — CLOSED needs no remap.
    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated, ProjectMigrationResult::skipped)
      .containsExactly(tuple(1, 0));
    IssueDto migrated = reload(closedHotspot);
    assertThat(migrated.getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(migrated.getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(migrated.getResolution()).isEqualTo(Issue.RESOLUTION_REMOVED);
    assertThat(migrated.getTags()).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    // Valid case -> no "Unexpected hotspot status" warning.
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void migrate_shouldSkipAndCountHotspotsWhoseRuleIsStillHotspot() {
    logInAdmin();
    RuleDto stillHotspotRule = db.rules().insertHotspotRule();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    IssueDto hotspot = insertHotspot(stillHotspotRule, branch, db.components().insertComponent(newFileDto(branch)), i -> {});

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated, ProjectMigrationResult::skipped)
      .containsExactly(tuple(0, 1));
    assertThat(reload(hotspot).getType()).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
  }

  @Test
  public void migrate_whenProjectKeyUnknown_shouldThrowNotFound() {
    logInAdmin();

    assertThatThrownBy(() -> underTest.migrate("does-not-exist", false))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("does-not-exist");
  }

  private void assertStatusMapping(String hotspotStatus, String hotspotResolution, String expectedStatus, String expectedResolution) {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    IssueDto hotspot = insertHotspot(vulnerabilityRule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setStatus(hotspotStatus).setResolution(hotspotResolution));

    underTest.migrate(null, false);

    IssueDto migrated = reload(hotspot);
    assertThat(migrated.getStatus()).isEqualTo(expectedStatus);
    assertThat(migrated.getResolution()).isEqualTo(expectedResolution);
  }

  private void logInAdmin() {
    userSession.logIn().setSystemAdministrator();
  }

  private IssueDto insertHotspot(RuleDto rule, ComponentDto branch, ComponentDto file, Consumer<IssueDto> populator) {
    return db.issues().insert(rule, branch, file, i -> {
      i.setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_TO_REVIEW).setResolution(null).setTags(List.of());
      populator.accept(i);
    });
  }

  private IssueDto reload(IssueDto issue) {
    return dbClient.issueDao().selectOrFailByKey(db.getSession(), issue.getKey());
  }
}