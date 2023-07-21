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
import java.util.List;
import java.util.Map;
import org.sonar.ce.task.projectanalysis.component.Component;
import org.sonar.core.issue.AnticipatedTransition;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.tracking.AnticipatedTransitionTracker;
import org.sonar.core.issue.tracking.Tracking;

import static org.sonar.ce.task.projectanalysis.component.Component.Type.FILE;

/**
 * Updates issues if an anticipated transition from SonarLint is found
 */
public class TransitionIssuesToAnticipatedStatesVisitor extends IssueVisitor {

  private Collection<AnticipatedTransition> anticipatedTransitions;
  private final AnticipatedTransitionTracker<DefaultIssue, AnticipatedTransition> tracker = new AnticipatedTransitionTracker<>();
  private final IssueLifecycle issueLifecycle;
  
  private final AnticipatedTransitionRepository anticipatedTransitionRepository;

  public TransitionIssuesToAnticipatedStatesVisitor(AnticipatedTransitionRepository anticipatedTransitionRepository, IssueLifecycle issueLifecycle) {
    this.anticipatedTransitionRepository = anticipatedTransitionRepository;
    this.issueLifecycle = issueLifecycle;
  }

  @Override
  public void beforeComponent(Component component) {
    if (FILE.equals(component.getType())) {
      anticipatedTransitions = anticipatedTransitionRepository.getAnticipatedTransitionByComponent(component);
    }
  }

  @Override
  public void onIssue(Component component, DefaultIssue issue) {
    if (issue.isNew()) {
      Tracking<DefaultIssue, AnticipatedTransition> tracking = tracker.track(List.of(issue), anticipatedTransitions);
      Map<DefaultIssue, AnticipatedTransition> matchedRaws = tracking.getMatchedRaws();
      if (matchedRaws.containsKey(issue)) {
        performAnticipatedTransition(issue, matchedRaws.get(issue));
      }
    }
  }

  private void performAnticipatedTransition(DefaultIssue issue, AnticipatedTransition anticipatedTransition) {
    issue.setBeingClosed(true);
    issue.setAnticipatedTransitions(true);
    issueLifecycle.doManualTransition(issue, anticipatedTransition.getTransition(), anticipatedTransition.getUserUuid());
    issueLifecycle.addComment(issue, anticipatedTransition.getComment(), anticipatedTransition.getUserUuid());
  }

}
