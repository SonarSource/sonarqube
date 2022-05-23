/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.Iterators;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.utils.Duration;
import org.sonar.ce.task.projectanalysis.batch.BatchReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ReportComponent;
import org.sonar.ce.task.projectanalysis.component.TreeRootHolderRule;
import org.sonar.ce.task.projectanalysis.issue.commonrule.CommonRuleEngine;
import org.sonar.ce.task.projectanalysis.issue.filter.IssueFilter;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRule;
import org.sonar.ce.task.projectanalysis.qualityprofile.ActiveRulesHolderRule;
import org.sonar.ce.task.projectanalysis.source.SourceLinesHashRepository;
import org.sonar.ce.task.projectanalysis.source.SourceLinesRepository;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.IssueType;
import org.sonar.scanner.protocol.output.ScannerReport.TextRange;
import org.sonar.server.rule.CommonRuleKeys;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;

@RunWith(DataProviderRunner.class)
public class TrackerRawInputFactoryTest {

  private static final String FILE_UUID = "fake_uuid";
  private static final String ANOTHER_FILE_UUID = "another_fake_uuid";
  private static final String EXAMPLE_LINE_OF_CODE_FORMAT = "int example = line + of + code + %d; ";
  private static final String LINE_IN_THE_MAIN_FILE = "String string = 'line-in-the-main-file';";
  private static final String LINE_IN_ANOTHER_FILE = "String string = 'line-in-the-another-file';";
  private static final int FILE_REF = 2;
  private static final int NOT_IN_REPORT_FILE_REF = 3;
  private static final int ANOTHER_FILE_REF = 4;

  @Rule
  public TreeRootHolderRule treeRootHolder = new TreeRootHolderRule().setRoot(PROJECT);
  @Rule
  public BatchReportReaderRule reportReader = new BatchReportReaderRule();
  @Rule
  public ActiveRulesHolderRule activeRulesHolder = new ActiveRulesHolderRule();
  @Rule
  public RuleRepositoryRule ruleRepository = new RuleRepositoryRule();

  private static final ReportComponent FILE = ReportComponent.builder(Component.Type.FILE, FILE_REF).setUuid(FILE_UUID).build();
  private static final ReportComponent ANOTHER_FILE = ReportComponent.builder(Component.Type.FILE, ANOTHER_FILE_REF).setUuid(ANOTHER_FILE_UUID).build();
  private static final ReportComponent PROJECT = ReportComponent.builder(Component.Type.PROJECT, 1).addChildren(FILE, ANOTHER_FILE).build();

  private final SourceLinesHashRepository sourceLinesHash = mock(SourceLinesHashRepository.class);
  private final SourceLinesRepository sourceLinesRepository = mock(SourceLinesRepository.class);
  private final CommonRuleEngine commonRuleEngine = mock(CommonRuleEngine.class);
  private final IssueFilter issueFilter = mock(IssueFilter.class);
  private final TrackerRawInputFactory underTest = new TrackerRawInputFactory(treeRootHolder, reportReader, sourceLinesHash,
    sourceLinesRepository, commonRuleEngine, issueFilter, ruleRepository, activeRulesHolder);

  @Before
  public void before() {
    Iterator<String> stringIterator = IntStream.rangeClosed(1, 9)
      .mapToObj(i -> String.format(EXAMPLE_LINE_OF_CODE_FORMAT, i))
      .iterator();
    when(sourceLinesRepository.readLines(any())).thenReturn(CloseableIterator.from(stringIterator));
  }

  @Test
  public void load_source_hash_sequences() {
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getLineHashSequence()).isNotNull();
    assertThat(input.getLineHashSequence().getHashForLine(1)).isEqualTo("line");
    assertThat(input.getLineHashSequence().getHashForLine(2)).isEmpty();
    assertThat(input.getLineHashSequence().getHashForLine(3)).isEmpty();

