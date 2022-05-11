/*
 * SonarQube
 * Copyright (C) 2009-2022 SonarSource SA
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
package org.sonar.server.pushapi.issues;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs.Diff;
import org.sonar.core.util.issue.Issue;
import org.sonar.core.util.issue.IssueChangedEvent;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;

import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.api.issue.DefaultTransitions.CONFIRM;
import static org.sonar.api.issue.DefaultTransitions.FALSE_POSITIVE;
import static org.sonar.api.issue.DefaultTransitions.UNCONFIRM;
import static org.sonar.api.issue.DefaultTransitions.WONT_FIX;
import static org.sonar.db.component.BranchType.BRANCH;

@ServerSide
public class IssueChangeEventServiceImpl implements IssueChangeEventService {
  private static final String FALSE_POSITIVE_KEY = "FALSE-POSITIVE";
  private static final String WONT_FIX_KEY = "WONTFIX";

  private static final String RESOLUTION_KEY = "resolution";
  private static final String SEVERITY_KEY = "severity";
  private static final String TYPE_KEY = "type";

  private final IssueChangeEventsDistributor eventsDistributor;

  public IssueChangeEventServiceImpl(IssueChangeEventsDistributor eventsDistributor) {
    this.eventsDistributor = eventsDistributor;
  }

  @Override
  public void distributeIssueChangeEvent(DefaultIssue issue, @Nullable String severity, @Nullable String type, @Nullable String transition,
    BranchDto branch, String projectKey) {
    Issue changedIssue = new Issue(issue.key(), branch.getKey());

    Boolean resolved = isResolved(transition);

    if (severity == null && type == null && resolved == null) {
      return;
    }

    IssueChangedEvent event = new IssueChangedEvent(projectKey, new Issue[]{changedIssue},
      resolved, severity, type);
    eventsDistributor.pushEvent(event);
  }

  @Override
  public void distributeIssueChangeEvent(Collection<DefaultIssue> issues, Map<String, ComponentDto> projectsByUuid,
    Map<String, BranchDto> branchesByProjectUuid) {

    for (Entry<String, ComponentDto> entry : projectsByUuid.entrySet()) {
      String projectKey = entry.getValue().getKey();

      Set<DefaultIssue> issuesInProject = issues
        .stream()
        .filter(i -> i.projectUuid().equals(entry.getKey()))
        .collect(Collectors.toSet());

      Issue[] issueChanges = issuesInProject.stream()
        .filter(i -> branchesByProjectUuid.get(i.projectUuid()).getBranchType().equals(BRANCH))
        .map(i -> new Issue(i.key(), branchesByProjectUuid.get(i.projectUuid()).getKey()))
        .toArray(Issue[]::new);

      if (issueChanges.length == 0) {
        continue;
      }

      IssueChangedEvent event = getIssueChangedEvent(projectKey, issuesInProject, issueChanges);

      if (event != null) {
        eventsDistributor.pushEvent(event);
      }
    }
  }

  @CheckForNull
  private static IssueChangedEvent getIssueChangedEvent(String projectKey, Set<DefaultIssue> issuesInProject, Issue[] issueChanges) {
    DefaultIssue firstIssue = issuesInProject.stream().iterator().next();

    if (firstIssue.currentChange() == null) {
      return null;
    }

    Boolean resolved = null;
    String severity = null;
    String type = null;

    boolean isRelevantEvent = false;
    Map<String, Diff> diffs = firstIssue.currentChange().diffs();

    if (diffs.containsKey(RESOLUTION_KEY)) {
      resolved = diffs.get(RESOLUTION_KEY).newValue() == null ? false : isResolved(diffs.get(RESOLUTION_KEY).newValue().toString());
      isRelevantEvent = true;
    }

    if (diffs.containsKey(SEVERITY_KEY)) {
      severity = diffs.get(SEVERITY_KEY).newValue() == null ? null : diffs.get(SEVERITY_KEY).newValue().toString();
      isRelevantEvent = true;
    }

    if (diffs.containsKey(TYPE_KEY)) {
      type = diffs.get(TYPE_KEY).newValue() == null ? null : diffs.get(TYPE_KEY).newValue().toString();
      isRelevantEvent = true;
    }

    if (!isRelevantEvent) {
      return null;
    }

    return new IssueChangedEvent(projectKey, issueChanges, resolved, severity, type);
  }

  @CheckForNull
  private static Boolean isResolved(@Nullable String transitionOrStatus) {
    if (isNullOrEmpty(transitionOrStatus)) {
      return null;
    }

    if (transitionOrStatus.equals(CONFIRM) || transitionOrStatus.equals(UNCONFIRM)) {
      return null;
    }

    return transitionOrStatus.equals(WONT_FIX) || transitionOrStatus.equals(FALSE_POSITIVE) ||
      transitionOrStatus.equals(FALSE_POSITIVE_KEY) || transitionOrStatus.equals(WONT_FIX_KEY);
  }
}
