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

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueStatsIndexer;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;
import org.sonarsource.compliancereports.ingestion.IssueIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;

public class MigrateToIssuesActionIT {

  private final System2 system2 = new TestSystem2().setNow(1_500_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final IssueFieldsSetter issueFieldsSetter = new IssueFieldsSetter();
  // Indexer is mocked: this test verifies DB/changelog/scope behaviour, not ES indexing (kept out to avoid an ES dependency).
  private final IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private final WebIssueStorage issueStorage = new WebIssueStorage(system2, dbClient,
    new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, uuidFactory);
  private final TestIssueChangePostProcessor issueChangePostProcessor = new TestIssueChangePostProcessor();
  private final IssueStatsByRuleKeyDaoImpl issueStatsByRuleKeyDao = new IssueStatsByRuleKeyDaoImpl(dbClient);
  private final IssueStatsIndexer issueStatsIndexer = new IssueStatsIndexer(dbClient,
    new IssueIngestionService(issueStatsByRuleKeyDao), issueStatsByRuleKeyDao);
  private final MigrationBatchWriter batchWriter = new MigrationBatchWriter(dbClient, issueStorage, issueChangePostProcessor,
    issueIndexer, issueStatsIndexer, uuidFactory, system2);
  private final HotspotsToIssuesMigrator migrator = new HotspotsToIssuesMigrator(dbClient, issueFieldsSetter, batchWriter,
    system2, userSession);

  @org.junit.Before
  public void stubIndexer() {
    when(issueIndexer.enqueueForIndexing(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
      .thenReturn(java.util.List.of());
  }

  private final WsActionTester tester = new WsActionTester(new MigrateToIssuesAction(userSession, migrator));

  @Test
  public void handle_whenNotSystemAdministrator_shouldThrowForbidden() {
    userSession.logIn();
    TestRequest request = tester.newRequest();

    assertThatThrownBy(request::execute)
      .isInstanceOf(ForbiddenException.class);
  }

  @Test
  public void handle_shouldMigrateHotspotFindingsToTheirRuleTargetType() {
    userSession.logIn().setSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    RuleDto codeSmellRule = db.rules().insert(r -> r.setType(RuleType.CODE_SMELL));

    IssueDto onVuln = insertHotspotFinding(vulnerabilityRule, branch, file);
    IssueDto onCodeSmell = insertHotspotFinding(codeSmellRule, branch, file);

    TestResponse response = tester.newRequest().execute();

    assertThat(response.getInput()).contains("\"dryRun\":false", project.getProjectDto().getKey(), "\"migrated\":2", "\"skipped\":0");
    assertThat(reloadType(onVuln)).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(reloadType(onCodeSmell)).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(reload(onVuln).getTags()).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    assertThat(issueChangePostProcessor.wasCalled()).isTrue();
  }

  @Test
  public void handle_dryRun_shouldCountWithoutWriting() {
    userSession.logIn().setSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    IssueDto hotspot = insertHotspotFinding(vulnerabilityRule, branch, file);

    TestResponse response = tester.newRequest().setParam("dryRun", "true").execute();

    assertThat(response.getInput()).contains("\"dryRun\":true", "\"migrated\":1");
    // No writes: the finding is still a hotspot and no measures/QG recompute happened.
    assertThat(reloadType(hotspot)).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();
  }

  @Test
  public void handle_shouldRecordSingleChangelogWithNativeTypeDiff() {
    userSession.logIn().setSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    IssueDto hotspot = insertHotspotFinding(vulnerabilityRule, branch, file);

    tester.newRequest().execute();

    List<FieldDiffs> changelog = dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), hotspot.getKey());
    assertThat(changelog).hasSize(1);
    // Native 'type' diff records the migration (no custom 'migration' key — avoids a webapp label dependency).
    assertThat(changelog.get(0).diffs()).containsKey("type").doesNotContainKey("migration");
  }

  @Test
  public void handle_shouldScopeToRequestedProject() {
    userSession.logIn().setSystemAdministrator();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));

