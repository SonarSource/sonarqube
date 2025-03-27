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
package org.sonar.server.issue.workflow;

import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.issue.TaintChecker;

import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;

@ServerSide
@ComputeEngineSide
public class CodeQualityIssueWorkflow {

  private static final String AUTOMATIC_CLOSE_TRANSITION = "automaticclose";
  private final TaintChecker taintChecker;
  private final StateMachine machine;

  public CodeQualityIssueWorkflow(TaintChecker taintChecker) {
    this.taintChecker = taintChecker;
    StateMachine.Builder builder = StateMachine.builder()
      .states(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED);
    buildManualTransitions(builder);
    buildAutomaticTransitions(builder);
    machine = builder.build();
  }

  public StateMachine getMachine() {
    return machine;
  }

  private static void buildManualTransitions(StateMachine.Builder builder) {
    builder
      // replacement transition for org.sonar.api.issue.DefaultTransitions.WONT_FIX
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())

      // resolve as false-positive
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())

      // reopen
      .transition(Transition.builder(DefaultTransitions.UNCONFIRM)
        .from(STATUS_CONFIRMED).to(STATUS_REOPENED)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(STATUS_RESOLVED).to(STATUS_REOPENED)
        .functions(new SetResolution(null))
        .build())

      // confirm
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(STATUS_OPEN).to(STATUS_CONFIRMED)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(STATUS_REOPENED).to(STATUS_CONFIRMED)
        .functions(new SetResolution(null))
        .build())

      // resolve as fixed
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())

      // resolve as won't fix, deprecated
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build());

  }

  private void buildAutomaticTransitions(StateMachine.Builder builder) {
    // Close the "end of life" issues (disabled/deleted rule, deleted component)
    builder
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_OPEN).to(STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_REOPENED).to(STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_CONFIRMED).to(STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_RESOLVED).to(STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      // Reopen issues that are marked as resolved but that are still alive.
      .transition(Transition.builder("automaticreopen")
        .from(STATUS_RESOLVED).to(STATUS_REOPENED)
        .conditions(new NotCondition(IsBeingClosed.INSTANCE), new HasResolution(RESOLUTION_FIXED))
        .functions(new SetResolution(null), UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseopen")
        .from(STATUS_CLOSED).to(STATUS_OPEN)
        .conditions(
          new PreviousStatusWas(STATUS_OPEN),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticunclosereopen")
        .from(STATUS_CLOSED).to(STATUS_REOPENED)
        .conditions(
          new PreviousStatusWas(STATUS_REOPENED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseconfirmed")
        .from(STATUS_CLOSED).to(STATUS_CONFIRMED)
        .conditions(
          new PreviousStatusWas(STATUS_CONFIRMED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseresolved")
        .from(STATUS_CLOSED).to(STATUS_RESOLVED)
        .conditions(
          new PreviousStatusWas(STATUS_RESOLVED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(reopenTaintVulnOnFlowChanged());
  }

  private Transition reopenTaintVulnOnFlowChanged() {
    return Transition.builder("reopentaintvulnerability")
      .from(STATUS_RESOLVED)
      .to(STATUS_REOPENED)
      .conditions(
        issue -> taintChecker.isTaintVulnerability((DefaultIssue) issue),
        issue -> ((DefaultIssue) issue).locationsChanged())
      .functions(
        Function.Context::unsetCloseDate,
        context -> context.setResolution(null),
        CodeQualityIssueWorkflow::commentOnTaintVulnReopened)
      .automatic()
      .build();
  }

  private static void commentOnTaintVulnReopened(Function.Context context) {
    DefaultIssue issue = (DefaultIssue) context.issue();
    DefaultIssueComment defaultIssueComment = DefaultIssueComment.create(issue.key(), "Automatically reopened because the vulnerability flow changed.");
    issue.addComment(defaultIssueComment);
  }

}
