/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.internal.MapSettings;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.common.scanner.ScannerReportReaderRule;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ConfigurationRepository;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.ce.task.projectanalysis.component.TestSettingsRepository;
import org.sonar.ce.task.projectanalysis.scm.ScmInfoRepository;
import org.sonar.core.config.CorePropertyDefinitions;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.scanner.protocol.output.ScannerReport;
import org.sonar.server.issue.IssueFieldsSetter;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;

class IssueResolutionVisitorIT {

  @RegisterExtension
  ScannerReportReaderRule reportReader = new ScannerReportReaderRule();

  private final IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);
  private final ScmInfoRepository scmInfoRepository = mock(ScmInfoRepository.class);
  private final ScmAccountToUser scmAccountToUser = mock(ScmAccountToUser.class);
  private IssueResolutionVisitor underTest;

  @BeforeEach
  void setUp() {
    underTest = createVisitor(true);
  }

  @Test
  void enabled_appliesIssueResolutionFromScannerReport() {
    putDefaultIssueResolution();
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), null);
    verify(issueLifecycle).addComment(issue, "issue-resolution: approved", null);
    assertThat(issue.internalTags()).contains(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void disabled_doesNotApplyIssueResolutionData() {
    underTest = createVisitor(false);
    putDefaultIssueResolution();
    Component component = createComponent(1);

    underTest.beforeComponent(component);

    DefaultIssue issue = newIssue();
    issue.setLine(3);
    underTest.onIssue(component, issue);

    verifyNoInteractions(issueLifecycle);
    assertThat(issue.internalTags()).doesNotContain(IssueFieldsSetter.ISSUE_RESOLUTION_TAG);
  }

  @Test
  void disabled_reopensAlreadyTaggedIssue() {
    underTest = createVisitor(false);
    putDefaultIssueResolution();
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

  private static Component createComponent(int ref) {
    Component component = mock(Component.class);
    ReportAttributes reportAttributes = mock(ReportAttributes.class);
    when(reportAttributes.getRef()).thenReturn(ref);
    when(component.getReportAttributes()).thenReturn(reportAttributes);
    return component;
  }

  private void putDefaultIssueResolution() {
    ScannerReport.IssueResolution issueResolution = ScannerReport.IssueResolution.newBuilder()
      .addAllRuleKeys(List.of("java:S123"))
      .setTextRange(ScannerReport.TextRange.newBuilder().setStartLine(3).setEndLine(3).build())
      .setComment("approved")
      .setStatus(ScannerReport.IssueResolutionStatus.DEFAULT)
      .build();
    reportReader.putIssueResolution(1, List.of(issueResolution));
  }

  private IssueResolutionVisitor createVisitor(boolean enabled) {
    MapSettings settings = new MapSettings();
    settings.setProperty(CorePropertyDefinitions.ISSUE_RESOLUTION_GLOBAL_ENABLED, String.valueOf(enabled));
    settings.setProperty(CorePropertyDefinitions.ISSUE_RESOLUTION_ENABLED, String.valueOf(enabled));
    ConfigurationRepository configurationRepository = new TestSettingsRepository(settings.asConfig());
    return new IssueResolutionVisitor(issueLifecycle, scmInfoRepository, scmAccountToUser, reportReader, configurationRepository);
  }

  private static DefaultIssue newIssue() {
    DefaultIssue issue = new DefaultIssue();
    issue.setRuleKey(RuleKey.of("java", "S123"));
    return issue;
  }
}
