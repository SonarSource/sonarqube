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
package org.sonar.server.issue.workflow.securityhotspot;

import java.util.function.Consumer;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.db.permission.ProjectPermission;
import org.sonar.server.issue.workflow.statemachine.StateMachine;
import org.sonar.server.issue.workflow.statemachine.Transition;

import static org.sonar.api.issue.Issue.RESOLUTION_ACKNOWLEDGED;
import static org.sonar.api.issue.Issue.RESOLUTION_FIXED;
import static org.sonar.api.issue.Issue.RESOLUTION_REMOVED;
import static org.sonar.api.issue.Issue.RESOLUTION_SAFE;
import static org.sonar.api.issue.Issue.STATUS_CLOSED;
import static org.sonar.api.issue.Issue.STATUS_REVIEWED;
import static org.sonar.api.issue.Issue.STATUS_TO_REVIEW;
import static org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition.RESET_AS_TO_REVIEW;
import static org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition.RESOLVE_AS_ACKNOWLEDGED;
import static org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition.RESOLVE_AS_REVIEWED;
import static org.sonar.server.issue.workflow.securityhotspot.SecurityHotspotWorkflowTransition.RESOLVE_AS_SAFE;

@ServerSide
@ComputeEngineSide
public class SecurityHotspotWorkflowDefinition {

  private static final String AUTOMATIC_CLOSE_TRANSITION = "automaticclose";
  public static final String AUTOMATIC_UNCLOSE_TO_REVIEW = "automaticunclosetoreview";
  public static final String AUTOMATIC_UNCLOSE_REVIEWED = "automaticunclosereviewed";

  private static final Consumer<SecurityHotspotWorkflowActions> SET_CLOSE_DATE = SecurityHotspotWorkflowActions::setCloseDate;
  private static final Consumer<SecurityHotspotWorkflowActions> SET_CLOSED = SecurityHotspotWorkflowActions::setClosed;
  private static final Consumer<SecurityHotspotWorkflowActions> UNSET_CLOSE_DATE = SecurityHotspotWorkflowActions::unsetCloseDate;
  private static final Consumer<SecurityHotspotWorkflowActions> UNSET_RESOLUTION = SecurityHotspotWorkflowActions::unsetResolution;
  private static final Consumer<SecurityHotspotWorkflowActions> RESTORE_RESOLUTION = SecurityHotspotWorkflowActions::restoreResolution;
  private final StateMachine<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> machine;

  public SecurityHotspotWorkflowDefinition() {
    StateMachine.Builder<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> builder = StateMachine
      .<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder()
      .states(STATUS_TO_REVIEW, STATUS_REVIEWED, STATUS_CLOSED);
    buildManualTransitions(builder);
    buildAutomaticTransitions(builder);
    machine = builder.build();
  }

  public StateMachine<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> getMachine() {
    return machine;
  }