    ProjectData projectA = db.components().insertPrivateProject();
    ComponentDto branchA = projectA.getMainBranchComponent();
    IssueDto onA = insertHotspotFinding(vulnerabilityRule, branchA, db.components().insertComponent(newFileDto(branchA)));

    ProjectData projectB = db.components().insertPrivateProject();
    ComponentDto branchB = projectB.getMainBranchComponent();
    IssueDto onB = insertHotspotFinding(vulnerabilityRule, branchB, db.components().insertComponent(newFileDto(branchB)));

    tester.newRequest().setParam("project", projectA.getProjectDto().getKey()).execute();

    assertThat(reloadType(onA)).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(reloadType(onB)).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
  }

  @Test
  public void handle_shouldSkipHotspotsWhoseRuleIsNotConvertedYet() {
    userSession.logIn().setSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleDto stillHotspotRule = db.rules().insertHotspotRule();
    IssueDto hotspot = insertHotspotFinding(stillHotspotRule, branch, file);

    TestResponse response = tester.newRequest().execute();

    // Reported as a scope-wide 'skipped', with no per-project entry: the finding is filtered out in SQL, so the
    // run never loads it and cannot attribute it to a project.
    assertThat(response.getInput()).contains("\"skipped\":1", "\"projects\":[]");
    assertThat(reloadType(hotspot)).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
  }

  @Test
  public void handle_shouldApplyTheStatusMappingToEveryHotspotStatus() {
    userSession.logIn().setSystemAdministrator();
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    IssueDto toReview = insertHotspotWith(rule, branch, file, Issue.STATUS_TO_REVIEW, null);
    IssueDto acknowledged = insertHotspotWith(rule, branch, file, Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED);
    IssueDto fixed = insertHotspotWith(rule, branch, file, Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED);
    IssueDto safe = insertHotspotWith(rule, branch, file, Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE);
    IssueDto closedRemoved = insertHotspotWith(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED);

    TestResponse response = tester.newRequest().execute();

    assertThat(response.getInput()).contains("\"migrated\":5").doesNotContain("repaired");
    assertStatus(toReview, Issue.STATUS_OPEN, null, IssueStatus.OPEN);
    assertStatus(acknowledged, Issue.STATUS_CONFIRMED, null, IssueStatus.CONFIRMED);
    // RESOLVED, not CLOSED: issue tracking skips CLOSED issues, and a reviewed hotspot is still detected. Fixed is a
    // review users set by hand, so it is kept as Accepted like Safe instead of being re-evaluated by the analyzer.
    assertStatus(fixed, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
    assertThat(reload(fixed).getTags()).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
    assertThat(reload(safe).getTags()).doesNotContain(HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
    assertStatus(safe, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
    assertStatus(closedRemoved, Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED, IssueStatus.FIXED);
  }

  @Test
  public void handle_shouldRepairFindingsMigratedByThePreviousMappingAndReportThem() {
    userSession.logIn().setSystemAdministrator();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    IssueDto safe = insertPreviouslyMigrated(rule, branch, file, Issue.RESOLUTION_WONT_FIX, false);
    IssueDto fixed = insertPreviouslyMigrated(rule, branch, file, Issue.RESOLUTION_FIXED, true);
    // Closed before the migration (no migration status diff): must stay closed.
    IssueDto closedBeforeMigration = insertPreviouslyMigrated(rule, branch, file, Issue.RESOLUTION_FIXED, false);

    TestResponse preview = tester.newRequest().setParam("dryRun", "true").execute();

    assertThat(preview.getInput()).contains("\"dryRun\":true", "\"migrated\":0", "\"repaired\":2", project.getProjectDto().getKey());
    assertStatus(safe, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, IssueStatus.FIXED);
    assertThat(issueChangePostProcessor.wasCalled()).isFalse();

    TestResponse response = tester.newRequest().execute();

    assertThat(response.getInput()).contains("\"dryRun\":false", "\"migrated\":0", "\"repaired\":2");
    assertStatus(safe, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
    assertStatus(fixed, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
    assertThat(reload(fixed).getTags()).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
    assertThat(reload(safe).getTags()).doesNotContain(HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
    assertStatus(closedBeforeMigration, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, IssueStatus.FIXED);
    assertThat(issueChangePostProcessor.wasCalled()).isTrue();

    TestResponse rerun = tester.newRequest().execute();

    assertThat(rerun.getInput()).contains("\"migrated\":0", "\"projects\":[]").doesNotContain("repaired");
  }

  @Test
  public void handle_whenProjectGiven_shouldOnlyRepairThatProject() {
    userSession.logIn().setSystemAdministrator();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData projectA = db.components().insertPrivateProject();
    ComponentDto branchA = projectA.getMainBranchComponent();
    IssueDto inA = insertPreviouslyMigrated(rule, branchA, db.components().insertComponent(newFileDto(branchA)), Issue.RESOLUTION_WONT_FIX, false);
    ComponentDto branchB = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto inB = insertPreviouslyMigrated(rule, branchB, db.components().insertComponent(newFileDto(branchB)), Issue.RESOLUTION_WONT_FIX, false);

    TestResponse response = tester.newRequest().setParam("project", projectA.getProjectDto().getKey()).execute();

    assertThat(response.getInput()).contains("\"repaired\":1");
    assertStatus(inA, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
    assertStatus(inB, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, IssueStatus.FIXED);
  }

  private IssueDto insertHotspotWith(RuleDto rule, ComponentDto branch, ComponentDto file, String status, @Nullable String resolution) {
    return db.issues().insert(rule, branch, file, i -> i
      .setType(RuleType.SECURITY_HOTSPOT)
      .setStatus(status)
      .setResolution(resolution)
      .setTags(List.of()));
  }

  /** A finding as the previous version of the migration left it: rule type, former-hotspot tag, stored as CLOSED. */
  private IssueDto insertPreviouslyMigrated(RuleDto rule, ComponentDto branch, ComponentDto file, String resolution, boolean withMigrationStatusDiff) {
    IssueDto issue = db.issues().insert(rule, branch, file, i -> i
      .setType(rule.getEnumType())
      .setStatus(Issue.STATUS_CLOSED)
      .setResolution(resolution)
      .setTags(List.of(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG)));
    if (withMigrationStatusDiff) {
      db.issues().insertFieldDiffs(issue, new FieldDiffs()
        .setIssueKey(issue.getKey())
        .setCreationDate(new Date(system2.now() - 1_000))
        .setDiff("status", Issue.STATUS_REVIEWED, Issue.STATUS_CLOSED)
        .setDiff("tags", null, "former-hotspot"));
    }
    return issue;
  }

  private void assertStatus(IssueDto issue, String status, @Nullable String resolution, IssueStatus issueStatus) {
    IssueDto reloaded = reload(issue);
    assertThat(reloaded.getStatus()).isEqualTo(status);
    assertThat(reloaded.getResolution()).isEqualTo(resolution);
    assertThat(IssueStatus.of(reloaded.getStatus(), reloaded.getResolution())).isEqualTo(issueStatus);
  }

  private IssueDto insertHotspotFinding(RuleDto rule, ComponentDto branch, ComponentDto file) {
    // Clear the fixture's random tags: newIssue() generates values with '_' that don't pass RuleTagFormat
    // (real issue tags are always pre-validated on write, so this only matters for the direct-insert fixture).
    return db.issues().insert(rule, branch, file, i -> i
      .setType(RuleType.SECURITY_HOTSPOT)
      .setStatus(Issue.STATUS_TO_REVIEW)
      .setResolution(null)
      .setTags(List.of()));
  }

  private IssueDto reload(IssueDto issue) {
    db.getSession().clearCache();
    return dbClient.issueDao().selectOrFailByKey(db.getSession(), issue.getKey());
  }

  private int reloadType(IssueDto issue) {
    return reload(issue).getType();
  }
}