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

import java.util.List;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonar.server.ws.TestRequest;
import org.sonar.server.ws.TestResponse;
import org.sonar.server.ws.WsActionTester;

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
  private final MigrationBatchWriter batchWriter = new MigrationBatchWriter(dbClient, issueStorage, issueChangePostProcessor,
    issueIndexer, uuidFactory, system2);
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

    assertThat(response.getInput()).contains("\"migrated\":0", "\"skipped\":1");
    assertThat(reloadType(hotspot)).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
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
    return dbClient.issueDao().selectOrFailByKey(db.getSession(), issue.getKey());
  }

  private int reloadType(IssueDto issue) {
    return reload(issue).getType();
  }
}