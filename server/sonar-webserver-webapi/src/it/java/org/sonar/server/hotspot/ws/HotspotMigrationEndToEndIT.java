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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.tracking.BlockHashSequence;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.issue.tracking.LineHashSequence;
import org.sonar.core.issue.tracking.NonClosedTracking;
import org.sonar.core.issue.tracking.Tracker;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.SequenceUuidFactory;
import org.sonar.db.DbClient;
import org.sonar.db.DbTester;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.issue.IssueDto;
import org.sonar.db.report.IssueStatsByRuleKeyDaoImpl;
import org.sonar.db.rule.RuleDto;
import org.sonar.server.hotspot.ws.HotspotsToIssuesMigrator.MigrationResult;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.IssueStatsIndexer;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.TestIssueChangePostProcessor;
import org.sonar.server.issue.WebIssueStorage;
import org.sonar.server.issue.index.IssueIndexer;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflow;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowActionsFactory;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowDefinition;
import org.sonar.server.rule.DefaultRuleFinder;
import org.sonar.server.rule.RuleDescriptionFormatter;
import org.sonar.server.tester.UserSessionRule;
import org.sonarsource.compliancereports.ingestion.IssueIngestionService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.db.component.ComponentTesting.newFileDto;

/**
 * What a user sees after the hotspot migration, end to end: the real migrator and repair write the rows, then the real
 * issue tracker (rule, line, line hash, message) and the real issue workflow decide what the next analysis does with
 * them. The unit-level tests pin each piece; these pin the chain (MMF-5734, SONAR-32693, USER-2694).
 */
@SuppressWarnings("deprecation") // legacy STATUS_*/RESOLUTION_* constants are the persisted column values
public class HotspotMigrationEndToEndIT {

  private static final String TAG = HotspotsToIssuesMigrator.FORMER_HOTSPOT_TAG;
  private static final List<RuleType> TARGET_TYPES = List.of(RuleType.VULNERABILITY, RuleType.CODE_SMELL, RuleType.BUG);
  private static final int FIRST_LINE = 5;

  private final System2 system2 = new TestSystem2().setNow(1_500_000_000_000L);

  @Rule
  public DbTester db = DbTester.create(system2);
  @Rule
  public UserSessionRule userSession = UserSessionRule.standalone();

  private final DbClient dbClient = db.getDbClient();
  private final SequenceUuidFactory uuidFactory = new SequenceUuidFactory();
  private final IssueIndexer issueIndexer = mock(IssueIndexer.class);
  private final WebIssueStorage issueStorage = new WebIssueStorage(system2, dbClient,
    new DefaultRuleFinder(dbClient, mock(RuleDescriptionFormatter.class)), issueIndexer, uuidFactory);
  private final IssueStatsByRuleKeyDaoImpl issueStatsByRuleKeyDao = new IssueStatsByRuleKeyDaoImpl(dbClient);
  private final IssueStatsIndexer issueStatsIndexer = new IssueStatsIndexer(dbClient,
    new IssueIngestionService(issueStatsByRuleKeyDao), issueStatsByRuleKeyDao);
  private final MigrationBatchWriter batchWriter = new MigrationBatchWriter(dbClient, issueStorage, new TestIssueChangePostProcessor(),
    issueIndexer, issueStatsIndexer, uuidFactory, system2);
  private final HotspotsToIssuesMigrator underTest = new HotspotsToIssuesMigrator(dbClient, new IssueFieldsSetter(), batchWriter, system2, userSession);
  private final IssueWorkflow workflow = new IssueWorkflow(new CodeQualityIssueWorkflow(new CodeQualityIssueWorkflowActionsFactory(new IssueFieldsSetter()),
    new CodeQualityIssueWorkflowDefinition(), mock(TaintChecker.class)), null);

  @Before
  public void setUp() {
    when(issueIndexer.enqueueForIndexing(any(), any())).thenReturn(List.of());
    userSession.logIn().setSystemAdministrator();
  }

