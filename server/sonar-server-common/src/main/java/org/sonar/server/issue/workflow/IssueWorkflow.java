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
package org.sonar.server.issue.workflow;

import java.util.List;
import org.sonar.api.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.status.IssueStatus;
import org.sonar.server.issue.IssueFieldsSetter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FALSE_POSITIVE;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.RESOLUTION_WONT_FIX;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_CONFIRMED;
import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.api.issue.Issue.STATUS_REOPENED;
import static org.sonar.api.issue.Issue.STATUS_RESOLVED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;

@ServerSide
@ComputeEngineSide
public class IssueWorkflow implements Startable {

  private static final String AUTOMATIC_CLOSE_TRANSITION = "automaticclose";
  private final FunctionExecutor functionExecutor;
  private final IssueFieldsSetter updater;
  private StateMachine machine;

  public IssueWorkflow(FunctionExecutor functionExecutor, IssueFieldsSetter updater) {
    this.functionExecutor = functionExecutor;
    this.updater = updater;
  }

  @Override
  public void start() {
    StateMachine.Builder builder = StateMachine.builder()
      .states(STATUS_OPEN, STATUS_CONFIRMED, STATUS_REOPENED, STATUS_RESOLVED, STATUS_CLOSED,
        STATUS_TO_REVIEW, STATUS_REVIEWED);
    buildManualTransitions(builder);
    buildAutomaticTransitions(builder);
    buildSecurityHotspotTransitions(builder);
    machine = builder.build();
  }

  private static void buildManualTransitions(StateMachine.Builder builder) {
    builder
      //replacement transition for org.sonar.api.issue.DefaultTransitions.WONT_FIX
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())

