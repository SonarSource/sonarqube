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
package org.sonar.ce.task.projectanalysis.issue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.event.Level;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTesterJUnit5;
import org.sonar.ce.common.scanner.ScannerReportReader;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.ce.task.projectanalysis.component.TestSettingsRepository;
import org.sonar.ce.task.projectanalysis.scm.Changeset;
import org.sonar.ce.task.projectanalysis.scm.ScmInfo;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.rule.RuleType;
import org.sonar.core.util.CloseableIterator;
import org.sonar.db.user.UserIdDto;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition;
import org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;

class IssueResolutionVisitorTest {

  @RegisterExtension
  LogTesterJUnit5 logTester = new LogTesterJUnit5();

  private final IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);
  private final ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
  private final ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  private final ScannerReportReader reportReader = mock(ScannerReportReader.class);
  private IssueResolutionVisitor underTest;

  @BeforeEach
  void setUp() {
    underTest = createVisitor(true);
  }

  @Test
  void onIssue_whenNoIssueResolutionInReport_doesNothing() {
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.emptyCloseableIterator());
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(1);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenIssueResolutionDisabled_matchingDataIsIgnored() {
    underTest = createVisitor(false);
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenIssueResolutionDisabled_taggedIssueIsReopened() {
    underTest = createVisitor(false);
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_RESOLVED);
    issue.setResolution(RESOLUTION_WONT_FIX);
    issue.setInternalTags(Set.of(IssueFieldsSetter.ISSUE_RESOLUTION_TAG));
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.REOPEN.getKey(), null);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenIssueResolutionDisabled_hotspotTaggedIssueIsReopened() {
    underTest = createVisitor(false);
    Component component = createComponent(1);
    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_REVIEWED);
    issue.setResolution(RESOLUTION_ACKNOWLEDGED);
    issue.setInternalTags(Set.of(IssueFieldsSetter.ISSUE_RESOLUTION_TAG));
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW.getKey(), null);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenIssueResolutionDisabled_hotspotMatchingDataIsIgnored() {
    underTest = createVisitor(false);
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("xoo:Hotspot"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("approved")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @ParameterizedTest
  @MethodSource("issueResolutionStatusAndTransition")
  void onIssue_whenIssueResolution_transitionsAndAddsComment(ScannerReport.IssueResolutionStatus reportStatus,
    CodeQualityIssueWorkflowTransition expectedTransition) {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(reportStatus)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, expectedTransition.getKey(), null);
    verify(issueLifecycle).addComment(issue, "issue-resolution: comment", null);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @ParameterizedTest
  @MethodSource("alreadyResolvedStatusAndResolution")
  void onIssue_whenAlreadyResolved_doesNotTransitionButAddsTag(ScannerReport.IssueResolutionStatus reportStatus,
    String currentResolution) {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(reportStatus)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_RESOLVED);
    issue.setResolution(currentResolution);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @ParameterizedTest
  @MethodSource("resolutionChangedParams")
  void onIssue_whenResolutionChanged_reopensThenTransitions(ScannerReport.IssueResolutionStatus newStatus,
    String currentResolution, CodeQualityIssueWorkflowTransition expectedTransition) {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(newStatus)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_RESOLVED);
    issue.setResolution(currentResolution);
    underTest.onIssue(component, issue);

    var inOrder = org.mockito.Mockito.inOrder(issueLifecycle);
    inOrder.verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.REOPEN.getKey(), null);
    inOrder.verify(issueLifecycle).doManualTransition(issue, expectedTransition.getKey(), null);
    inOrder.verify(issueLifecycle).addComment(issue, "issue-resolution: comment", null);
  }

  @Test
  void onIssue_whenTransitionFails_logsWarning() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(42).setEndLine(42).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(42);
    issue.setComponentKey("project:src/Foo.java");

    doThrow(new IllegalStateException("Cannot apply transition"))
      .when(issueLifecycle).doManualTransition(any(), anyString(), any());

    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isFalse();
    assertThat(logTester.logs(Level.WARN))
      .contains("Cannot apply issue resolution data on issue at line 42 of project:src/Foo.java");
  }

  @Test
  void onIssue_whenBlameInfoAvailable_usesAuthorAsCommentUser() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("approved")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));

    ScmInfo scmInfo = mock(ScmInfo.class);
    Component component = createComponent(1);
    when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));
    when(scmInfo.hasChangesetForLine(3)).thenReturn(true);
    when(scmInfo.getChangesetForLine(3)).thenReturn(Changeset.newChangesetBuilder()
      .setAuthor("dev@example.com").setDate(1L).build());
    when(scmAccountToUser.getNullable("dev@example.com")).thenReturn(new UserIdDto("user-uuid-1", "devlogin"));

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), "user-uuid-1");
    verify(issueLifecycle).addComment(issue, "issue-resolution: approved", "user-uuid-1");
  }

  @Test
  void onIssue_whenNoBlameInfo_usesNullUser() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));

    Component component = createComponent(1);
    when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.empty());

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), null);
    verify(issueLifecycle).addComment(issue, "issue-resolution: comment", null);
  }

  @Test
  void onIssue_whenScmAuthorNotMappedToUser_usesNullUser() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));

    ScmInfo scmInfo = mock(ScmInfo.class);
    Component component = createComponent(1);
    when(scmInfoRepository.getScmInfo(component)).thenReturn(Optional.of(scmInfo));
    when(scmInfo.hasChangesetForLine(3)).thenReturn(true);
    when(scmInfo.getChangesetForLine(3)).thenReturn(Changeset.newChangesetBuilder()
      .setAuthor("unknown@example.com").setDate(1L).build());
    when(scmAccountToUser.getNullable("unknown@example.com")).thenReturn(null);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), null);
    verify(issueLifecycle).addComment(issue, "issue-resolution: comment", null);
  }

  @Test
  void onIssue_whenNonMatchingRuleKey_doesNothing() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S999"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
  }

  @Test
  void onIssue_whenNonMatchingLine_doesNothing() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(99).setEndLine(99).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
  }

  @Test
  void onIssue_whenMultipleIssueResolutionOnSameLine_matchesByRuleKey() {
    ScannerReport.IssueResolution acceptS123 = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("accept S123")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    ScannerReport.IssueResolution fpS456 = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S456"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("fp S456")
      .setStatus(ScannerReport.IssueResolutionStatus.FALSE_POSITIVE)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(acceptS123, fpS456).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issueS123 = newIssue();
    issueS123.setLine(3);
    issueS123.setRuleKey(RuleKey.of("java", "S123"));
    underTest.onIssue(component, issueS123);

    verify(issueLifecycle).doManualTransition(issueS123, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), null);
    verify(issueLifecycle).addComment(issueS123, "issue-resolution: accept S123", null);

    DefaultIssue issueS456 = newIssue();
    issueS456.setLine(3);
    issueS456.setRuleKey(RuleKey.of("java", "S456"));
    underTest.onIssue(component, issueS456);

    verify(issueLifecycle).doManualTransition(issueS456, CodeQualityIssueWorkflowTransition.FALSE_POSITIVE.getKey(), null);
    verify(issueLifecycle).addComment(issueS456, "issue-resolution: fp S456", null);
  }

  @Test
  void onIssue_whenMultipleIssuesOnLineButOnlyOneRuleSilenced_onlyMatchingIssueIsTransitioned() {
    ScannerReport.IssueResolution data = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("silenced")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(data).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue matchingIssue = newIssue();
    matchingIssue.setLine(3);
    matchingIssue.setRuleKey(RuleKey.of("java", "S123"));
    underTest.onIssue(component, matchingIssue);

    verify(issueLifecycle).doManualTransition(matchingIssue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), null);

    org.mockito.Mockito.reset(issueLifecycle);

    DefaultIssue nonMatchingIssue = newIssue();
    nonMatchingIssue.setLine(3);
    nonMatchingIssue.setRuleKey(RuleKey.of("java", "S789"));
    underTest.onIssue(component, nonMatchingIssue);

    verifyNoInteractions(issueLifecycle);
  }

  @Test
  void onIssue_reopensIssue_whenTagPresentButIssueResolutionRemoved() {
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.emptyCloseableIterator());
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_RESOLVED);
    issue.setResolution(RESOLUTION_WONT_FIX);
    issue.setInternalTags(Set.of(IssueFieldsSetter.ISSUE_RESOLUTION_TAG));
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.REOPEN.getKey(), null);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_removesStaleTag_whenTagPresentButIssueAlreadyOpen() {
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.emptyCloseableIterator());
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    // Issue is OPEN (default from newIssue()) but has a stale tag
    issue.setInternalTags(Set.of(IssueFieldsSetter.ISSUE_RESOLUTION_TAG));
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
    assertThat(issue.isChanged()).isTrue();
  }

  @Test
  void onIssue_whenReopenFails_logsWarning() {
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.emptyCloseableIterator());
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(42);
    issue.setComponentKey("project:src/Foo.java");
    issue.setStatus(STATUS_RESOLVED);
    issue.setResolution(RESOLUTION_WONT_FIX);
    issue.setInternalTags(Set.of(IssueFieldsSetter.ISSUE_RESOLUTION_TAG));

    doThrow(new IllegalStateException("Cannot apply transition"))
      .when(issueLifecycle).doManualTransition(any(), anyString(), any());

    underTest.onIssue(component, issue);

    assertThat(logTester.logs(Level.WARN))
      .contains("Cannot reopen issue-resolution issue at line 42 of project:src/Foo.java");
  }

  @Test
  void afterComponent_clearsIssueResolution() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("comment")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    when(reportReader.readIssueResolution(2)).thenReturn(CloseableIterator.emptyCloseableIterator());

    Component component1 = createComponent(1);
    Component component2 = createComponent(2);

    underTest.beforeComponent(component1);
    underTest.afterComponent(component1);

    // Component 2 has no issue resolution
    underTest.beforeComponent(component2);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component2, issue);

    verifyNoInteractions(issueLifecycle);
  }

  private static Stream<Arguments> issueResolutionStatusAndTransition() {
    return Stream.of(
      Arguments.of(ScannerReport.IssueResolutionStatus.DEFAULT, CodeQualityIssueWorkflowTransition.ACCEPT),
      Arguments.of(ScannerReport.IssueResolutionStatus.FALSE_POSITIVE, CodeQualityIssueWorkflowTransition.FALSE_POSITIVE));
  }

  private static Stream<Arguments> alreadyResolvedStatusAndResolution() {
    return Stream.of(
      Arguments.of(ScannerReport.IssueResolutionStatus.DEFAULT, RESOLUTION_WONT_FIX),
      Arguments.of(ScannerReport.IssueResolutionStatus.FALSE_POSITIVE, RESOLUTION_FALSE_POSITIVE));
  }

  private static Stream<Arguments> resolutionChangedParams() {
    return Stream.of(
      Arguments.of(ScannerReport.IssueResolutionStatus.DEFAULT, RESOLUTION_FALSE_POSITIVE, CodeQualityIssueWorkflowTransition.ACCEPT),
      Arguments.of(ScannerReport.IssueResolutionStatus.FALSE_POSITIVE, RESOLUTION_WONT_FIX, CodeQualityIssueWorkflowTransition.FALSE_POSITIVE));
  }

  private IssueResolutionVisitor createVisitor(boolean issueResolutionEnabled) {
    MapSettings settings = new MapSettings();
    settings.setProperty(CorePropertyDefinitions.ISSUE_RESOLUTION_GLOBAL_ENABLED, String.valueOf(issueResolutionEnabled));
    settings.setProperty(CorePropertyDefinitions.ISSUE_RESOLUTION_ENABLED, String.valueOf(issueResolutionEnabled));
    ConfigurationRepository configurationRepository = new TestSettingsRepository(settings.asConfig());
    return new IssueResolutionVisitor(issueLifecycle, scmInfoRepository, scmAccountToUser, reportReader, configurationRepository);
  }

  private static Component createComponent(int ref) {
    Component component = mock(Component.class);
    ReportAttributes reportAttributes = mock(ReportAttributes.class);
    when(reportAttributes.getRef()).thenReturn(ref);
    when(component.getReportAttributes()).thenReturn(reportAttributes);
    return component;
  }

  private static DefaultIssue newIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setStatus(STATUS_OPEN);
    issue.setResolution(null);
    issue.setRuleKey(RuleKey.of("java", "S123"));
    return issue;
  }

  private static DefaultIssue newHotspotIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setType(RuleType.SECURITY_HOTSPOT);
    issue.setStatus(STATUS_TO_REVIEW);
    issue.setResolution(null);
    issue.setRuleKey(RuleKey.of("xoo", "Hotspot"));
    return issue;
  }

  @Test
  void onIssue_whenHotspotAndDefaultResolution_transitionsToAcknowledged() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("xoo:Hotspot"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("approved")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, SecurityHotspotWorkflowTransition.RESOLVE_AS_ACKNOWLEDGED.getKey(), null);
    verify(issueLifecycle).addComment(issue, "issue-resolution: approved", null);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenHotspotAndFpResolution_transitionsToSafe() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("xoo:Hotspot"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("safe")
      .setStatus(ScannerReport.IssueResolutionStatus.FALSE_POSITIVE)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, SecurityHotspotWorkflowTransition.RESOLVE_AS_SAFE.getKey(), null);
    verify(issueLifecycle).addComment(issue, "issue-resolution: safe", null);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenHotspotAlreadyAcknowledged_doesNotTransitionButAddsTag() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("xoo:Hotspot"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("approved")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_REVIEWED);
    issue.setResolution(RESOLUTION_ACKNOWLEDGED);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenHotspotAlreadySafe_doesNotTransitionButAddsTag() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("xoo:Hotspot"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("safe")
      .setStatus(ScannerReport.IssueResolutionStatus.FALSE_POSITIVE)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_REVIEWED);
    issue.setResolution(RESOLUTION_SAFE);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenHotspotHasTagButResolutionRemoved_resetsToToReview() {
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.emptyCloseableIterator());
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_REVIEWED);
    issue.setResolution(RESOLUTION_ACKNOWLEDGED);
    issue.setInternalTags(Set.of(IssueFieldsSetter.ISSUE_RESOLUTION_TAG));
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW.getKey(), null);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void onIssue_whenHotspotResolutionChanged_reopensThenTransitions() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("xoo:Hotspot"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("approved")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    when(reportReader.readIssueResolution(1)).thenReturn(CloseableIterator.from(List.of(issueResolution).iterator()));
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newHotspotIssue();
    issue.setLine(3);
    issue.setStatus(STATUS_REVIEWED);
    issue.setResolution(RESOLUTION_SAFE);
    underTest.onIssue(component, issue);

    var inOrder = org.mockito.Mockito.inOrder(issueLifecycle);
    inOrder.verify(issueLifecycle).doManualTransition(issue, SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW.getKey(), null);
    inOrder.verify(issueLifecycle).doManualTransition(issue, SecurityHotspotWorkflowTransition.RESOLVE_AS_ACKNOWLEDGED.getKey(), null);
    inOrder.verify(issueLifecycle).addComment(issue, "issue-resolution: approved", null);
  }
}