  // --- 1. migrate, then re-analyse unchanged code: 0 new and 0 closed, for every mapping and target type

  @Test
  public void afterMigration_reanalysingUnchangedCode_shouldMatchEveryFindingForEveryMappingAndTargetType() {
    for (int linesAddedAbove : new int[] {0, 3}) {
      Fixture fixture = new Fixture();
      List<Finding> findings = new ArrayList<>();
      for (RuleType targetType : TARGET_TYPES) {
        RuleDto rule = db.rules().insert(r -> r.setType(targetType));
        findings.add(fixture.hotspot(rule, Issue.STATUS_TO_REVIEW, null));
        findings.add(fixture.hotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED));
        findings.add(fixture.hotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE));
        findings.add(fixture.hotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED));
      }

      assertThat(underTest.migrate(null, false).totalMigrated()).isEqualTo(findings.size());

      Analysis analysis = fixture.analyse(findings, linesAddedAbove);
      assertThat(analysis.newIssues()).as("new issues, %s lines added above", linesAddedAbove).isEmpty();
      assertThat(analysis.closedIssues()).as("closed issues, %s lines added above", linesAddedAbove).isEmpty();
      assertThat(findings).allSatisfy(finding -> assertThat(analysis.baseOf(finding)).isNotNull());
    }
  }

  // --- 2. reintroduced code of hotspots closed before the migration is reported, with the mapped status

  @Test
  public void afterMigration_hotspotsClosedBeforeMigration_shouldBeRestoredWithTheMappedStatusWhenTheirCodeComesBack() {
    record Case(String statusBefore, String resolutionBefore, String expectedStatus, String expectedResolution, IssueStatus expectedIssueStatus) {
    }
    List<Case> cases = List.of(
      new Case(Issue.STATUS_TO_REVIEW, null, Issue.STATUS_OPEN, null, IssueStatus.OPEN),
      new Case(Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED, Issue.STATUS_CONFIRMED, null, IssueStatus.CONFIRMED),
      new Case(Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED),
      new Case(Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED, Issue.STATUS_RESOLVED, Issue.RESOLUTION_WONT_FIX, IssueStatus.ACCEPTED));
    Fixture fixture = new Fixture();
    Map<Finding, Case> closed = new LinkedHashMap<>();
    for (RuleType targetType : TARGET_TYPES) {
      RuleDto rule = db.rules().insert(r -> r.setType(targetType));
      for (Case c : cases) {
        for (String closeResolution : List.of(Issue.RESOLUTION_REMOVED, Issue.RESOLUTION_FIXED)) {
          closed.put(fixture.closedHotspot(rule, c.statusBefore(), c.resolutionBefore(), closeResolution), c);
        }
      }
    }

    underTest.migrate(null, false);

    // As an analysis does: the code is back, the workflow runs on the closed issue with its full changelog.
    closed.forEach((finding, c) -> {
      DefaultIssue issue = fixture.withChangelog(finding);
      assertThat(issue.status()).isEqualTo(Issue.STATUS_CLOSED);

      workflow.doAutomaticTransition(issue, IssueChangeContext.issueChangeContextByScanBuilder(new Date(system2.now())).build());

      assertThat(issue.status()).as("status of %s closed from %s/%s", finding.type, c.statusBefore(), c.resolutionBefore()).isEqualTo(c.expectedStatus());
      assertThat(issue.resolution()).isEqualTo(c.expectedResolution());
      assertThat(issue.issueStatus()).isEqualTo(c.expectedIssueStatus());
      // A restored Reviewed/Fixed finding is tagged and commented like a migrated or repaired one, so that listing the
      // reviewed-fixed tag finds it; no other restored finding is.
      boolean reviewedAsFixed = Issue.RESOLUTION_FIXED.equals(c.resolutionBefore());
      assertThat(issue.tags().contains(HotspotsToIssuesMigrator.FORMER_HOTSPOT_REVIEWED_FIXED_TAG)).as("reviewed-fixed tag of %s", finding.key)
        .isEqualTo(reviewedAsFixed);
      assertThat(issue.defaultIssueComments()).extracting(c2 -> c2.markdownText())
        .isEqualTo(reviewedAsFixed ? List.of(HotspotsToIssuesMigrator.MIGRATED_FROM_REVIEWED_FIXED_COMMENT) : List.of());
    });
  }

  // --- 3. repair of instances migrated by 2026.5.0-5.2

  @Test
  public void repair_withoutDuplicate_shouldKeepTheReviewOnTheNextAnalysis_forSafeAndFixed_withAndWithoutLinesAddedAbove() {
    for (int linesAddedAbove : new int[] {0, 3}) {
      Fixture fixture = new Fixture();
      List<Finding> findings = new ArrayList<>();
      List<Finding> closedBeforeMigration = new ArrayList<>();
      for (RuleType targetType : TARGET_TYPES) {
        RuleDto rule = db.rules().insert(r -> r.setType(targetType));
        findings.add(fixture.brokenSafe(rule));
        findings.add(fixture.brokenFixed(rule));
        closedBeforeMigration.add(fixture.closedHotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE, Issue.RESOLUTION_REMOVED));
      }

      MigrationResult result = underTest.migrate(null, false);

      assertThat(result.totalRepaired()).as("repaired, %s lines added above", linesAddedAbove).isEqualTo(findings.size());
      assertThat(findings).allSatisfy(f -> {
        IssueDto repaired = fixture.reload(f);
        assertThat(IssueStatus.of(repaired.getStatus(), repaired.getResolution())).isEqualTo(IssueStatus.ACCEPTED);
      });
      assertThat(closedBeforeMigration).allSatisfy(f -> assertThat(fixture.reload(f).getStatus()).isEqualTo(Issue.STATUS_CLOSED));

      // one issue per finding, carrying the review: every repaired row is matched, nothing new, nothing closed
      Analysis analysis = fixture.analyse(findings, linesAddedAbove);
      assertThat(analysis.newIssues()).isEmpty();
      assertThat(analysis.closedIssues()).isEmpty();
      assertThat(findings).allSatisfy(f -> assertThat(analysis.baseOf(f)).isNotNull());
    }
  }

  /**
   * What users see on an instance that already re-analysed after the buggy migration: each finding exists twice, the
   * Open duplicate the analysis created and the original row, which the repair turns into Accepted. The next analysis
   * tracks the duplicate (it is on the current line, the original kept the line it had), and the repaired row is
   * closed: the user's review is lost and the duplicate must be closed by hand. The repair does not close it, because
   * matching issues against each other on the server is not safe (MMF-5734).
   */
  @Test
  public void repair_withAnOpenDuplicateAlreadyCreated_shouldLeaveTheDuplicateTrackedAndCloseTheRepairedRow_forSafeAndFixed() {
    for (int linesAddedAbove : new int[] {3, 6}) {
      Fixture fixture = new Fixture();
      List<Finding> repaired = new ArrayList<>();
      List<Finding> duplicates = new ArrayList<>();
      for (RuleType targetType : TARGET_TYPES) {
        RuleDto rule = db.rules().insert(r -> r.setType(targetType));
        Finding safe = fixture.brokenSafe(rule);
        Finding fixed = fixture.brokenFixed(rule);
        repaired.add(safe);
        repaired.add(fixed);
        // created by the analysis that ran after the buggy migration, when 3 lines had been added above
        duplicates.add(fixture.openDuplicateOf(safe, 3));
        duplicates.add(fixture.openDuplicateOf(fixed, 3));
      }

      underTest.migrate(null, false);

      Analysis analysis = fixture.analyse(concat(repaired, duplicates), linesAddedAbove, 3);
      assertThat(analysis.newIssues()).as("new issues, %s lines added above", linesAddedAbove).isEmpty();
      assertThat(duplicates).allSatisfy(d -> assertThat(analysis.baseOf(d)).as("duplicate is tracked").isNotNull());
      assertThat(repaired).allSatisfy(r -> {
        assertThat(analysis.baseOf(r)).as("repaired row is no longer matched").isNull();
        assertThat(analysis.closedIssues()).extracting(DefaultIssue::key).contains(r.key);
      });
    }
  }

  // --- 4. idempotent

  @Test
  public void rerunningTheMigrationOnAnAlreadyMigratedAndRepairedInstance_shouldChangeNothing() {
    Fixture fixture = new Fixture();
    List<Finding> all = new ArrayList<>();
    for (RuleType targetType : TARGET_TYPES) {
      RuleDto rule = db.rules().insert(r -> r.setType(targetType));
      all.add(fixture.hotspot(rule, Issue.STATUS_TO_REVIEW, null));
      all.add(fixture.hotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_ACKNOWLEDGED));
      all.add(fixture.hotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE));
      all.add(fixture.hotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_FIXED));
      all.add(fixture.closedHotspot(rule, Issue.STATUS_REVIEWED, Issue.RESOLUTION_SAFE, Issue.RESOLUTION_REMOVED));
      all.add(fixture.brokenSafe(rule));
      all.add(fixture.brokenFixed(rule));
    }
    MigrationResult first = underTest.migrate(null, false);
    assertThat(first.totalMigrated()).isEqualTo(15);
    assertThat(first.totalRepaired()).isEqualTo(6);
    Map<String, String> before = snapshot(fixture, all);

    MigrationResult second = underTest.migrate(null, false);

    assertThat(second.totalMigrated()).isZero();
    assertThat(second.totalRepaired()).isZero();
    assertThat(second.projects()).isEmpty();
    assertThat(snapshot(fixture, all)).isEqualTo(before);
  }

  private Map<String, String> snapshot(Fixture fixture, List<Finding> findings) {
    Map<String, String> snapshot = new LinkedHashMap<>();
    for (Finding f : findings) {
      IssueDto issue = fixture.reload(f);
      snapshot.put(f.key, String.join("|", issue.getStatus(), String.valueOf(issue.getResolution()), String.valueOf(issue.getType()),
        issue.getTags().stream().sorted().toList().toString(), String.valueOf(issue.getUpdatedAt()),
        String.valueOf(dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), f.key).size()),
        String.valueOf(dbClient.issueChangeDao().selectByTypeAndIssueKeys(db.getSession(), List.of(f.key), "comment").size())));
    }
    return snapshot;
  }

  private static <T> List<T> concat(List<T> a, List<T> b) {
    List<T> all = new ArrayList<>(a);
    all.addAll(b);
    return all;
  }

  private record Finding(String key, RuleDto rule, RuleType type, int line, String lineHash, String message) {
  }

  private record Analysis(Map<Finding, DefaultIssue> baseByFinding, List<DefaultIssue> newIssues, List<DefaultIssue> closedIssues) {
    DefaultIssue baseOf(Finding finding) {
      return baseByFinding.get(finding);
    }
  }

  /**
   * One file of 40 lines and the findings on it. Line {@code n} of the original file has the hash {@code line-n}, and
   * lines added above it get their own hashes, so a hash identifies the code and the tracker can follow it.
   */
  private final class Fixture {
    private final ComponentDto branch = db.components().insertPrivateProject().getMainBranchComponent();
    private final ComponentDto file = db.components().insertComponent(newFileDto(branch));
    private int nextLine = FIRST_LINE;

    Finding hotspot(RuleDto rule, String status, @Nullable String resolution) {
      Finding finding = nextFinding(rule);
      IssueDto issue = db.issues().insert(rule, branch, file, i -> i.setType(RuleType.SECURITY_HOTSPOT).setStatus(status).setResolution(resolution)
        .setTags(List.of()).setLine(finding.line()).setChecksum(finding.lineHash()).setMessage(finding.message()));
      return new Finding(issue.getKey(), rule, rule.getEnumType(), finding.line(), finding.lineHash(), finding.message());
    }

    /** A hotspot the analyzer closed before the migration: it holds the changelog entry that closing wrote. */
    Finding closedHotspot(RuleDto rule, String statusBefore, @Nullable String resolutionBefore, String closeResolution) {
      Finding finding = nextFinding(rule);
      IssueDto issue = db.issues().insert(rule, branch, file, i -> i.setType(RuleType.SECURITY_HOTSPOT).setStatus(Issue.STATUS_CLOSED)
        .setResolution(closeResolution).setTags(List.of()).setLine(null).setChecksum(finding.lineHash()).setMessage(finding.message()));
      // One entry, as the analyzer writes it. It records a resolution diff only when the resolution changes: a hotspot
      // reviewed as Fixed and closed as FIXED has none.
      FieldDiffs closing = new FieldDiffs().setIssueKey(issue.getKey()).setCreationDate(new Date(system2.now() - 100_000))
        .setDiff("status", statusBefore, Issue.STATUS_CLOSED)
        .setDiff("line", String.valueOf(finding.line()), "");
      if (!closeResolution.equals(resolutionBefore)) {
        closing.setDiff("resolution", resolutionBefore == null ? "" : resolutionBefore, closeResolution);
      }
      db.issues().insertFieldDiffs(issue, closing);
      return new Finding(issue.getKey(), rule, rule.getEnumType(), finding.line(), finding.lineHash(), finding.message());
    }

    /** Reviewed/Safe as the buggy 2026.5.0-5.2 migration left it: CLOSED + WONTFIX. */
    Finding brokenSafe(RuleDto rule) {
      Finding finding = nextFinding(rule);
      IssueDto issue = migratedRow(rule, finding, Issue.RESOLUTION_WONT_FIX);
      return new Finding(issue.getKey(), rule, rule.getEnumType(), finding.line(), finding.lineHash(), finding.message());
    }

    /** Reviewed/Fixed as the buggy migration left it: CLOSED + FIXED, with its status diff REVIEWED to CLOSED. */
    Finding brokenFixed(RuleDto rule) {
      Finding finding = nextFinding(rule);
      IssueDto issue = migratedRow(rule, finding, Issue.RESOLUTION_FIXED);
      db.issues().insertFieldDiffs(issue, new FieldDiffs().setIssueKey(issue.getKey()).setCreationDate(new Date(system2.now() - 1_000))
        .setDiff("type", "SECURITY_HOTSPOT", rule.getEnumType().name())
        .setDiff("status", Issue.STATUS_REVIEWED, Issue.STATUS_CLOSED)
        .setDiff("tags", null, TAG));
      return new Finding(issue.getKey(), rule, rule.getEnumType(), finding.line(), finding.lineHash(), finding.message());
    }

    private IssueDto migratedRow(RuleDto rule, Finding finding, String resolution) {
      return db.issues().insert(rule, branch, file, i -> i.setType(rule.getEnumType()).setStatus(Issue.STATUS_CLOSED).setResolution(resolution)
        .setTags(List.of(TAG)).setLine(finding.line()).setChecksum(finding.lineHash()).setMessage(finding.message()));
    }

    /** The Open issue an analysis created for the same code after the migration, on the line it had by then. */
    Finding openDuplicateOf(Finding original, int linesAddedAbove) {
      int line = original.line() + linesAddedAbove;
      IssueDto issue = db.issues().insert(original.rule(), branch, file, i -> i.setType(original.type()).setStatus(Issue.STATUS_OPEN).setResolution(null)
        .setTags(List.of()).setLine(line).setChecksum(original.lineHash()).setMessage(original.message()));
      return new Finding(issue.getKey(), original.rule(), original.type(), line, original.lineHash(), original.message());
    }

    private Finding nextFinding(RuleDto rule) {
      int line = nextLine++;
      return new Finding(null, rule, rule.getEnumType(), line, "line-" + line, "message of line " + line);
    }

    IssueDto reload(Finding finding) {
      db.getSession().clearCache();
      return dbClient.issueDao().selectOrFailByKey(db.getSession(), finding.key);
    }

    DefaultIssue withChangelog(Finding finding) {
      DefaultIssue issue = reload(finding).toDefaultIssue();
      dbClient.issueChangeDao().selectChangelogByIssue(db.getSession(), finding.key).forEach(issue::addChange);
      return issue;
    }

    /**
     * The analysis of the file once {@code linesAddedAbove} lines were added above it. The base is what the database
     * holds, the raw issues are what the analyzer reports for the same findings, and the tracker matches them the way
     * the Compute Engine does: closed base issues are not offered, so they are not matched.
     */
    Analysis analyse(List<Finding> findings, int linesAddedAbove) {
      return analyse(findings, linesAddedAbove, 0);
    }

    /** {@code baseLinesAdded}: lines the file already had added when the base issues were last analysed. */
    Analysis analyse(List<Finding> findings, int linesAddedAbove, int baseLinesAdded) {
      List<DefaultIssue> bases = findings.stream().map(this::reloadBase).toList();
      // the findings the analyzer reports: one per distinct (rule, code) of the findings that exist on the file
      Map<String, Finding> reported = new LinkedHashMap<>();
      findings.forEach(f -> reported.putIfAbsent(f.rule().getKey() + "|" + f.lineHash(), f));
      List<DefaultIssue> raws = reported.values().stream().map(f -> raw(f, linesAddedAbove)).toList();

      LineHashSequence currentHashes = new LineHashSequence(fileHashes(linesAddedAbove));
      NonClosedTracking<DefaultIssue, DefaultIssue> tracking = new Tracker<DefaultIssue, DefaultIssue>()
        .trackNonClosed(input(raws, currentHashes), input(bases, new LineHashSequence(fileHashes(baseLinesAdded))));

      Map<Finding, DefaultIssue> matchedBases = new LinkedHashMap<>();
      for (Finding f : findings) {
        DefaultIssue base = bases.get(findings.indexOf(f));
        if (tracking.getMatchedRaws().containsValue(base)) {
          matchedBases.put(f, base);
        }
      }
      return new Analysis(matchedBases, tracking.getUnmatchedRaws().toList(), tracking.getUnmatchedBases().toList());
    }

    private DefaultIssue reloadBase(Finding finding) {
      return reload(finding).toDefaultIssue();
    }

    private DefaultIssue raw(Finding finding, int linesAddedAbove) {
      return new DefaultIssue().setRuleKey(finding.rule().getKey()).setLine(finding.line() + linesAddedAbove).setChecksum(finding.lineHash())
        .setMessage(finding.message());
    }

    private List<String> fileHashes(int linesAddedAbove) {
      List<String> hashes = new ArrayList<>();
      for (int i = 1; i <= linesAddedAbove; i++) {
        hashes.add("added-" + i);
      }
      for (int line = 1; line <= 60; line++) {
        hashes.add("line-" + line);
      }
      return hashes;
    }

    private Input<DefaultIssue> input(Collection<DefaultIssue> issues, LineHashSequence hashes) {
      return new Input<>() {
        @Override
        public LineHashSequence getLineHashSequence() {
          return hashes;
        }

        @Override
        public BlockHashSequence getBlockHashSequence() {
          return BlockHashSequence.create(hashes);
        }

        @Override
        public Collection<DefaultIssue> getIssues() {
          return issues;
        }
      };
    }
  }
}