      // resolve as false-positive
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())

      // reopen
      .transition(Transition.builder(DefaultTransitions.UNCONFIRM)
        .from(STATUS_CONFIRMED).to(STATUS_REOPENED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(STATUS_RESOLVED).to(STATUS_REOPENED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(null))
        .build())

      // confirm
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(STATUS_OPEN).to(STATUS_CONFIRMED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(STATUS_REOPENED).to(STATUS_CONFIRMED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(null))
        .build())

      // resolve as fixed
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_FIXED))
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())

      // resolve as won't fix, deprecated
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(STATUS_OPEN).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(STATUS_REOPENED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(STATUS_CONFIRMED).to(STATUS_RESOLVED)
        .conditions(IsNotHotspot.INSTANCE)
        .functions(new SetResolution(RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build());

  }

  private static void buildSecurityHotspotTransitions(StateMachine.Builder builder) {
    // hotspot reviewed as fixed, either from TO_REVIEW or from REVIEWED-SAFE or from REVIEWED-ACKNOWLEDGED
    Transition.TransitionBuilder reviewedAsFixedBuilder = Transition.builder(DefaultTransitions.RESOLVE_AS_REVIEWED)
      .to(STATUS_REVIEWED)
      .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
      .functions(new SetResolution(RESOLUTION_FIXED))
      .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN);
    builder
      .transition(reviewedAsFixedBuilder
        .from(STATUS_TO_REVIEW)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .build())
      .transition(reviewedAsFixedBuilder
        .from(STATUS_REVIEWED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED))
        .build());

    // hotspot reviewed as safe, either from TO_REVIEW or from REVIEWED-FIXED or from REVIEWED-ACKNOWLEDGED
    Transition.TransitionBuilder resolveAsSafeTransitionBuilder = Transition.builder(DefaultTransitions.RESOLVE_AS_SAFE)
      .to(STATUS_REVIEWED)
      .functions(new SetResolution(RESOLUTION_SAFE))
      .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN);
    builder
      .transition(resolveAsSafeTransitionBuilder
        .from(STATUS_TO_REVIEW)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .build())
      .transition(resolveAsSafeTransitionBuilder
        .from(STATUS_REVIEWED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(RESOLUTION_FIXED, RESOLUTION_ACKNOWLEDGED))
        .build());

    // hotspot reviewed as acknowledged, either from TO_REVIEW or from REVIEWED-FIXED or from REVIEWED-SAFE
    Transition.TransitionBuilder resolveAsAcknowledgedTransitionBuilder = Transition.builder(DefaultTransitions.RESOLVE_AS_ACKNOWLEDGED)
      .to(STATUS_REVIEWED)
      .functions(new SetResolution(RESOLUTION_ACKNOWLEDGED))
      .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN);
    builder
      .transition(resolveAsAcknowledgedTransitionBuilder
        .from(STATUS_TO_REVIEW)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .build())
      .transition(resolveAsAcknowledgedTransitionBuilder
        .from(STATUS_REVIEWED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(RESOLUTION_FIXED, RESOLUTION_SAFE))
        .build());

    // put hotspot back into TO_REVIEW
    builder
      .transition(Transition.builder(DefaultTransitions.RESET_AS_TO_REVIEW)
        .from(STATUS_REVIEWED).to(STATUS_TO_REVIEW)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(RESOLUTION_FIXED, RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED))
        .functions(new SetResolution(null))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build());
  }

  private static void buildAutomaticTransitions(StateMachine.Builder builder) {
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
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_TO_REVIEW).to(STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE, new HasType(RuleType.SECURITY_HOTSPOT))
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_REVIEWED).to(STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE, new HasType(RuleType.SECURITY_HOTSPOT))
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())

      // Reopen issues that are marked as resolved but that are still alive.
      .transition(Transition.builder("automaticreopen")
        .from(STATUS_RESOLVED).to(STATUS_REOPENED)
        .conditions(new NotCondition(IsBeingClosed.INSTANCE), new HasResolution(RESOLUTION_FIXED), IsNotHotspot.INSTANCE)
        .functions(new SetResolution(null), UnsetCloseDate.INSTANCE)
        .automatic()
        .build())

      .transition(Transition.builder("automaticuncloseopen")
        .from(STATUS_CLOSED).to(STATUS_OPEN)
        .conditions(
          new PreviousStatusWas(STATUS_OPEN),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED),
          IsNotHotspot.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticunclosereopen")
        .from(STATUS_CLOSED).to(STATUS_REOPENED)
        .conditions(
          new PreviousStatusWas(STATUS_REOPENED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED),
          IsNotHotspot.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseconfirmed")
        .from(STATUS_CLOSED).to(STATUS_CONFIRMED)
        .conditions(
          new PreviousStatusWas(STATUS_CONFIRMED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED),
          IsNotHotspot.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseresolved")
        .from(STATUS_CLOSED).to(STATUS_RESOLVED)
        .conditions(
          new PreviousStatusWas(STATUS_RESOLVED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED),
          IsNotHotspot.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())

      // reopen closed hotspots
      .transition(Transition.builder("automaticunclosetoreview")
        .from(STATUS_CLOSED).to(STATUS_TO_REVIEW)
        .conditions(
          new PreviousStatusWas(STATUS_TO_REVIEW),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED),
          IsHotspot.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticunclosereviewed")
        .from(STATUS_CLOSED).to(STATUS_REVIEWED)
        .conditions(
          new PreviousStatusWas(STATUS_REVIEWED),
          new HasResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED),
          IsHotspot.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build());
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public boolean doManualTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).transition(transitionKey);
    if (transition.supports(issue) && !transition.automatic()) {
      IssueStatus previousIssueStatus = issue.getIssueStatus();
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
      updater.setIssueStatus(issue, previousIssueStatus, issue.getIssueStatus(), issueChangeContext);
      return true;
    }
    return false;
  }

  public List<Transition> outTransitions(Issue issue) {
    String status = issue.status();
    State state = machine.state(status);
    checkArgument(state != null, "Unknown status: %s", status);
    return state.outManualTransitions(issue);
  }

  public void doAutomaticTransition(DefaultIssue issue, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).outAutomaticTransition(issue);
    if (transition != null) {
      IssueStatus previousIssueStatus = issue.getIssueStatus();
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
      updater.setIssueStatus(issue, previousIssueStatus, issue.getIssueStatus(), issueChangeContext);
    }
  }

  public List<String> statusKeys() {
    return machine.stateKeys();
  }

  private State stateOf(DefaultIssue issue) {
    String status = issue.status();
    State state = machine.state(status);
    String issueKey = issue.key();
    checkState(state != null, "Unknown status: %s [issue=%s]", status, issueKey);
    return state;
  }

}
