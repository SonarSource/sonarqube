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
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.component.ProjectData;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.issue.IssueDao;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.user.UserDto;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.hotspot.ws.HotspotsToIssuesMigrator.MigrationResult;
import org.sonar.server.hotspot.ws.HotspotsToIssuesMigrator.ProjectMigrationResult;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueStatsIndexer;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonarsource.compliancereports.dao.AggregationType;
import org.sonarsource.compliancereports.dao.IssueStats;
import org.sonarsource.compliancereports.ingestion.IssueIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
  private final WebIssueStorage issueStorage = new WebIssueStorage(system2, dbClient,
    new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, uuidFactory);
  private final TestIssueChangePostProcessor postProcessor = new TestIssueChangePostProcessor();
  private final IssueStatsByRuleKeyDaoImpl issueStatsByRuleKeyDao = new IssueStatsByRuleKeyDaoImpl(dbClient);
  private final IssueStatsIndexer issueStatsIndexer = new IssueStatsIndexer(dbClient,
    new IssueIngestionService(issueStatsByRuleKeyDao), issueStatsByRuleKeyDao);
  private final MigrationBatchWriter batchWriter = new MigrationBatchWriter(dbClient, issueStorage, postProcessor,
    issueIndexer, issueStatsIndexer, uuidFactory, system2);

  private final HotspotsToIssuesMigrator underTest = new HotspotsToIssuesMigrator(dbClient, issueFieldsSetter, batchWriter,
    system2, userSession);

  @org.junit.Before
  public void stubIndexer() {
    when(issueIndexer.enqueueForIndexing(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
      .thenReturn(java.util.List.of());
  }

  @Test
  public void migrate_shouldSetRuleTargetTypeAndFormerHotspotTag() {
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
    assertThat(result.skipped()).isZero();
    assertThat(result.projects()).extracting(ProjectMigrationResult::projectKey, ProjectMigrationResult::migrated)
      .containsExactly(tuple(project.getProjectDto().getKey(), 2));
    assertThat(reload(onVuln).getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(reload(onCodeSmell).getType()).isEqualTo(RuleType.CODE_SMELL.getDbConstant());
    assertThat(reload(onVuln).getTags()).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    assertThat(postProcessor.wasCalled()).isTrue();
  }

  @Test
  public void migrate_shouldMapToReviewToOpen() {
    assertStatusMapping(Issue.STATUS_TO_REVIEW, null, Issue.STATUS_OPEN, null, IssueStatus.OPEN);
  }

  @Test
  public void migrate_shouldMapReviewedAcknowledgedToConfirmed() {
    assertStatusMapping(Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED, Issue.STATUS_CONFIRMED, null, IssueStatus.CONFIRMED);
  }

  @Test
  public void migrate_shouldMapReviewedFixedToResolvedWontFixShownAsAccepted() {
    // Reviewed/Fixed is a review a user sets by hand on a hotspot the analyzer still raises: it is kept like Safe, not
    // re-evaluated (RESOLVED/FIXED would reopen it as a vulnerability), and never CLOSED, which tracking skips.
    assertStatusMapping(Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
  }

  @Test
  public void migrate_shouldMapReviewedWithoutKnownResolutionToConfirmed() {
    // Not reachable through the workflow (REVIEWED always carries a resolution), but pinned so it does not change silently.
    assertStatusMapping(Issue.STATUS_REVIEWED, null, Issue.STATUS_CONFIRMED, null, IssueStatus.CONFIRMED);
  }

  @Test
  public void migrate_shouldKeepTheStatusOfAHotspotInAnUnexpectedStatusAndWarn() {
    logInAdmin();
    logTester.setLevel(Level.WARN);
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto weird = insertHotspot(rule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setStatus(Issue.STATUS_CONFIRMED).setResolution(null));

    underTest.migrate(null, false);

    IssueDto migrated = reload(weird);
    assertThat(migrated.getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(migrated.getStatus()).isEqualTo(Issue.STATUS_CONFIRMED);
    assertThat(logTester.logs(Level.WARN)).anyMatch(l -> l.contains("Unexpected hotspot status 'CONFIRMED'"));
  }

  @Test
  public void migrate_shouldAddTheReviewedFixedTagAndCommentOnlyToReviewedFixed() {
    logInAdmin();
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    Map<String, IssueDto> byCase = new java.util.LinkedHashMap<>();
    for (RuleType targetType : List.of(RuleType.VULNERABILITY, RuleType.CODE_SMELL, RuleType.BUG)) {
      RuleDto rule = db.rules().insert(r -> r.setType(targetType));
      byCase.put("fixed-" + targetType, insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_FIXED)));
      byCase.put("safe-" + targetType, insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_SAFE)));
      byCase.put("ack-" + targetType, insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_ACKNOWLEDGED)));
      byCase.put("review-" + targetType, insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_TO_REVIEW).setResolution(null)));
      byCase.put("closed-" + targetType, insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_REMOVED)));
    }

    underTest.migrate(null, false);

    byCase.forEach((name, hotspot) -> {
      List<String> comments = commentsOf(hotspot);
      List<String> tags = reload(hotspot).getTags().stream().toList();
      if (name.startsWith("fixed-")) {
        assertThat(comments).as(name).containsExactly(HotspotsToIssuesMigrator.MIGRATED_FROM_REVIEWED_FIXED_COMMENT);
        assertThat(tags).as(name).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG, HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
      } else {
        assertThat(comments).as(name).isEmpty();
        assertThat(tags).as(name).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
      }
    });
  }

  @Test
  public void migrate_shouldAttributeTheMigratedFromReviewedFixedCommentToTheAdministratorRunningIt() {
    userSession.logIn("admin-login").setSystemAdministrator();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto fixed = insertHotspot(rule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_FIXED));

    underTest.migrate(null, false);

    assertThat(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), List.of(fixed.getKey()), IssueChangeDto.TYPE_COMMENT))
      .singleElement().satisfies(comment -> assertThat(comment.getUserUuid()).isEqualTo(userSession.getUuid()));
  }

  @Test
  public void migrate_shouldPreserveTheExistingHistoryAndAssigneeOfAHotspot() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    UserDto assignee = db.users().insertUser();
    IssueDto hotspot = insertHotspot(rule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_FIXED).setAssigneeUuid(assignee.getUuid()).setLine(12));
    db.issues().insertComment(hotspot, assignee, "reviewed on 2025-03-01");
    db.issues().insertFieldDiffs(hotspot, new FieldDiffs().setIssueKey(hotspot.getKey()).setCreationDate(new Date(system2.now() - 5_000))
      .setDiff("status", Issue.STATUS_TO_REVIEW, Issue.STATUS_REVIEWED).setDiff("resolution", null, Issue.RESOLUTION_FIXED));

    underTest.migrate(null, false);

    IssueDto migrated = reload(hotspot);
    assertThat(migrated.getKey()).isEqualTo(hotspot.getKey());
    assertThat(migrated.getAssigneeUuid()).isEqualTo(assignee.getUuid());
    assertThat(migrated.getLine()).isEqualTo(12);
    assertThat(commentsOf(hotspot)).containsExactlyInAnyOrder("reviewed on 2025-03-01", HotspotsToIssuesMigrator.MIGRATED_FROM_REVIEWED_FIXED_COMMENT);
    // The earlier diff is kept and the migration appends its own.
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), hotspot.getKey())).hasSize(2);
  }

  @Test
  public void migrate_shouldBeIdempotentAndNotRepeatTheComment() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto fixed = insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_FIXED));
    IssueDto safe = insertHotspot(rule, branch, file, i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_SAFE));

    assertThat(underTest.migrate(null, false).totalMigrated()).isEqualTo(2);
    MigrationResult secondRun = underTest.migrate(null, false);

    assertThat(secondRun.totalMigrated()).isZero();
    assertThat(secondRun.totalRepaired()).isZero();
    assertThat(commentsOf(fixed)).hasSize(1);
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), fixed.getKey())).hasSize(1);
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), safe.getKey())).hasSize(1);
  }

  @Test
  public void migrate_shouldMapReviewedSafeToResolvedWontFixShownAsAccepted() {
    // "Accepted" is RESOLVED + WONTFIX. CLOSED would read as Fixed (IssueStatus.FIXED) and is skipped by issue tracking.
    assertStatusMapping(Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED);
  }

  @Test
  public void migrate_shouldKeepHotspotsClosedBecauseRemovedFromCodeClosed() {
    for (String resolution : List.of(Issue.RESOLUTION_REMOVED, Issue.RESOLUTION_FIXED)) {
      assertStatusMapping(Issue.STATUS_CLOSED, resolution, Issue.STATUS_CLOSED, resolution, IssueStatus.FIXED);
    }
  }

  @Test
  public void migrate_shouldRecordStatusDiffOnlyForHotspotsWhoseStatusChanged() {
    // A hotspot that was already closed has no status diff; a Reviewed one records REVIEWED -> RESOLVED. The repair of
    // rows migrated by the previous mapping selects REVIEWED -> CLOSED, so a row migrated now can never match it.
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto reviewedFixed = insertHotspot(vulnerabilityRule, branch, file,
      i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_FIXED));
    IssueDto reviewedSafe = insertHotspot(vulnerabilityRule, branch, file,
      i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_SAFE));
    IssueDto alreadyClosed = insertHotspot(vulnerabilityRule, branch, file,
      i -> i.setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED));

    underTest.migrate(null, false);

    var fixedDiffs = dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), reviewedFixed.getKey());
    assertThat(fixedDiffs).hasSize(1);
    assertThat(fixedDiffs.get(0).get("status").oldValue()).isEqualTo(Issue.STATUS_REVIEWED);
    assertThat(fixedDiffs.get(0).get("status").newValue()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(fixedDiffs.get(0).get("resolution").oldValue()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(fixedDiffs.get(0).get("resolution").newValue()).isEqualTo(Issue.RESOLUTION_WONT_FIX);
    assertThat(String.valueOf(fixedDiffs.get(0).get("tags").newValue())).contains("former-hotspot", "former-hotspot-reviewed-fixed");
    assertThat(hasStatusChange(reviewedSafe, Issue.STATUS_REVIEWED, Issue.STATUS_RESOLVED)).isTrue();
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), reviewedSafe.getKey()).get(0).get("resolution").oldValue())
      .isEqualTo(Issue.RESOLUTION_SAFE);
    assertThat(hasStatusChange(reviewedFixed, Issue.STATUS_REVIEWED, Issue.STATUS_CLOSED)).isFalse();
    var closedDiffs = dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), alreadyClosed.getKey());
    assertThat(closedDiffs).hasSize(1);
    assertThat(closedDiffs.get(0).diffs()).doesNotContainKey("status");
  }

  @Test
  public void repair_shouldMoveSafeRowFromClosedToResolvedKeepingWontFix() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    IssueDto safe = insertPreviouslyMigrated(rule, branch, db.components().insertComponent(newFileDto(branch)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.totalMigrated()).isZero();
    assertThat(result.totalRepaired()).isEqualTo(1);
    assertThat(result.projects()).extracting(ProjectMigrationResult::projectKey, ProjectMigrationResult::migrated, ProjectMigrationResult::repaired)
      .containsExactly(tuple(project.getProjectDto().getKey(), 0, 1));
    IssueDto repaired = reload(safe);
    assertThat(repaired.getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(repaired.getResolution()).isEqualTo(Issue.RESOLUTION_WONT_FIX);
    assertThat(IssueStatus.of(repaired.getStatus(), repaired.getResolution())).isEqualTo(IssueStatus.ACCEPTED);
    assertThat(repaired.getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(repaired.getTags()).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    assertThat(hasStatusChange(safe, Issue.STATUS_CLOSED, Issue.STATUS_RESOLVED)).isTrue();
    assertThat(postProcessor.wasCalled()).isTrue();
  }

  @Test
  public void repair_shouldMoveFixedRowWithMigrationStatusDiffToResolvedWontFixAndComment() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto fixed = insertPreviouslyMigrated(rule, branch, db.components().insertComponent(newFileDto(branch)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.totalRepaired()).isEqualTo(1);
    IssueDto repaired = reload(fixed);
    assertThat(repaired.getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(repaired.getResolution()).isEqualTo(Issue.RESOLUTION_WONT_FIX);
    assertThat(IssueStatus.of(repaired.getStatus(), repaired.getResolution())).isEqualTo(IssueStatus.ACCEPTED);
    // The migration's own entry is kept and the repair appends one more, with both the status and the resolution change.
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), fixed.getKey())).hasSize(2);
    assertThat(hasStatusChange(fixed, Issue.STATUS_REVIEWED, Issue.STATUS_CLOSED)).isTrue();
    assertThat(hasStatusChange(fixed, Issue.STATUS_CLOSED, Issue.STATUS_RESOLVED)).isTrue();
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), fixed.getKey()))
      .anyMatch(diffs -> diffs.get("resolution") != null
        && Issue.RESOLUTION_FIXED.equals(diffs.get("resolution").oldValue()) && Issue.RESOLUTION_WONT_FIX.equals(diffs.get("resolution").newValue()));
    assertThat(commentsOf(fixed)).containsExactly(HotspotsToIssuesMigrator.MIGRATED_FROM_REVIEWED_FIXED_COMMENT);
    assertThat(repaired.getTags()).containsExactlyInAnyOrder(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
  }

  @Test
  public void repair_shouldKeepTheOtherTagsOfAFixedRow() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto fixed = insertPreviouslyMigrated(rule, branch, db.components().insertComponent(newFileDto(branch)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG, "cwe", "owasp-a1");

    underTest.migrate(null, false);

    assertThat(reload(fixed).getTags()).containsExactlyInAnyOrder("cwe", "owasp-a1", HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
  }

  @Test
  public void repair_shouldNotAddTheReviewedFixedCommentOrTagToARepairedSafeRow() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto safe = insertPreviouslyMigrated(rule, branch, db.components().insertComponent(newFileDto(branch)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    underTest.migrate(null, false);

    assertThat(commentsOf(safe)).isEmpty();
    assertThat(reload(safe).getTags()).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
  }

  @Test
  public void repair_shouldRepairEveryTargetType() {
    logInAdmin();
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    List<IssueDto> rows = new ArrayList<>();
    for (RuleType targetType : List.of(RuleType.VULNERABILITY, RuleType.CODE_SMELL, RuleType.BUG)) {
      RuleDto rule = db.rules().insert(r -> r.setType(targetType));
      rows.add(insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false,
        HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG));
      rows.add(insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true,
        HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG));
    }

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.totalRepaired()).isEqualTo(6);
    assertThat(rows).hasSize(6).allSatisfy(row -> {
      IssueDto repaired = reload(row);
      assertThat(repaired.getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
      assertThat(repaired.getResolution()).isEqualTo(Issue.RESOLUTION_WONT_FIX);
      assertThat(IssueStatus.of(repaired.getStatus(), repaired.getResolution())).isEqualTo(IssueStatus.ACCEPTED);
      assertThat(repaired.getType()).isEqualTo(row.getType());
    });
    // rows alternate Safe, Fixed per target type: only the Fixed ones carry the reviewed-fixed tag
    for (int i = 0; i < rows.size(); i++) {
      assertThat(reload(rows.get(i)).getTags().contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG)).as("row %s", i).isEqualTo(i % 2 == 1);
    }
  }

  @Test
  public void repair_shouldLeaveEveryOtherRowUntouched() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    String tag = HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG;
    IssueDto eligibleSafe = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, tag);
    // Already closed before the migration (code removed): no migration status diff, must stay CLOSED.
    IssueDto closedFixedWithoutDiff = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, false, tag);
    IssueDto closedRemoved = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_REMOVED, true, tag);
    // Closed for another reason.
    IssueDto closedFalsePositive = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_FALSE_POSITIVE, true, tag);
    // Not tagged by the migration, or only a look-alike of its tag.
    IssueDto untagged = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, "other");
    IssueDto lookAlikeTag = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, "former-hotspot-x");
    // The new tag alone does not make a finding a migrated one: only the exact former-hotspot tag does.
    IssueDto onlyReviewedFixedTag = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
    // Statuses that issue tracking already follows.
    IssueDto open = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_OPEN, null, false, tag);
    IssueDto confirmed = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CONFIRMED, null, false, tag);
    IssueDto resolvedWontFix = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, false, tag);
    IssueDto resolvedFixed = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_RESOLVED, Issue.RESOLUTION_FIXED, true, tag);
    List<IssueDto> untouched = List.of(closedFixedWithoutDiff, closedRemoved, closedFalsePositive, untagged, lookAlikeTag, onlyReviewedFixedTag, open,
      confirmed, resolvedWontFix, resolvedFixed);

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.totalRepaired()).isEqualTo(1);
    assertThat(reload(eligibleSafe).getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(untouched).hasSize(10).allSatisfy(row -> {
      IssueDto reloaded = reload(row);
      assertThat(reloaded.getStatus()).isEqualTo(row.getStatus());
      assertThat(reloaded.getResolution()).isEqualTo(row.getResolution());
    });
  }

  @Test
  public void repair_shouldNotTouchPlainIssuesThatWereNeverHotspots() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto closedWontFix = db.issues().insert(rule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setType(RuleType.VULNERABILITY).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_WONT_FIX).setTags(List.of()));

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.totalRepaired()).isZero();
    assertThat(reload(closedWontFix).getStatus()).isEqualTo(Issue.STATUS_CLOSED);
  }

  @Test
  public void repair_dryRun_shouldCountWithoutWriting() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto safe = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    IssueDto fixed = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    MigrationResult result = underTest.migrate(null, true);

    assertThat(result.dryRun()).isTrue();
    assertThat(result.totalRepaired()).isEqualTo(2);
    assertThat(reload(safe).getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(reload(fixed).getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(hasStatusChange(safe, Issue.STATUS_CLOSED, Issue.STATUS_RESOLVED)).isFalse();
    assertThat(postProcessor.wasCalled()).isFalse();
  }

  @Test
  public void repair_shouldBeIdempotent() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto safe = insertPreviouslyMigrated(rule, branch, db.components().insertComponent(newFileDto(branch)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    assertThat(underTest.migrate(null, false).totalRepaired()).isEqualTo(1);
    MigrationResult secondRun = underTest.migrate(null, false);

    assertThat(secondRun.totalRepaired()).isZero();
    assertThat(secondRun.projects()).isEmpty();
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), safe.getKey())).hasSize(1);
  }

  @Test
  public void repair_shouldBeScopedToTheRequestedProject() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData projectA = db.components().insertPrivateProject();
    ComponentDto branchA = projectA.getMainBranchComponent();
    IssueDto inA = insertPreviouslyMigrated(rule, branchA, db.components().insertComponent(newFileDto(branchA)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    ComponentDto branchB = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto inB = insertPreviouslyMigrated(rule, branchB, db.components().insertComponent(newFileDto(branchB)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    MigrationResult result = underTest.migrate(projectA.getProjectDto().getKey(), false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::projectKey, ProjectMigrationResult::repaired)
      .containsExactly(tuple(projectA.getProjectDto().getKey(), 1));
    assertThat(reload(inA).getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(reload(inB).getStatus()).isEqualTo(Issue.STATUS_CLOSED);
  }

  @Test
  public void repair_shouldRepairEveryRowAcrossBranchesAndChunksInSingleCall() {
    logInAdmin();
    underTest.setPageSize(2);
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto mainBranch = project.getMainBranchComponent();
    ComponentDto mainFile = db.components().insertComponent(newFileDto(mainBranch));
    ComponentDto featureBranch = db.components().insertProjectBranch(mainBranch);
    ComponentDto featureFile = db.components().insertComponent(newFileDto(featureBranch));
    List<IssueDto> rows = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      rows.add(insertPreviouslyMigrated(rule, mainBranch, mainFile, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false,
        HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG));
      rows.add(insertPreviouslyMigrated(rule, featureBranch, featureFile, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true,
        HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG));
    }

    MigrationResult result = underTest.migrate(project.getProjectDto().getKey(), false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::repaired).containsExactly(6);
    assertThat(rows).hasSize(6).allSatisfy(row -> {
      IssueDto repaired = reload(row);
      assertThat(repaired.getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
      assertThat(repaired.getResolution()).isEqualTo(Issue.RESOLUTION_WONT_FIX);
    });
  }

  @Test
  public void repair_shouldBeIdempotentForAFixedRowAndNotRepeatTheComment() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    IssueDto fixed = insertPreviouslyMigrated(rule, branch, db.components().insertComponent(newFileDto(branch)),
      Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    assertThat(underTest.migrate(null, false).totalRepaired()).isEqualTo(1);
    MigrationResult secondRun = underTest.migrate(null, false);

    assertThat(secondRun.totalRepaired()).isZero();
    assertThat(commentsOf(fixed)).hasSize(1);
    assertThat(reload(fixed).getTags()).containsExactlyInAnyOrder(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), fixed.getKey())).hasSize(2);
  }

  @Test
  public void migrate_shouldMigrateAndRepairInTheSameRun() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto stillHotspot = insertHotspot(rule, branch, file,
      i -> i.setStatus(Issue.STATUS_REVIEWED).setResolution(Issue.RESOLUTION_SAFE));
    IssueDto migratedEarlier = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, false,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);

    MigrationResult result = underTest.migrate(null, false);

    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated, ProjectMigrationResult::repaired)
      .containsExactly(tuple(1, 1));
    assertThat(result.totalMigrated()).isEqualTo(1);
    assertThat(result.totalRepaired()).isEqualTo(1);
    // Migrated now: stored as RESOLVED straight away, so it is not "repaired" by the same run.
    assertThat(reload(stillHotspot).getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
    assertThat(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), stillHotspot.getKey())).hasSize(1);
    assertThat(reload(migratedEarlier).getStatus()).isEqualTo(Issue.STATUS_RESOLVED);
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

    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated).containsExactly(4);
    assertThat(result.skipped()).isZero();
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

    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated).containsExactly(5);
    assertThat(result.skipped()).isZero();
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
    assertThat(result.projects()).extracting(ProjectMigrationResult::migrated).containsExactly(1);
    assertThat(result.skipped()).isZero();
    IssueDto migrated = reload(closedHotspot);
    assertThat(migrated.getType()).isEqualTo(RuleType.VULNERABILITY.getDbConstant());
    assertThat(migrated.getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(migrated.getResolution()).isEqualTo(Issue.RESOLUTION_REMOVED);
    assertThat(migrated.getTags()).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    // Valid case -> no "Unexpected hotspot status" warning.
    assertThat(logTester.logs(Level.WARN)).isEmpty();
  }

  @Test
  public void migrate_shouldCountButNotLoadHotspotsWhoseRuleIsStillHotspot() {
    logInAdmin();
    RuleDto stillHotspotRule = db.rules().insertHotspotRule();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    IssueDto hotspot = insertHotspot(stillHotspotRule, branch, db.components().insertComponent(newFileDto(branch)), i -> {});

    MigrationResult result = underTest.migrate(null, false);

    // Filtered out in SQL, so the run never loads it and cannot attribute it to a project - it is reported as a
    // scope-wide skipped count instead, and the project contributes no result at all.
    assertThat(result.skipped()).isEqualTo(1);
    assertThat(result.projects()).isEmpty();
    assertThat(reload(hotspot).getType()).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
  }

  @Test
  public void migrate_whenRuleBecomesHotspotBetweenKeyQueryAndLoad_shouldSkipItWithWarning() {
    logInAdmin();
    logTester.setLevel(Level.WARN);
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    IssueDto hotspot = insertHotspot(rule, branch, db.components().insertComponent(newFileDto(branch)), i -> {});

    // The key query and the by-keys load run in separate sessions, so a rule can change type between them - which
    // the SQL filter cannot catch. That is the only path left to the guard in MigrationRun.accept, and without it a
    // finding would be given SECURITY_HOTSPOT as its "target" type. Reproduce the race by flipping the rule back to
    // a hotspot exactly as the load runs.
    IssueDao realDao = dbClient.issueDao();
    IssueDao racingDao = spy(realDao);
    doAnswer(invocation -> {
      db.executeUpdateSql("update rules set rule_type = 4 where uuid = '" + rule.getUuid() + "'");
      return realDao.selectHotspotsForMigrationByKeys(invocation.getArgument(0), invocation.getArgument(1));
    }).when(racingDao).selectHotspotsForMigrationByKeys(any(), any());
    DbClient racingClient = spy(dbClient);
    when(racingClient.issueDao()).thenReturn(racingDao);

    MigrationResult result = new HotspotsToIssuesMigrator(racingClient, issueFieldsSetter, batchWriter, system2,
      userSession).migrate(null, false);

    assertThat(result.projects()).isEmpty();
    assertThat(result.skipped()).isEqualTo(1);
    assertThat(reload(hotspot).getType()).isEqualTo(RuleType.SECURITY_HOTSPOT.getDbConstant());
    assertThat(logTester.logs(Level.WARN)).anyMatch(l -> l.contains("must have changed type mid-run"));
  }

  @Test
  public void migrate_shouldLeaveNothingRemainingWhenSomeRulesAreNotConverted() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    RuleDto stillHotspotRule = db.rules().insertHotspotRule();
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    insertHotspot(vulnerabilityRule, branch, file, i -> {});
    insertHotspot(stillHotspotRule, branch, file, i -> {});

    MigrationResult result = underTest.migrate(null, false);

    // Regression guard for SONAR-32194: the remaining count used to include findings the migrator permanently
    // skips, so it never reached zero and "re-run until it reaches zero" looped forever.
    assertThat(result.skipped()).isEqualTo(1);
    assertThat(db.getDbClient().issueDao().countHotspotsForMigration(db.getSession(), null)).isZero();
  }

  @Test
  public void migrate_shouldRefreshIssueStatsUsedBySecurityReports() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    insertHotspot(vulnerabilityRule, branch, file, i -> {});
    insertHotspot(vulnerabilityRule, branch, file, i -> {});
    issueStatsIndexer.indexOnAnalysis(branch.branchUuid());
    assertThat(issueStats(branch)).extracting(IssueStats::issueCount, IssueStats::hotspotCount)
      .containsExactly(tuple(0, 2));

    underTest.migrate(null, false);

    assertThat(issueStats(branch))
      .extracting(IssueStats::ruleKey, IssueStats::issueCount, IssueStats::hotspotCount, IssueStats::hotspotsReviewed)
      .containsExactly(tuple(vulnerabilityRule.getKey().toString(), 2, 0, 0));
  }

  @Test
  public void migrate_shouldRefreshIssueStatsOfEveryMigratedBranch() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto mainBranch = project.getMainBranchComponent();
    ComponentDto featureBranch = db.components().insertProjectBranch(mainBranch);
    insertHotspot(vulnerabilityRule, mainBranch, db.components().insertComponent(newFileDto(mainBranch)), i -> {});
    insertHotspot(vulnerabilityRule, featureBranch, db.components().insertComponent(newFileDto(featureBranch)), i -> {});
    issueStatsIndexer.indexOnAnalysis(mainBranch.branchUuid());
    issueStatsIndexer.indexOnAnalysis(featureBranch.branchUuid());

    underTest.migrate(project.getProjectDto().getKey(), false);

    assertThat(issueStats(mainBranch)).extracting(IssueStats::issueCount, IssueStats::hotspotCount)
      .containsExactly(tuple(1, 0));
    assertThat(issueStats(featureBranch)).extracting(IssueStats::issueCount, IssueStats::hotspotCount)
      .containsExactly(tuple(1, 0));
  }

  @Test
  public void migrate_dryRun_shouldLeaveIssueStatsUntouched() {
    logInAdmin();
    RuleDto vulnerabilityRule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    insertHotspot(vulnerabilityRule, branch, db.components().insertComponent(newFileDto(branch)), i -> {});
    issueStatsIndexer.indexOnAnalysis(branch.branchUuid());

    underTest.migrate(null, true);

    assertThat(issueStats(branch)).extracting(IssueStats::issueCount, IssueStats::hotspotCount)
      .containsExactly(tuple(0, 1));
  }

  @Test
  public void migrate_whenProjectKeyUnknown_shouldThrowNotFound() {
    logInAdmin();

    assertThatThrownBy(() -> underTest.migrate("does-not-exist", false))
      .isInstanceOf(NotFoundException.class)
      .hasMessageContaining("does-not-exist");
  }

  /**
   * The status mapping is independent of the target type, so it is checked for a Vulnerability, a Code Smell and a Bug
   * rule at once. {@code expectedIssueStatus} is what the issue model derives from the stored status/resolution.
   */
  private void assertStatusMapping(String hotspotStatus, @Nullable String hotspotResolution, String expectedStatus,
    @Nullable String expectedResolution, IssueStatus expectedIssueStatus) {
    logInAdmin();
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    Map<RuleType, IssueDto> hotspotsByTargetType = new EnumMap<>(RuleType.class);
    for (RuleType targetType : List.of(RuleType.VULNERABILITY, RuleType.CODE_SMELL, RuleType.BUG)) {
      RuleDto rule = db.rules().insert(r -> r.setType(targetType));
      hotspotsByTargetType.put(targetType, insertHotspot(rule, branch, file,
        i -> i.setStatus(hotspotStatus).setResolution(hotspotResolution)));
    }

    underTest.migrate(null, false);

    hotspotsByTargetType.forEach((targetType, hotspot) -> {
      IssueDto migrated = reload(hotspot);
      assertThat(migrated.getType()).as("type for %s", targetType).isEqualTo(targetType.getDbConstant());
      assertThat(migrated.getStatus()).as("status for %s", targetType).isEqualTo(expectedStatus);
      assertThat(migrated.getResolution()).as("resolution for %s", targetType).isEqualTo(expectedResolution);
      assertThat(IssueStatus.of(migrated.getStatus(), migrated.getResolution())).as("issue status for %s", targetType)
        .isEqualTo(expectedIssueStatus);
      assertThat(migrated.getTags()).contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
      boolean reviewedAsFixed = Issue.STATUS_REVIEWED.equals(hotspotStatus) && Issue.RESOLUTION_FIXED.equals(hotspotResolution);
      if (reviewedAsFixed) {
        assertThat(migrated.getTags()).as("tags for %s", targetType)
          .containsExactlyInAnyOrder(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG, HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG);
      } else {
        assertThat(migrated.getTags()).as("tags for %s", targetType).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
      }
    });
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

  private List<IssueStats> issueStats(ComponentDto branch) {
    return issueStatsByRuleKeyDao.getIssueStats(branch.branchUuid(), AggregationType.PROJECT);
  }

  private IssueDto reload(IssueDto issue) {
    // The test session is shared: without this a row read before the migration would be served from its cache.
    db.getSession().clearCache();
    return dbClient.issueDao().selectOrFailByKey(db.getSession(), issue.getKey());
  }

  /**
   * A finding as an earlier version of the migration left it: already converted to the rule's type and tagged, but
   * stored as CLOSED. {@code withMigrationStatusDiff} adds the changelog entry that migration wrote when it changed a
   * Reviewed hotspot to CLOSED (REVIEWED -> CLOSED), which is what tells such a row from a hotspot that was already
   * closed before the migration.
   */
  private IssueDto insertPreviouslyMigrated(RuleDto rule, ComponentDto branch, ComponentDto file, String status,
    @Nullable String resolution, boolean withMigrationStatusDiff, String... tags) {
    IssueDto issue = db.issues().insert(rule, branch, file,
      i -> i.setType(rule.getEnumType()).setStatus(status).setResolution(resolution).setTags(List.of(tags)));
    if (withMigrationStatusDiff) {
      db.issues().insertFieldDiffs(issue, new FieldDiffs()
        .setIssueKey(issue.getKey())
        .setCreationDate(new Date(system2.now() - 1_000))
        .setDiff("type", "SECURITY_HOTSPOT", rule.getEnumType().name())
        .setDiff("status", Issue.STATUS_REVIEWED, Issue.STATUS_CLOSED)
        .setDiff("tags", null, HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG));
    }
    return issue;
  }

  /**
   * Found on a real server: once a repaired row was closed again by an analysis, every later run repaired it again,
   * repeating the comment and tagging a Safe row as a Fixed one.
   */
  @Test
  public void repair_shouldNotRepairARowAgainAfterAnAnalysisClosedIt() {
    logInAdmin();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    ComponentDto file = db.components().insertComponent(newFileDto(branch));
    IssueDto fixed = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_FIXED, true,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    IssueDto safe = insertPreviouslyMigrated(rule, branch, file, Issue.STATUS_CLOSED, Issue.RESOLUTION_WONT_FIX, true,
      HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    assertThat(underTest.migrate(null, false).totalRepaired()).isEqualTo(2);

    // What the next analysis does to both when they are no longer matched: closes them, as FIXED, with its own change.
    for (IssueDto repaired : List.of(fixed, safe)) {
      db.getSession().clearCache();
      IssueDto dto = dbClient.issueDao().selectOrFailByKey(db.getSession(), repaired.getKey());
      dto.setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED);
      dbClient.issueDao().update(db.getSession(), dto);
      db.issues().insertFieldDiffs(repaired, new FieldDiffs().setIssueKey(repaired.getKey()).setCreationDate(new Date(system2.now() + 1_000))
        .setDiff("status", Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED)
        .setDiff("resolution", Issue.RESOLUTION_WONT_FIX, Issue.RESOLUTION_FIXED));
    }
    db.commit();

    MigrationResult second = underTest.migrate(null, false);

    assertThat(second.totalRepaired()).isZero();
    assertThat(reload(fixed).getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(reload(safe).getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(commentsOf(fixed)).containsExactly(HotspotsToIssuesMigrator.MIGRATED_FROM_REVIEWED_FIXED_COMMENT);
    assertThat(commentsOf(safe)).isEmpty();
    assertThat(reload(safe).getTags()).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
  }

  @Test
  public void migrate_shouldNotRepairAReviewedHotspotTheAnalyzerClosedBeforeTheMigration() {
    ProjectData project = db.components().insertPrivateProject();
    ComponentDto branch = project.getMainBranchComponent();
    RuleDto rule = db.rules().insert(r -> r.setType(RuleType.VULNERABILITY));
    IssueDto closed = db.issues().insert(rule, branch, db.components().insertComponent(newFileDto(branch)),
      i -> i.setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED).setResolution(Issue.RESOLUTION_FIXED).setTags(List.of()));
    db.issues().insertFieldDiffs(closed, new FieldDiffs().setIssueKey(closed.getKey()).setCreationDate(new Date(system2.now() - 100_000))
      .setDiff("status", Issue.STATUS_REVIEWED, Issue.STATUS_CLOSED)
      .setDiff("resolution", Issue.RESOLUTION_SAFE, Issue.RESOLUTION_FIXED));

    MigrationResult first = underTest.migrate(null, false);
    MigrationResult second = underTest.migrate(null, false);

    assertThat(first.totalMigrated()).isEqualTo(1);
    assertThat(first.totalRepaired()).isZero();
    assertThat(second.totalRepaired()).isZero();
    IssueDto reloaded = reload(closed);
    assertThat(reloaded.getStatus()).isEqualTo(Issue.STATUS_CLOSED);
    assertThat(reloaded.getResolution()).isEqualTo(Issue.RESOLUTION_FIXED);
    assertThat(reloaded.getTags()).containsExactly(HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG);
    assertThat(commentsOf(closed)).isEmpty();
  }

  private List<String> commentsOf(IssueDto issue) {
    db.getSession().clearCache();
    return dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), List.of(issue.getKey()), IssueChangeDto.TYPE_COMMENT).stream()
      .map(IssueChangeDto::getChangeData)
      .toList();
  }

  private boolean hasStatusChange(IssueDto issue, String from, String to) {
    return dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), issue.getKey()).stream()
      .map(diffs -> diffs.get("status"))
      .anyMatch(diff -> diff != null && from.equals(diff.oldValue()) && to.equals(diff.newValue()));
  }
}