    assertThat(input.getBlockHashSequence()).isNotNull();
  }

  @Test
  public void load_source_hash_sequences_only_on_files() {
    Input<DefaultIssue> input = underTest.create(PROJECT);

    assertThat(input.getLineHashSequence()).isNotNull();
    assertThat(input.getBlockHashSequence()).isNotNull();
  }

  @Test
  public void load_issues_from_report() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .setGap(3.14)
      .setQuickFixAvailable(true)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = Iterators.getOnlyElement(issues.iterator());

    // fields set by analysis report
    assertThat(issue.ruleKey()).isEqualTo(ruleKey);
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.line()).isEqualTo(2);
    assertThat(issue.gap()).isEqualTo(3.14);
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.isQuickFixAvailable()).isTrue();

    // fields set by compute engine
    assertThat(issue.checksum()).isEqualTo(input.getLineHashSequence().getHashForLine(2));
    assertThat(issue.tags()).isEmpty();
    assertInitializedIssue(issue);
    assertThat(issue.effort()).isNull();

    assertLocationHashIsMadeOf(input, "intexample=line+of+code+2;");
  }

  @Test
  public void calculateLocationHash_givenIssueOn3Lines_calculateHashOn3Lines() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(TextRange.newBuilder()
        .setStartLine(1)
        .setEndLine(3)
        .setStartOffset(0)
        .setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)
        .build())
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertLocationHashIsMadeOf(input, "intexample=line+of+code+1;intexample=line+of+code+2;intexample=line+of+code+3;");
  }

  @Test
  public void calculateLocationHash_givenIssuePartiallyOn1Line_calculateHashOnAPartOfLine() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(TextRange.newBuilder()
        .setStartLine(1)
        .setEndLine(1)
        .setStartOffset(13)
        .setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)
        .build())
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertLocationHashIsMadeOf(input, "line+of+code+1;");
  }

  @Test
  public void calculateLocationHash_givenIssuePartiallyOn1LineAndPartiallyOnThirdLine_calculateHashAccordingly() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(TextRange.newBuilder()
        .setStartLine(1)
        .setEndLine(3)
        .setStartOffset(13)
        .setEndOffset(11)
        .build())
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertLocationHashIsMadeOf(input, "line+of+code+1;intexample=line+of+code+2;intexample");
  }

  @Test
  public void calculateLocationHash_givenIssueOn2Components_calculateHashesByReading2Files() {
    when(sourceLinesRepository.readLines(any())).thenReturn(
      newOneLineIterator(LINE_IN_THE_MAIN_FILE),
      newOneLineIterator(LINE_IN_THE_MAIN_FILE),
      newOneLineIterator(LINE_IN_ANOTHER_FILE));
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(1, LINE_IN_THE_MAIN_FILE.length()))
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .setGap(3.14)
      .addFlow(ScannerReport.Flow.newBuilder()
        .addLocation(ScannerReport.IssueLocation.newBuilder()
          .setComponentRef(FILE_REF)
          .setMsg("Secondary location in same file")
          .setTextRange(newTextRange(1, LINE_IN_THE_MAIN_FILE.length())))
        .addLocation(ScannerReport.IssueLocation.newBuilder()
          .setComponentRef(ANOTHER_FILE_REF)
          .setMsg("Secondary location in other file")
          .setTextRange(newTextRange(1, LINE_IN_ANOTHER_FILE.length())))
        .build())
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));

    Input<DefaultIssue> input = underTest.create(FILE);
    DefaultIssue issue = Iterators.getOnlyElement(input.getIssues().iterator());

    DbIssues.Locations locations = issue.getLocations();

    assertThat(locations.getFlow(0).getLocation(0).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-main-file';"));
    assertThat(locations.getFlow(0).getLocation(1).getChecksum()).isEqualTo(DigestUtils.md5Hex("Stringstring='line-in-the-another-file';"));
  }

  private CloseableIterator<String> newOneLineIterator(String lineContent) {
    return CloseableIterator.from(List.of(lineContent).iterator());
  }

  @Test
  public void set_rule_name_as_message_when_issue_message_from_report_is_empty() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    registerRule(ruleKey, "Rule 1");
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setMsg("")
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = Iterators.getOnlyElement(issues.iterator());

    // fields set by analysis report
    assertThat(issue.ruleKey()).isEqualTo(ruleKey);

    // fields set by compute engine
    assertInitializedIssue(issue);
    assertThat(issue.message()).isEqualTo("Rule 1");
  }

  // SONAR-10781
  @Test
  public void load_issues_from_report_missing_secondary_location_component() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .setGap(3.14)
      .addFlow(ScannerReport.Flow.newBuilder()
        .addLocation(ScannerReport.IssueLocation.newBuilder()
          .setComponentRef(FILE_REF)
          .setMsg("Secondary location in same file")
          .setTextRange(newTextRange(2)))
        .addLocation(ScannerReport.IssueLocation.newBuilder()
          .setComponentRef(NOT_IN_REPORT_FILE_REF)
          .setMsg("Secondary location in a missing file")
          .setTextRange(newTextRange(3)))
        .addLocation(ScannerReport.IssueLocation.newBuilder()
          .setComponentRef(ANOTHER_FILE_REF)
          .setMsg("Secondary location in another file")
          .setTextRange(newTextRange(3)))
        .build())
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = Iterators.getOnlyElement(issues.iterator());

    DbIssues.Locations locations = issue.getLocations();
    // fields set by analysis report
    assertThat(locations.getFlowList()).hasSize(1);
    assertThat(locations.getFlow(0).getLocationList()).hasSize(2);
    // Not component id if location is in the same file
    assertThat(locations.getFlow(0).getLocation(0).getComponentId()).isEmpty();
    assertThat(locations.getFlow(0).getLocation(1).getComponentId()).isEqualTo(ANOTHER_FILE_UUID);
  }

  @Test
  @UseDataProvider("ruleTypeAndStatusByIssueType")
  public void load_external_issues_from_report(IssueType issueType, RuleType expectedRuleType, String expectedStatus) {
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.ExternalIssue reportIssue = ScannerReport.ExternalIssue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setEngineId("eslint")
      .setRuleId("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .setEffort(20L)
      .setType(issueType)
      .build();
    reportReader.putExternalIssues(FILE.getReportAttributes().getRef(), asList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = Iterators.getOnlyElement(issues.iterator());

    // fields set by analysis report
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("external_eslint", "S001"));
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.line()).isEqualTo(2);
    assertThat(issue.effort()).isEqualTo(Duration.create(20L));
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.type()).isEqualTo(expectedRuleType);

    // fields set by compute engine
    assertThat(issue.checksum()).isEqualTo(input.getLineHashSequence().getHashForLine(2));
    assertThat(issue.tags()).isEmpty();
    assertInitializedExternalIssue(issue, expectedStatus);
  }

  @DataProvider
  public static Object[][] ruleTypeAndStatusByIssueType() {
    return new Object[][] {
      {IssueType.CODE_SMELL, RuleType.CODE_SMELL, STATUS_OPEN},
      {IssueType.BUG, RuleType.BUG, STATUS_OPEN},
      {IssueType.VULNERABILITY, RuleType.VULNERABILITY, STATUS_OPEN},
      {IssueType.SECURITY_HOTSPOT, RuleType.SECURITY_HOTSPOT, STATUS_TO_REVIEW}
    };
  }

  @Test
  @UseDataProvider("ruleTypeAndStatusByIssueType")
  public void load_external_issues_from_report_with_default_effort(IssueType issueType, RuleType expectedRuleType, String expectedStatus) {
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.ExternalIssue reportIssue = ScannerReport.ExternalIssue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setEngineId("eslint")
      .setRuleId("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .setType(issueType)
      .build();
    reportReader.putExternalIssues(FILE.getReportAttributes().getRef(), asList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).hasSize(1);
    DefaultIssue issue = Iterators.getOnlyElement(issues.iterator());

    // fields set by analysis report
    assertThat(issue.ruleKey()).isEqualTo(RuleKey.of("external_eslint", "S001"));
    assertThat(issue.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(issue.line()).isEqualTo(2);
    assertThat(issue.effort()).isEqualTo(Duration.create(0L));
    assertThat(issue.message()).isEqualTo("the message");
    assertThat(issue.type()).isEqualTo(expectedRuleType);

    // fields set by compute engine
    assertThat(issue.checksum()).isEqualTo(input.getLineHashSequence().getHashForLine(2));
    assertThat(issue.tags()).isEmpty();
    assertInitializedExternalIssue(issue, expectedStatus);
  }

  @Test
  public void excludes_issues_on_inactive_rules() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);

    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .setGap(3.14)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  public void filter_excludes_issues_from_report() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(false);
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .setGap(3.14)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues).isEmpty();
  }

  @Test
  public void exclude_issues_on_common_rules() {
    RuleKey ruleKey = RuleKey.of(CommonRuleKeys.commonRepositoryForLang("java"), "S001");
    markRuleAsActive(ruleKey);
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setSeverity(Constants.Severity.BLOCKER)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getIssues()).isEmpty();
  }

  @Test
  public void load_issues_of_compute_engine_common_rules() {
    RuleKey ruleKey = RuleKey.of(CommonRuleKeys.commonRepositoryForLang("java"), "InsufficientCoverage");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    DefaultIssue ceIssue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setMessage("not enough coverage")
      .setGap(10.0);
    when(commonRuleEngine.process(FILE)).thenReturn(singletonList(ceIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getIssues()).containsOnly(ceIssue);
    assertInitializedIssue(input.getIssues().iterator().next());
  }

  @Test
  public void filter_exclude_issues_on_common_rule() {
    RuleKey ruleKey = RuleKey.of(CommonRuleKeys.commonRepositoryForLang("java"), "InsufficientCoverage");
    markRuleAsActive(ruleKey);
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(false);
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    DefaultIssue ceIssue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setMessage("not enough coverage")
      .setGap(10.0);
    when(commonRuleEngine.process(FILE)).thenReturn(singletonList(ceIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getIssues()).isEmpty();
  }

  private void assertInitializedIssue(DefaultIssue issue) {
    assertInitializedExternalIssue(issue, STATUS_OPEN);
    assertThat(issue.effort()).isNull();
    assertThat(issue.effortInMinutes()).isNull();
  }

  private void assertInitializedExternalIssue(DefaultIssue issue, String expectedStatus) {
    assertThat(issue.projectKey()).isEqualTo(PROJECT.getKey());
    assertThat(issue.componentKey()).isEqualTo(FILE.getKey());
    assertThat(issue.componentUuid()).isEqualTo(FILE.getUuid());
    assertThat(issue.resolution()).isNull();
    assertThat(issue.status()).isEqualTo(expectedStatus);
    assertThat(issue.key()).isNull();
    assertThat(issue.authorLogin()).isNull();
  }

  private void markRuleAsActive(RuleKey ruleKey) {
    activeRulesHolder.put(new ActiveRule(ruleKey, Severity.CRITICAL, emptyMap(), 1_000L, null, "qp1"));
  }

  private void registerRule(RuleKey ruleKey, String name) {
    DumbRule dumbRule = new DumbRule(ruleKey);
    dumbRule.setName(name);
    ruleRepository.add(dumbRule);
  }

  private TextRange newTextRange(int issueOnLine, int endOffset) {
    return TextRange.newBuilder()
      .setStartLine(issueOnLine)
      .setEndLine(issueOnLine)
      .setStartOffset(0)
      .setEndOffset(endOffset)
      .build();
  }

  private TextRange newTextRange(int issueOnLine) {
    return TextRange.newBuilder()
      .setStartLine(issueOnLine)
      .setEndLine(issueOnLine)
      .setStartOffset(0)
      .setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)
      .build();
  }

  private void assertLocationHashIsMadeOf(Input<DefaultIssue> input, String stringToHash) {
    DefaultIssue defaultIssue = Iterators.getOnlyElement(input.getIssues().iterator());
    String expectedHash = DigestUtils.md5Hex(stringToHash);
    DbIssues.Locations locations = defaultIssue.getLocations();

    assertThat(locations.getChecksum()).isEqualTo(expectedHash);
  }

}
