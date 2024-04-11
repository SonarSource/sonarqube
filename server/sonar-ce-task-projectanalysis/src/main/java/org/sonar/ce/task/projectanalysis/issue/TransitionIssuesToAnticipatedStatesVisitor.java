/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.ce.task.log.CeTaskMessages;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.AnticipatedTransitionTracker;
import org.sonar.core.issue.tracking.Tracking;
import org.sonar.db.dismissmessage.MessageType;

import static org.sonar.api.issue.Issue.STATUS_OPEN;
import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;

/**
 * Updates issues if an anticipated transition from SonarLint is found
 */
public class TransitionIssuesToAnticipatedStatesVisitor extends IssueVisitor {

  private static final Logger LOGGER = LoggerFactory.getLogger(TransitionIssuesToAnticipatedStatesVisitor.class);
  public static final String TRANSITION_ERROR_TEMPLATE = "Cannot resolve issue at line {} of {} due to: {}";

  private Collection<AnticipatedTransition> anticipatedTransitions;
  private final AnticipatedTransitionTracker<DefaultIssue, AnticipatedTransition> tracker = new AnticipatedTransitionTracker<>();
  private final IssueLifecycle issueLifecycle;

  private final CeTaskMessages ceTaskMessages;

  private final AnticipatedTransitionRepository anticipatedTransitionRepository;

  public TransitionIssuesToAnticipatedStatesVisitor(AnticipatedTransitionRepository anticipatedTransitionRepository,
    IssueLifecycle issueLifecycle, CeTaskMessages ceTaskMessages) {
    this.anticipatedTransitionRepository = anticipatedTransitionRepository;
    this.issueLifecycle = issueLifecycle;
    this.ceTaskMessages = ceTaskMessages;
  }

  @Override
  public void beforeComponent(Component component) {
    if (FILE.equals(component.getType())) {
      anticipatedTransitions = anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component);
    } else {
      anticipatedTransitions = Collections.emptyList();
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (isEligibleForAnticipatedTransitions(issue)) {
      Tracking<DefaultIssue, AnticipatedTransition> tracking = tracker.track(List.of(issue), anticipatedTransitions);
      Map<DefaultIssue, AnticipatedTransition> matchedRaws = tracking.getMatchedRaws();
      if (matchedRaws.containsKey(issue)) {
        performAnticipatedTransition(issue, matchedRaws.get(issue));
      }
    }
  }

  private static boolean isEligibleForAnticipatedTransitions(DefaultIssue issue) {
    return issue.isNew() && STATUS_OPEN.equals(issue.getStatus()) && null == issue.resolution();
  }

  private void performAnticipatedTransition(DefaultIssue issue, AnticipatedTransition anticipatedTransition) {
    try {
      issueLifecycle.doManualTransition(issue, anticipatedTransition.getTransition(), anticipatedTransition.getUserUuid());
      String transitionComment = anticipatedTransition.getComment();
      String comment = Strings.isNotBlank(transitionComment) ? transitionComment : "Automatically transitioned from SonarLint";
      issueLifecycle.addComment(issue, comment, anticipatedTransition.getUserUuid());
      issue.setBeingClosed(true);
      issue.setAnticipatedTransitionUuid(anticipatedTransition.getUuid());
    } catch (Exception e) {
      LOGGER.warn(TRANSITION_ERROR_TEMPLATE, issue.getLine(), issue.componentKey(), e.getMessage());
      ceTaskMessages.add(
        new CeTaskMessages.Message(getMessage(issue, e),
          Instant.now().toEpochMilli(),
          MessageType.GENERIC));
    }
  }

  private static String getMessage(DefaultIssue issue, Exception e) {
    final int MAX_LENGTH = 50;
    int componentKeyLength = issue.componentKey().length();
    String componentKey = componentKeyLength > MAX_LENGTH ? ("..." + issue.componentKey().substring(componentKeyLength - MAX_LENGTH, componentKeyLength)) : issue.componentKey();
    return String.format(TRANSITION_ERROR_TEMPLATE.replace("{}", "%s"), issue.getLine(), componentKey, e.getMessage());
  }
}
