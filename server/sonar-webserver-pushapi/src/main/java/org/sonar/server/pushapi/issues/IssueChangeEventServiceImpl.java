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
package org.sonar.server.pushapi.issues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.issue.impact.Severity;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.FieldDiffs;
import org.sonar.core.issue.FieldDiffs.Diff;
import org.sonar.core.util.issue.Issue;
import org.sonar.core.util.issue.IssueChangedEvent;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.pushevent.PushEventDto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.db.component.BranchType.BRANCH;
import static org.sonar.server.issue.workflow.CodeQualityIssueWorkflowTransition.ACCEPT;
import static org.sonar.server.issue.workflow.CodeQualityIssueWorkflowTransition.CONFIRM;
import static org.sonar.server.issue.workflow.CodeQualityIssueWorkflowTransition.FALSE_POSITIVE;
import static org.sonar.server.issue.workflow.CodeQualityIssueWorkflowTransition.UNCONFIRM;
import static org.sonar.server.issue.workflow.CodeQualityIssueWorkflowTransition.WONT_FIX;

@ServerSide
public class IssueChangeEventServiceImpl implements IssueChangeEventService {
  private static final Gson GSON = new GsonBuilder().create();

  private static final String EVENT_NAME = "IssueChanged";
  private static final String FALSE_POSITIVE_KEY = "FALSE-POSITIVE";
  private static final String WONT_FIX_KEY = "WONTFIX";

  private static final String RESOLUTION_KEY = "resolution";
  private static final String SEVERITY_KEY = "severity";
  private static final String IMPACT_SEVERITY_KEY = "impactSeverity";
  private static final String TYPE_KEY = "type";

  private final DbClient dbClient;

  public IssueChangeEventServiceImpl(DbClient dbClient) {
    this.dbClient = dbClient;
  }

  @Override
  public void distributeIssueChangeEvent(DefaultIssue issue, @Nullable String severity, Map<SoftwareQuality, Severity> impacts, @Nullable String type, @Nullable String transition,
    BranchDto branch, String projectKey) {
    Issue changedIssue = new Issue(issue.key(), branch.getKey());

    Boolean resolved = isResolved(transition);

    if (severity == null && type == null && resolved == null && impacts.isEmpty()) {
      return;
    }

    impacts.forEach(changedIssue::addImpact);
    IssueChangedEvent event = new IssueChangedEvent(projectKey, new Issue[] {changedIssue},
      resolved, severity, type);

    persistEvent(event, branch.getProjectUuid());
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

      Map<String, Issue> issueChanges = issuesInProject.stream()
        .filter(i -> branchesByProjectUuid.get(i.projectUuid()).getBranchType().equals(BRANCH))
        .map(i -> new Issue(i.key(), branchesByProjectUuid.get(i.projectUuid()).getKey()))
        .collect(Collectors.toMap(Issue::getIssueKey, i -> i));

      if (issueChanges.isEmpty()) {
        continue;
      }

      IssueChangedEvent event = getIssueChangedEvent(projectKey, issuesInProject, issueChanges);

      if (event != null) {
        BranchDto branchDto = branchesByProjectUuid.get(entry.getKey());
        persistEvent(event, branchDto.getProjectUuid());
      }
    }
  }

  @CheckForNull
  private static IssueChangedEvent getIssueChangedEvent(String projectKey, Set<DefaultIssue> issuesInProject, Map<String, Issue> issueChanges) {
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
    addImpactsToChangeEvent(issuesInProject, issueChanges);

    if (diffs.containsKey(TYPE_KEY)) {
      type = diffs.get(TYPE_KEY).newValue() == null ? null : diffs.get(TYPE_KEY).newValue().toString();
      isRelevantEvent = true;
    }

    if (!isRelevantEvent) {
      return null;
    }

    return new IssueChangedEvent(projectKey, issueChanges.values().toArray(new Issue[0]), resolved, severity, type);
  }

  private static void addImpactsToChangeEvent(Set<DefaultIssue> issuesInProject, Map<String, Issue> issueChanges) {
    for (DefaultIssue defaultIssue : issuesInProject) {
      FieldDiffs currentChanges = defaultIssue.currentChange();
      if (currentChanges == null) {
        continue;
      }

      Map<String, Diff> diffs = currentChanges.diffs();
      if (issueChanges.containsKey(defaultIssue.key())
        && diffs.containsKey(IMPACT_SEVERITY_KEY)) {
        Serializable newValue = diffs.get(IMPACT_SEVERITY_KEY).newValue();
        String impact = newValue != null ? newValue.toString() : null;
        Issue issue = issueChanges.get(defaultIssue.key());
        if (impact != null) {
          issue.addImpact(SoftwareQuality.valueOf(impact.split(":")[0]), Severity.valueOf(impact.split(":")[1]));
        }
      }
    }
  }

  @CheckForNull
  private static Boolean isResolved(@Nullable String transitionOrStatus) {
    if (isNullOrEmpty(transitionOrStatus)) {
      return null;
    }

    if (transitionOrStatus.equals(CONFIRM.getKey()) || transitionOrStatus.equals(UNCONFIRM.getKey())) {
      return null;
    }

    return transitionOrStatus.equals(ACCEPT.getKey()) || transitionOrStatus.equals(WONT_FIX.getKey()) || transitionOrStatus.equals(FALSE_POSITIVE.getKey()) ||
      transitionOrStatus.equals(FALSE_POSITIVE_KEY) || transitionOrStatus.equals(WONT_FIX_KEY);
  }

  private void persistEvent(IssueChangedEvent event, String entry) {
    try (DbSession dbSession = dbClient.openSession(false)) {
      PushEventDto eventDto = new PushEventDto()
        .setName(EVENT_NAME)
        .setProjectUuid(entry)
        .setPayload(serializeIssueToPushEvent(event));
      dbClient.pushEventDao().insert(dbSession, eventDto);
      dbSession.commit();
    }
  }

  private static byte[] serializeIssueToPushEvent(IssueChangedEvent event) {
    return GSON.toJson(event).getBytes(UTF_8);
  }
}
