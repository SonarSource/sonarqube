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

import java.util.function.Consumer;
import java.util.function.Predicate;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.issue.workflow.statemachine.StateMachine;
import org.sonar.server.issue.workflow.statemachine.Transition;

import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.ACCEPT;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.CONFIRM;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.FALSE_POSITIVE;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.REOPEN;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.RESOLVE;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.UNCONFIRM;
import static org.sonar.server.issue.workflow.codequalityissue.CodeQualityIssueWorkflowTransition.WONT_FIX;

@ServerSide
@ComputeEngineSide
public class CodeQualityIssueWorkflowDefinition {

  private static final String AUTOMATIC_CLOSE_TRANSITION = "automaticclose";

  private static final Consumer<CodeQualityIssueWorkflowActions> SET_CLOSE_DATE = CodeQualityIssueWorkflowActions::setCloseDate;
  private static final Consumer<CodeQualityIssueWorkflowActions> SET_CLOSED = CodeQualityIssueWorkflowActions::setClosed;
  private static final Consumer<CodeQualityIssueWorkflowActions> UNSET_ASSIGNEE = CodeQualityIssueWorkflowActions::unsetAssignee;
  private static final Consumer<CodeQualityIssueWorkflowActions> UNSET_CLOSE_DATE = CodeQualityIssueWorkflowActions::unsetCloseDate;
  private static final Consumer<CodeQualityIssueWorkflowActions> UNSET_RESOLUTION = CodeQualityIssueWorkflowActions::unsetResolution;
  private static final Consumer<CodeQualityIssueWorkflowActions> RESTORE_RESOLUTION = CodeQualityIssueWorkflowActions::restoreResolution;

  private final StateMachine<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> machine;

  public CodeQualityIssueWorkflowDefinition() {
    StateMachine.Builder<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> builder = StateMachine
      .<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder()
      .states(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED);
    buildManualTransitions(builder);
    buildAutomaticTransitions(builder);
    machine = builder.build();
  }

  public StateMachine<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> getMachine() {
    return machine;
  }

  private static void buildManualTransitions(StateMachine.Builder<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> builder) {
    builder
      // replacement transition for org.sonar.api.issue.Transitions.WONT_FIX
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(ACCEPT.getKey())
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_WONT_FIX), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(ACCEPT.getKey())
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_WONT_FIX), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(ACCEPT.getKey())
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_WONT_FIX), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())

      // resolve as false-positive
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(FALSE_POSITIVE.getKey())
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_FALSE_POSITIVE), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(FALSE_POSITIVE.getKey())
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_FALSE_POSITIVE), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(FALSE_POSITIVE.getKey())
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_FALSE_POSITIVE), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())

      // reopen
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(UNCONFIRM.getKey())
        .from(STATUS_CONFIRMED).to(STATUS_REOPENED)
        .actions(UNSET_RESOLUTION)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(REOPEN.getKey())
        .from(STATUS_RESOLVED).to(STATUS_REOPENED)
        .actions(UNSET_RESOLUTION)
        .build())

      // confirm
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(CONFIRM.getKey())
        .from(STATUS_OPEN).to(STATUS_CONFIRMED)
        .actions(UNSET_RESOLUTION)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(CONFIRM.getKey())
        .from(STATUS_REOPENED).to(STATUS_CONFIRMED)
        .actions(UNSET_RESOLUTION)
        .build())

      // resolve as fixed
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(RESOLVE.getKey())
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(RESOLVE.getKey())
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(RESOLVE.getKey())
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())

      // resolve as won't fix, deprecated
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(WONT_FIX.getKey())
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_WONT_FIX), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(WONT_FIX.getKey())
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_WONT_FIX), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(WONT_FIX.getKey())
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .actions(a -> a.setResolution(RESOLUTION_WONT_FIX), UNSET_ASSIGNEE)
        .requiredProjectPermission(ProjectPermission.ISSUE_ADMIN)
        .build());

  }

  private static void buildAutomaticTransitions(StateMachine.Builder<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> builder) {
    // Close the "end of life" issues (disabled/deleted rule, deleted component)
    builder
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_OPEN).to(STATUS_CLOSED)
        .conditions(CodeQualityIssueWorkflowEntity::isBeingClosed)
        .actions(SET_CLOSED, SET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_REOPENED).to(STATUS_CLOSED)
        .conditions(CodeQualityIssueWorkflowEntity::isBeingClosed)
        .actions(SET_CLOSED, SET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_CONFIRMED).to(STATUS_CLOSED)
        .conditions(CodeQualityIssueWorkflowEntity::isBeingClosed)
        .actions(SET_CLOSED, SET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_RESOLVED).to(STATUS_CLOSED)
        .conditions(CodeQualityIssueWorkflowEntity::isBeingClosed)
        .actions(SET_CLOSED, SET_CLOSE_DATE)
        .automatic()
        .build())
      // Reopen issues that are marked as resolved but that are still alive.
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder("automaticreopen")
        .from(STATUS_RESOLVED).to(STATUS_REOPENED)
        .conditions(
          Predicate.not(CodeQualityIssueWorkflowEntity::isBeingClosed),
          i -> i.hasAnyResolution(RESOLUTION_FIXED))
        .actions(UNSET_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder("automaticuncloseopen")
        .from(STATUS_CLOSED).to(STATUS_OPEN)
        .conditions(
          i -> i.previousStatusWas(STATUS_OPEN),
          i -> i.hasAnyResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .actions(RESTORE_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder("automaticunclosereopen")
        .from(STATUS_CLOSED).to(STATUS_REOPENED)
        .conditions(
          i -> i.previousStatusWas(STATUS_REOPENED),
          i -> i.hasAnyResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .actions(RESTORE_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder("automaticuncloseconfirmed")
        .from(STATUS_CLOSED).to(STATUS_CONFIRMED)
        .conditions(
          i -> i.previousStatusWas(STATUS_CONFIRMED),
          i -> i.hasAnyResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .actions(RESTORE_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder("automaticuncloseresolved")
        .from(STATUS_CLOSED).to(STATUS_RESOLVED)
        .conditions(
          i -> i.previousStatusWas(STATUS_RESOLVED),
          i -> i.hasAnyResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .actions(RESTORE_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(reopenTaintVulnOnFlowChanged());
  }

  private static Transition<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions> reopenTaintVulnOnFlowChanged() {
    return Transition.<CodeQualityIssueWorkflowEntity, CodeQualityIssueWorkflowActions>builder("reopentaintvulnerability")
      .from(STATUS_RESOLVED)
      .to(STATUS_REOPENED)
      .conditions(
        CodeQualityIssueWorkflowEntity::isTaintVulnerability,
        CodeQualityIssueWorkflowEntity::locationsChanged)
      .actions(
        UNSET_CLOSE_DATE,
        UNSET_RESOLUTION,
        a -> a.addComment("Automatically reopened because the vulnerability flow changed."))
      .automatic()
      .build();
  }

}
