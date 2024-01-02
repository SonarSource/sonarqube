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
package org.sonar.ce.task.projectanalysis.issue;

import com.google.common.collect.Iterators;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
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
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.Input;
import org.sonar.db.protobuf.DbIssues;
import org.sonar.scanner.protocol.Constants;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.scanner.protocol.output.ScannerReport.FlowType;
import org.sonar.scanner.protocol.output.ScannerReport.IssueType;
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
import static org.sonar.scanner.protocol.output.ScannerReport.MessageFormattingType.CODE;

@RunWith(DataProviderRunner.class)
public class TrackerRawInputFactoryTest {

  private static final String FILE_UUID = "fake_uuid";
  private static final String ANOTHER_FILE_UUID = "another_fake_uuid";
  private static final String EXAMPLE_LINE_OF_CODE_FORMAT = "int example = line + of + code + %d; ";

  private static final int FILE_REF = 2;
  private static final int NOT_IN_REPORT_FILE_REF = 3;
  private static final int ANOTHER_FILE_REF = 4;
  private static final String TEST_CONTEXT_KEY = "test_context_key";

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
  private final CommonRuleEngine commonRuleEngine = mock(CommonRuleEngine.class);
  private final IssueFilter issueFilter = mock(IssueFilter.class);
  private final TrackerRawInputFactory underTest = new TrackerRawInputFactory(treeRootHolder, reportReader, sourceLinesHash,
    commonRuleEngine, issueFilter, ruleRepository, activeRulesHolder);

  @Before
  public void before() {
    when(sourceLinesHash.getLineHashesMatchingDBVersion(FILE)).thenReturn(Collections.singletonList("line"));
    when(issueFilter.accept(any(), eq(FILE))).thenReturn(true);
  }

  @Test
  public void load_source_hash_sequences() {
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

    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .addMsgFormatting(ScannerReport.MessageFormatting.newBuilder().setStart(0).setEnd(3).setType(CODE).build())
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

    // Check message formatting
    DbIssues.MessageFormattings messageFormattings = Iterators.getOnlyElement(issues.iterator()).getMessageFormattings();
    assertThat(messageFormattings.getMessageFormattingCount()).isEqualTo(1);
    assertThat(messageFormattings.getMessageFormatting(0).getStart()).isZero();
    assertThat(messageFormattings.getMessageFormatting(0).getEnd()).isEqualTo(3);
    assertThat(messageFormattings.getMessageFormatting(0).getType()).isEqualTo(DbIssues.MessageFormattingType.CODE);

    // fields set by compute engine
    assertThat(issue.checksum()).isEqualTo(input.getLineHashSequence().getHashForLine(2));
    assertThat(issue.tags()).isEmpty();
    assertInitializedIssue(issue);
    assertThat(issue.effort()).isNull();
    assertThat(issue.getRuleDescriptionContextKey()).isEmpty();
  }

