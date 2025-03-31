/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.server.issue;

import java.util.List;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.workflow.IssueWorkflow;
import org.sonar.server.issue.workflow.WorkflowTransition;
import org.sonar.server.issue.workflow.statemachine.Transition;
import org.sonar.server.user.UserSession;

import static java.util.Objects.requireNonNull;

/**
 * This service is a kind of overlay of {@link IssueWorkflow} that also deals with permission checking
 */
public class TransitionService {

  private final UserSession userSession;
  private final IssueWorkflow workflow;

  public TransitionService(UserSession userSession, IssueWorkflow workflow) {
    this.userSession = userSession;
    this.workflow = workflow;
  }

  public List<String> listTransitionKeys(DefaultIssue issue) {
    String projectUuid = requireNonNull(issue.projectUuid());
    return workflow.outTransitions(issue)
      .stream()
      .filter(transition -> (userSession.isLoggedIn() && transition.requiredProjectPermission() == null)
        || (transition.requiredProjectPermission() != null && userSession.hasComponentUuidPermission(transition.requiredProjectPermission(), projectUuid)))
      .map(Transition::key)
      .toList();
  }

  public boolean doTransition(DefaultIssue defaultIssue, IssueChangeContext issueChangeContext, WorkflowTransition transition) {
    return doTransition(defaultIssue, issueChangeContext, transition.getKey());
  }

  public boolean doTransition(DefaultIssue defaultIssue, IssueChangeContext issueChangeContext, String transitionKey) {
    return workflow.doManualTransition(defaultIssue, transitionKey, issueChangeContext);
  }

  public void checkTransitionPermission(WorkflowTransition transition, DefaultIssue defaultIssue) {
    checkTransitionPermission(transition.getKey(), defaultIssue);
  }

  public void checkTransitionPermission(String transitionKey, DefaultIssue defaultIssue) {
    String projectUuid = requireNonNull(defaultIssue.projectUuid());
    workflow.outTransitions(defaultIssue)
      .stream()
      .filter(transition -> transition.key().equals(transitionKey) && transition.requiredProjectPermission() != null)
      .forEach(transition -> userSession.checkComponentUuidPermission(transition.requiredProjectPermission(), projectUuid));
  }

}
