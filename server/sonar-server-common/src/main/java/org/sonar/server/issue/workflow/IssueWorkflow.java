/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import org.picocontainer.Startable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.DefaultTransitions;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.server.issue.IssueFieldsSetter;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

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
      // order is important for UI
      .states(Issue.STATUS_OPEN, Issue.STATUS_CONFIRMED, Issue.STATUS_REOPENED, Issue.STATUS_RESOLVED, Issue.STATUS_CLOSED);

    buildManualTransitions(builder);
    buildAutomaticTransitions(builder);
    buildSecurityHotspotTransitions(builder);
    machine = builder.build();
  }

  private static void buildManualTransitions(StateMachine.Builder builder) {
    builder
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_CONFIRMED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.CONFIRM)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_CONFIRMED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.UNCONFIRM)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_REOPENED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(null))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.RESOLVE)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(null))
        .build())

      // resolve as false-positive
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.FALSE_POSITIVE)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_FALSE_POSITIVE), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())

      // resolve as won't fix
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.WONT_FIX)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_RESOLVED)
        .conditions(IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX), UnsetAssignee.INSTANCE)
        .requiredProjectPermission(UserRole.ISSUE_ADMIN)
        .build());
  }

  private static void buildSecurityHotspotTransitions(StateMachine.Builder builder) {
    builder
      .transition(Transition.builder(DefaultTransitions.DETECT)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_OPEN)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .functions(new SetType(RuleType.VULNERABILITY))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.DETECT)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_OPEN)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .functions(new SetType(RuleType.VULNERABILITY))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.DETECT)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_OPEN)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(Issue.RESOLUTION_WONT_FIX))
        .functions(new SetType(RuleType.VULNERABILITY), new SetResolution(null))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.DISMISS)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_REOPENED)
        .conditions(IsManualVulnerability.INSTANCE)
        .functions(new SetType(RuleType.SECURITY_HOTSPOT))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.REQUEST_REVIEW)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(IsManualVulnerability.INSTANCE)
        .functions(new SetType(RuleType.SECURITY_HOTSPOT), new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.REQUEST_REVIEW)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(IsManualVulnerability.INSTANCE)
        .functions(new SetType(RuleType.SECURITY_HOTSPOT), new SetResolution(Issue.RESOLUTION_FIXED))
        .build())
      .transition(Transition.builder(DefaultTransitions.REJECT)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(Issue.RESOLUTION_FIXED))
        .functions(new SetType(RuleType.VULNERABILITY), new SetResolution(null))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.ACCEPT)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_RESOLVED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(Issue.RESOLUTION_FIXED))
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.CLEAR)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_RESOLVED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.CLEAR)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_RESOLVED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT))
        .functions(new SetResolution(Issue.RESOLUTION_WONT_FIX))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build())
      .transition(Transition.builder(DefaultTransitions.REOPEN_HOTSPOT)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .conditions(new HasType(RuleType.SECURITY_HOTSPOT), new HasResolution(Issue.RESOLUTION_WONT_FIX))
        .functions(new SetResolution(null))
        .requiredProjectPermission(UserRole.SECURITYHOTSPOT_ADMIN)
        .build());

  }

  private static void buildAutomaticTransitions(StateMachine.Builder builder) {
    // Close the "end of life" issues (disabled/deleted rule, deleted component)
    builder
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(Issue.STATUS_OPEN).to(Issue.STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(Issue.STATUS_REOPENED).to(Issue.STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(Issue.STATUS_CONFIRMED).to(Issue.STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_CLOSED)
        .conditions(IsBeingClosed.INSTANCE)
        .functions(SetClosed.INSTANCE, SetCloseDate.INSTANCE)
        .automatic()
        .build())

      // Reopen issues that are marked as resolved but that are still alive.
      .transition(Transition.builder("automaticreopen")
        .from(Issue.STATUS_RESOLVED).to(Issue.STATUS_REOPENED)
        .conditions(new NotCondition(IsBeingClosed.INSTANCE), new HasResolution(Issue.RESOLUTION_FIXED), IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(new SetResolution(null), UnsetCloseDate.INSTANCE)
        .automatic()
        .build())

      .transition(Transition.builder("automaticuncloseopen")
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_OPEN)
        .conditions(
          new PreviousStatusWas(Issue.STATUS_OPEN),
          new HasResolution(Issue.RESOLUTION_REMOVED, Issue.RESOLUTION_FIXED),
          IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticunclosereopen")
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_REOPENED)
        .conditions(
          new PreviousStatusWas(Issue.STATUS_REOPENED),
          new HasResolution(Issue.RESOLUTION_REMOVED, Issue.RESOLUTION_FIXED),
          IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseconfirmed")
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_CONFIRMED)
        .conditions(
          new PreviousStatusWas(Issue.STATUS_CONFIRMED),
          new HasResolution(Issue.RESOLUTION_REMOVED, Issue.RESOLUTION_FIXED),
          IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())
      .transition(Transition.builder("automaticuncloseresolved")
        .from(Issue.STATUS_CLOSED).to(Issue.STATUS_RESOLVED)
        .conditions(
          new PreviousStatusWas(Issue.STATUS_RESOLVED),
          new HasResolution(Issue.RESOLUTION_REMOVED, Issue.RESOLUTION_FIXED),
          IsNotHotspotNorManualVulnerability.INSTANCE)
        .functions(RestoreResolutionFunction.INSTANCE, UnsetCloseDate.INSTANCE)
        .automatic()
        .build())

    ;
  }

  @Override
  public void stop() {
    // nothing to do
  }

  public boolean doManualTransition(DefaultIssue issue, String transitionKey, IssueChangeContext issueChangeContext) {
    Transition transition = stateOf(issue).transition(transitionKey);
    if (!transition.automatic()) {
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
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
      functionExecutor.execute(transition.functions(), issue, issueChangeContext);
      updater.setStatus(issue, transition.to(), issueChangeContext);
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

  StateMachine machine() {
    return machine;
  }
}
