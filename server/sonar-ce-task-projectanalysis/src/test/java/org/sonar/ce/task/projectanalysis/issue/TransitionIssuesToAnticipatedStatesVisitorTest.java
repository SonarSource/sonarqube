/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.ce.task.projectanalysis.component.ComponentImpl;
import org.sonar.ce.task.projectanalysis.component.ProjectAttributes;
import org.sonar.ce.task.projectanalysis.component.ReportAttributes;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.issue.DefaultIssue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.PROJECT;

public class TransitionIssuesToAnticipatedStatesVisitorTest {

  private final IssueLifecycle issueLifecycle = mock(IssueLifecycle.class);

  private final AnticipatedTransitionRepository anticipatedTransitionRepository = mock(AnticipatedTransitionRepository.class);

  private final TransitionIssuesToAnticipatedStatesVisitor underTest = new TransitionIssuesToAnticipatedStatesVisitor(anticipatedTransitionRepository, issueLifecycle);

  @Test
  public void givenMatchingAnticipatedTransitions_transitionsShouldBeAppliedToIssues() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component)).thenReturn(getAnticipatedTransitions("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isTrue();
    assertThat(issue.hasAnticipatedTransitions()).isTrue();
    verify(issueLifecycle).doManualTransition(issue, "wontfix", "admin");
    verify(issueLifecycle).addComment(issue, "doing the transition in an anticipated way", "admin");
  }

  @Test
  public void givenNonMatchingAnticipatedTransitions_transitionsAreNotAppliedToIssues() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component)).thenReturn(getAnticipatedTransitions("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(2, "abcdefghf", "another issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isFalse();
    assertThat(issue.hasAnticipatedTransitions()).isFalse();
    verifyNoInteractions(issueLifecycle);
  }

  @Test
  public void givenMatchingAnticipatedTransitionsWithEmptyComment_transitionsShouldBeAppliedToIssuesAndDefaultCommentApplied() {
    Component component = getComponent(Component.Type.FILE);
    when(anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component)).thenReturn(getAnticipatedTransitionsWithEmptyComment("projectKey", "fileName"));

    DefaultIssue issue = getDefaultIssue(1, "abcdefghi", "issue message");

    underTest.beforeComponent(component);
    underTest.onIssue(component, issue);

    assertThat(issue.isBeingClosed()).isTrue();
    assertThat(issue.hasAnticipatedTransitions()).isTrue();
    verify(issueLifecycle).doManualTransition(issue, "wontfix", "admin");
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

  private Collection<AnticipatedTransition> getAnticipatedTransitions(String projecKey, String fileName) {
    return Stream.of(new AnticipatedTransition(projecKey, null, "admin", RuleKey.parse("repo:id"), "issue message", fileName, 1, "abcdefghi", "wontfix", "doing the transition in an anticipated way")).collect(Collectors.toList());
  }

  private Collection<AnticipatedTransition> getAnticipatedTransitionsWithEmptyComment(String projecKey, String fileName) {
    return Stream.of(new AnticipatedTransition(projecKey, null, "admin", RuleKey.parse("repo:id"), "issue message", fileName, 1, "abcdefghi", "wontfix", null)).collect(Collectors.toList());
  }

  private Component getComponent(Component.Type type) {
    ComponentImpl.Builder builder = ComponentImpl.builder(type)
      .setUuid("componentUuid")
      .setKey("projectKey:filename")
      .setName("filename")
      .setStatus(Component.Status.ADDED)
      .setShortName("filename")
      .setReportAttributes(mock(ReportAttributes.class));

    if (PROJECT.equals(type)) {
      builder.setProjectAttributes(mock(ProjectAttributes.class));
    }

    return builder.build();
  }

  private DefaultIssue getDefaultIssue(Integer line, String hash, String message) {
    DefaultIssue defaultIssue = new DefaultIssue();
    defaultIssue.setLine(line);
    defaultIssue.setChecksum(hash);
    defaultIssue.setMessage(message);
    defaultIssue.setRuleKey(RuleKey.of("repo", "id"));
    return defaultIssue;
  }

}
