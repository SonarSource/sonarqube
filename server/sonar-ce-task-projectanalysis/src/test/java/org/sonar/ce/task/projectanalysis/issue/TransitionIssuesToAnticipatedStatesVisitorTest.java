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

import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.event.Level;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.testfixtures.log.LogTester;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentImpl;
import org.sonar.ce.task.projectanalysis.component.ProjectAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;

public class TransitionIssuesToAnticipatedStatesVisitorTest {

  @Rule
  public LogTester logTester = new LogTester();
  private final IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);

  private final AnticipatedTransitionRepository anticipatedTransitionRepository = mock(AnticipatedTransitionRepository.class);

  private final CeTaskMessages ceTaskMessages = mock(CeTaskMessages.class);

  private final TransitionIssuesToAnticipatedStatesVisitor underTest = new TransitionIssuesToAnticipatedStatesVisitor(anticipatedTransitionRepository, issueLifecycle,
    ceTaskMessages);

  @Test
  public void givenMatchingAnticipatedTransitions_transitionsShouldBeAppliedToIssues() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component))
      .thenReturn(getAnticipatedTransitions("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isTrue();
    assertThat(issue.getAnticipatedTransitionUuid()).isPresent();
    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), "admin");
    verify(issueLifecycle).addComment(issue, "doing the transition in an anticipated way", "admin");
  }

  @Test
  public void givenMatchingAnticipatedTransitions_whenExceptionIsThrown_transitionsShouldNotBeAppliedAndWarningLogged() {
    Component component = getComponent(Component.Type.FILE);
    String exceptionMessage = "Cannot apply transition";

    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component))
      .thenReturn(getAnticipatedTransitions("projectKey", "fileName"));
    doThrow(new IllegalStateException(exceptionMessage)).when(issueLifecycle).doManualTransition(any(), anyString(), any());
    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");
    issue.setComponentKey(component.getKey());

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isFalse();
    assertThat(issue.getAnticipatedTransitionUuid()).isEmpty();
    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), "admin");
    verifyNoMoreInteractions(issueLifecycle);
    assertThat(logTester.logs(Level.WARN))
      .contains(String.format("Cannot resolve issue at line %s of %s due to: %s", issue.getLine(), issue.componentKey(), exceptionMessage));
    verify(ceTaskMessages, times(1)).add(any());
  }

  @Test
  public void givenMatchingAnticipatedTransitionsOnResolvedIssue_transitionsShouldNotBeAppliedToIssues() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component))
      .thenReturn(getAnticipatedTransitions("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");
    issue.setStatus(STATUS_RESOLVED);

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isFalse();
    assertThat(issue.getAnticipatedTransitionUuid()).isNotPresent();
    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void givenMatchingAnticipatedTransitions_whenIssueIsNotNew_transitionsShouldNotBeAppliedToIssues() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component))
      .thenReturn(getAnticipatedTransitions("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");
    issue.setNew(false);

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isFalse();
    assertThat(issue.getAnticipatedTransitionUuid()).isNotPresent();
    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void givenNonMatchingAnticipatedTransitions_transitionsAreNotAppliedToIssues() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component))
      .thenReturn(getAnticipatedTransitions("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(2, "abcdefghf", "another issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isFalse();
    assertThat(issue.getAnticipatedTransitionUuid()).isNotPresent();
    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void givenMatchingAnticipatedTransitionsWithEmptyComment_transitionsShouldBeAppliedToIssuesAndDefaultCommentApplied() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component))
      .thenReturn(getAnticipatedTransitionsWithEmptyComment("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isTrue();
    assertThat(issue.getAnticipatedTransitionUuid()).isPresent();
    verify(issueLifecycle).doManualTransition(issue, CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), "admin");
    verify(issueLifecycle).addComment(issue, "Automatically transitioned from SonarLint", "admin");
  }

  @Test
  public void givenAFileComponent_theRepositoryIsHitForFetchingAnticipatedTransitions() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component)).thenReturn(Collections.emptyList());

    underTest.beforeComponent(component);

    verify(anticipatedTransitionRepository).getAnticipatedTransitionByComponent(component);
  }

  @Test
  public void givenAProjecComponent_theRepositoryIsNotQueriedForAnticipatedTransitions() {
    Component component = getComponent(PROJECT);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component)).thenReturn(Collections.emptyList());

    underTest.beforeComponent(component);

    verifyNoInteractions(anticipatedTransitionRepository);
  }

  @Test
  public void givenAProjecComponent_the_issue_is_not_affected() {
    Component component = getComponent(PROJECT);
    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);
    assertThat(issue.getAnticipatedTransitionUuid()).isEmpty();
    assertThat(issue.isBeingClosed()).isFalse();
  }

  private Collection<AnticipatedTransition> getAnticipatedTransitions(String projecKey, String fileName) {
    return Stream.of(new AnticipatedTransition("atuuid", projecKey, "admin", RuleKey.parse("repo:id"), "issue message", fileName, 1,
      "abcdefghi", CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), "doing the transition in an anticipated way")).toList();
  }

  private Collection<AnticipatedTransition> getAnticipatedTransitionsWithEmptyComment(String projecKey, String fileName) {
    return Stream.of(new AnticipatedTransition("atuuid", projecKey, "admin", RuleKey.parse("repo:id"), "issue message", fileName, 1,
      "abcdefghi", CodeQualityIssueWorkflowTransition.ACCEPT.getKey(), null)).toList();
  }

  private Component getComponent(Component.Type type) {
    ComponentImpl.Builder builder = ComponentImpl.builder(type)
      .setUuid("componentUuid")
      .setKey("projectKey:filename")
      .setName("filename")
      .setStatus(Component.Status.ADDED)
      .setShortName("filename")
      .setReportAttributes(mock(ReportAttributes.class));

    if (PROJECT == type) {
      builder.setProjectAttributes(mock(ProjectAttributes.class));
    }

    return builder.build();
  }

  private DefaultIssue getDefaultIssue(Integer line, String hash, String message) {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setStatus(STATUS_OPEN);
    defaultIssue.setResolution(null);
    defaultIssue.setLine(line);
    defaultIssue.setChecksum(hash);
    defaultIssue.setMessage(message);
    defaultIssue.setRuleKey(RuleKey.of("repo", "id"));
    return defaultIssue;
  }

}