  private static void buildManualTransitions(StateMachine.Builder<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> builder) {
    // hotspot reviewed as fixed, either from TO_REVIEW or from REVIEWED-SAFE or from REVIEWED-ACKNOWLEDGED
    Transition.TransitionBuilder<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> reviewedAsFixedBuilder = Transition
      .<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(RESOLVE_AS_REVIEWED.getKey())
      .to(STATUS_REVIEWED)
      .actions(a -> a.setResolution(RESOLUTION_FIXED))
      .requiredProjectPermission(ProjectPermission.SECURITYHOTSPOT_ADMIN);
    builder
      .transition(reviewedAsFixedBuilder
        .from(STATUS_TO_REVIEW)
        .build())
      .transition(reviewedAsFixedBuilder
        .from(STATUS_REVIEWED)
        .conditions(sh -> sh.hasAnyResolution(RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED))
        .build());

    // hotspot reviewed as safe, either from TO_REVIEW or from REVIEWED-FIXED or from REVIEWED-ACKNOWLEDGED
    Transition.TransitionBuilder<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> resolveAsSafeTransitionBuilder = Transition
      .<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(RESOLVE_AS_SAFE.getKey())
      .to(STATUS_REVIEWED)
      .actions(a -> a.setResolution(RESOLUTION_SAFE))
      .requiredProjectPermission(ProjectPermission.SECURITYHOTSPOT_ADMIN);
    builder
      .transition(resolveAsSafeTransitionBuilder
        .from(STATUS_TO_REVIEW)
        .build())
      .transition(resolveAsSafeTransitionBuilder
        .from(STATUS_REVIEWED)
        .conditions(sh -> sh.hasAnyResolution(RESOLUTION_FIXED, RESOLUTION_ACKNOWLEDGED))
        .build());

    // hotspot reviewed as acknowledged, either from TO_REVIEW or from REVIEWED-FIXED or from REVIEWED-SAFE
    Transition.TransitionBuilder<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> resolveAsAcknowledgedTransitionBuilder = Transition
      .<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(RESOLVE_AS_ACKNOWLEDGED.getKey())
      .to(STATUS_REVIEWED)
      .actions(a -> a.setResolution(RESOLUTION_ACKNOWLEDGED))
      .requiredProjectPermission(ProjectPermission.SECURITYHOTSPOT_ADMIN);
    builder
      .transition(resolveAsAcknowledgedTransitionBuilder
        .from(STATUS_TO_REVIEW)
        .build())
      .transition(resolveAsAcknowledgedTransitionBuilder
        .from(STATUS_REVIEWED)
        .conditions(sh -> sh.hasAnyResolution(RESOLUTION_FIXED, RESOLUTION_SAFE))
        .build());

    // put hotspot back into TO_REVIEW
    builder
      .transition(Transition.<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(RESET_AS_TO_REVIEW.getKey())
        .from(STATUS_REVIEWED).to(STATUS_TO_REVIEW)
        .conditions(sh -> sh.hasAnyResolution(RESOLUTION_FIXED, RESOLUTION_SAFE, RESOLUTION_ACKNOWLEDGED))
        .actions(UNSET_RESOLUTION)
        .requiredProjectPermission(ProjectPermission.SECURITYHOTSPOT_ADMIN)
        .build());
  }

  private static void buildAutomaticTransitions(StateMachine.Builder<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions> builder) {
    builder
      // Close the "end of life" issues (disabled/deleted rule, deleted component)
      .transition(Transition.<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_TO_REVIEW).to(STATUS_CLOSED)
        .conditions(SecurityHotspotWorkflowEntity::isBeingClosed)
        .actions(SET_CLOSED, SET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(AUTOMATIC_CLOSE_TRANSITION)
        .from(STATUS_REVIEWED).to(STATUS_CLOSED)
        .conditions(SecurityHotspotWorkflowEntity::isBeingClosed)
        .actions(SET_CLOSED, SET_CLOSE_DATE)
        .automatic()
        .build())
      // reopen closed hotspots
      .transition(Transition.<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(AUTOMATIC_UNCLOSE_TO_REVIEW)
        .from(STATUS_CLOSED).to(STATUS_TO_REVIEW)
        .conditions(
          sh -> sh.previousStatusWas(STATUS_TO_REVIEW),
          sh -> sh.hasAnyResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .actions(RESTORE_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build())
      .transition(Transition.<SecurityHotspotWorkflowEntity, SecurityHotspotWorkflowActions>builder(AUTOMATIC_UNCLOSE_REVIEWED)
        .from(STATUS_CLOSED).to(STATUS_REVIEWED)
        .conditions(
          sh -> sh.previousStatusWas(STATUS_REVIEWED),
          sh -> sh.hasAnyResolution(RESOLUTION_REMOVED, RESOLUTION_FIXED))
        .actions(RESTORE_RESOLUTION, UNSET_CLOSE_DATE)
        .automatic()
        .build());
  }

}