  @Test
  public void load_issues_from_report_with_locations() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);

    ScannerReport.MessageFormatting messageFormatting = ScannerReport.MessageFormatting.newBuilder().setStart(0).setEnd(4).setType(CODE).build();
    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .addFlow(ScannerReport.Flow.newBuilder()
        .setType(FlowType.DATA)
        .setDescription("flow1")
        .addLocation(ScannerReport.IssueLocation.newBuilder().setMsg("loc1").addMsgFormatting(messageFormatting).setComponentRef(1).build())
        .addLocation(ScannerReport.IssueLocation.newBuilder().setMsg("loc2").setComponentRef(1).build()))
      .addFlow(ScannerReport.Flow.newBuilder()
        .setType(FlowType.EXECUTION)
        .addLocation(ScannerReport.IssueLocation.newBuilder().setTextRange(newTextRange(2)).setComponentRef(1).build()))
      .addFlow(ScannerReport.Flow.newBuilder()
        .addLocation(ScannerReport.IssueLocation.newBuilder().setTextRange(newTextRange(2)).setComponentRef(1).build()))
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    DbIssues.Locations locations = Iterators.getOnlyElement(issues.iterator()).getLocations();
    assertThat(locations.getFlowCount()).isEqualTo(3);
    assertThat(locations.getFlow(0).getDescription()).isEqualTo("flow1");
    assertThat(locations.getFlow(0).getType()).isEqualTo(DbIssues.FlowType.DATA);
    assertThat(locations.getFlow(0).getLocationList()).hasSize(2);

    assertThat(locations.getFlow(0).getLocation(0).getMsg()).isEqualTo("loc1");
    assertThat(locations.getFlow(0).getLocation(0).getMsgFormattingCount()).isEqualTo(1);
    assertThat(locations.getFlow(0).getLocation(0).getMsgFormatting(0)).extracting(m -> m.getStart(), m -> m.getEnd(), m -> m.getType())
      .containsExactly(0, 4, DbIssues.MessageFormattingType.CODE);

    assertThat(locations.getFlow(1).hasDescription()).isFalse();
    assertThat(locations.getFlow(1).getType()).isEqualTo(DbIssues.FlowType.EXECUTION);
    assertThat(locations.getFlow(1).getLocationList()).hasSize(1);

    assertThat(locations.getFlow(2).hasDescription()).isFalse();
    assertThat(locations.getFlow(2).hasType()).isFalse();
    assertThat(locations.getFlow(2).getLocationList()).hasSize(1);
  }

  @Test
  public void load_issues_from_report_with_rule_description_context_key() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);

    ScannerReport.Issue reportIssue = ScannerReport.Issue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .setRuleRepository(ruleKey.repository())
      .setRuleKey(ruleKey.rule())
      .setRuleDescriptionContextKey(TEST_CONTEXT_KEY)
      .build();
    reportReader.putIssues(FILE.getReportAttributes().getRef(), singletonList(reportIssue));
    Input<DefaultIssue> input = underTest.create(FILE);

    Collection<DefaultIssue> issues = input.getIssues();
    assertThat(issues)
      .hasSize(1)
      .extracting(DefaultIssue::getRuleDescriptionContextKey)
      .containsOnly(Optional.of(TEST_CONTEXT_KEY));
  }

  @Test
  public void set_rule_name_as_message_when_issue_message_from_report_is_empty() {
    RuleKey ruleKey = RuleKey.of("java", "S001");
    markRuleAsActive(ruleKey);
    registerRule(ruleKey, "Rule 1");
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
    ScannerReport.ExternalIssue reportIssue = ScannerReport.ExternalIssue.newBuilder()
      .setTextRange(newTextRange(2))
      .setMsg("the message")
      .addMsgFormatting(ScannerReport.MessageFormatting.newBuilder().setStart(0).setEnd(3).build())
      .setEngineId("eslint")
      .setRuleId("S001")
      .setSeverity(Constants.Severity.BLOCKER)
      .setEffort(20L)
      .setType(issueType)
      .addFlow(ScannerReport.Flow.newBuilder().setType(FlowType.DATA).addLocation(ScannerReport.IssueLocation.newBuilder().build()).build())
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

    // Check message formatting
    DbIssues.MessageFormattings messageFormattings = Iterators.getOnlyElement(issues.iterator()).getMessageFormattings();
    assertThat(messageFormattings.getMessageFormattingCount()).isEqualTo(1);
    assertThat(messageFormattings.getMessageFormatting(0).getStart()).isZero();
    assertThat(messageFormattings.getMessageFormatting(0).getEnd()).isEqualTo(3);
    assertThat(messageFormattings.getMessageFormatting(0).getType()).isEqualTo(DbIssues.MessageFormattingType.CODE);

    assertThat(issue.type()).isEqualTo(expectedRuleType);

    DbIssues.Locations locations = Iterators.getOnlyElement(issues.iterator()).getLocations();
    assertThat(locations.getFlowCount()).isEqualTo(1);
    assertThat(locations.getFlow(0).getType()).isEqualTo(DbIssues.FlowType.DATA);
    assertThat(locations.getFlow(0).getLocationList()).hasSize(1);

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
    DefaultIssue ceIssue = new DefaultIssue()
      .setRuleKey(ruleKey)
      .setMessage("not enough coverage")
      .setGap(10.0);
    when(commonRuleEngine.process(FILE)).thenReturn(singletonList(ceIssue));

    Input<DefaultIssue> input = underTest.create(FILE);

    assertThat(input.getIssues()).isEmpty();
  }

  private ScannerReport.TextRange newTextRange(int issueOnLine) {
    return ScannerReport.TextRange.newBuilder()
      .setStartLine(issueOnLine)
      .setEndLine(issueOnLine)
      .setStartOffset(0)
      .setEndOffset(EXAMPLE_LINE_OF_CODE_FORMAT.length() - 1)
      .build();
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
}
