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
package org.sonar.server.issue.workflow.codequalityissue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.IssueStatus;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.TaintChecker;
import org.sonar.server.issue.workflow.issue.IssueWorkflowEntityAdapter;
import org.sonar.server.issue.workflow.statemachine.State;
import org.sonar.server.issue.workflow.statemachine.Transition;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

@ServerSide
@ComputeEngineSide
public class CodeQualityIssueWorkflow {

  private final CodeQualityIssueWorkflowActionsFactory actionsFactory;
  private final CodeQualityIssueWorkflowDefinition workflowDefinition;
  private final TaintChecker taintChecker;

  public CodeQualityIssueWorkflow(CodeQualityIssueWorkflowActionsFactory actionsFactory, CodeQualityIssueWorkflowDefinition workflowDefinition,
    TaintChecker taintChecker) {
    this.actionsFactory = actionsFactory;
    this.workflowDefinition = workflowDefinition;
    this.taintChecker = taintChecker;
  }

  public boolean doManualTransition(DefaultIssue issue, CodeQualityIssueWorkflowTransition transition, IssueChangeContext issueChangeContext) {
    return doManualTransition(issue, transition.getKey(), issueChangeContext);
  }

  public boolean doManualTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> transition = stateOf(issue).transition(transitionKey);
    var wfIssue = adapt(issue);
    if (transition.supports(wfIssue) && !transition.automatic()) {
      IssueStatus previousIssueStatus = issue.issueStatus();
      CodeQualityIssueWorkflowActions issueActions = actionsFactory.provideContextualActions(issue, issueChangeContext);
      execute(transition.actions(), issueActions);
      issueActions.setStatus(previousIssueStatus, transition.to());
      return true;
    }
    return false;
  }

  public Set<CodeQualityIssueWorkflowTransition> outTransitionsEnums(DefaultIssue issue) {
    return outTransitions(issue).stream().map(Transition::key)
      .map(CodeQualityIssueWorkflowTransition::fromKey)
      .flatMap(Optional::stream)
      .collect(Collectors.toSet());
  }

  public List<Transition> outTransitions(DefaultIssue issue) {
    String status = issue.status();
    State<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> state = workflowDefinition.getMachine().state(status);
    checkArgument(state != null, "Unknown status: %s", status);
    return state.outManualTransitions(adapt(issue)).stream().map(t -> (Transition) t).toList();
  }

  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    var wfIssue = adapt(issue);
    Transition<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> transition = stateOf(issue).outAutomaticTransition(wfIssue);
    if (transition != null) {
      IssueStatus previousIssueStatus = issue.issueStatus();
      CodeQualityIssueWorkflowActions issueActions = actionsFactory.provideContextualActions(issue, issueChangeContext);
      execute(transition.actions(), issueActions);
      issueActions.setStatus(previousIssueStatus, transition.to());
    }
  }

  private State<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> stateOf(DefaultIssue issue) {
    State<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> state = workflowDefinition.getMachine().state(issue.status());
    checkState(state != null, "Unknown status: %s [issue=%s]", issue.status(), issue.key());
    return state;
  }

  void execute(List<Consumer<CodeQualityIssueWorkflowActions>> actions, CodeQualityIssueWorkflowActions issueActions) {
    if (!actions.isEmpty()) {
      for (Consumer<CodeQualityIssueWorkflowActions> action : actions) {
        action.accept(issueActions);
      }
    }
  }

  private CodeQualityIssueWorkflowEntity adapt(DefaultIssue issue) {
    return new CodeQualityIssueWorkflowEntityAdapter(issue);
  }

  private class CodeQualityIssueWorkflowEntityAdapter extends IssueWorkflowEntityAdapter implements CodeQualityIssueWorkflowEntity {

    protected CodeQualityIssueWorkflowEntityAdapter(DefaultIssue issue) {
      super(issue);
    }

    @Override
    public boolean locationsChanged() {
      return issue.locationsChanged();
    }

    @Override
    public boolean isTaintVulnerability() {
      return taintChecker.isTaintVulnerability(issue);
    }
  }

}
