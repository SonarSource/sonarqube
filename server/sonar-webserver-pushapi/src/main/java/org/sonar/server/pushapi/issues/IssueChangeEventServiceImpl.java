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
package org.sonar.server.pushapi.issues;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.component.BranchDto;
import org.sonar.db.component.ComponentDto;
import org.sonar.db.pushevent.PushEventDto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.elasticsearch.common.Strings.isNullOrEmpty;
import static org.sonar.api.issue.DefaultTransitions.CONFIRM;
import static org.sonar.api.issue.DefaultTransitions.FALSE_POSITIVE;
import static org.sonar.api.issue.DefaultTransitions.UNCONFIRM;
import static org.sonar.api.issue.DefaultTransitions.WONT_FIX;
import static org.sonar.db.component.BranchType.BRANCH;

@ServerSide
public class IssueChangeEventServiceImpl implements IssueChangeEventService {
  private static final Gson GSON = new GsonBuilder().create();

  private static final String EVENT_NAME = "IssueChanged";
  private static final String FALSE_POSITIVE_KEY = "FALSE-POSITIVE";
  private static final String WONT_FIX_KEY = "WONTFIX";

  private static final String RESOLUTION_KEY = "resolution";
  private static final String SEVERITY_KEY = "severity";
  private static final String TYPE_KEY = "type";

  private final DbClient dbClient;

  public IssueChangeEventServiceImpl(DbClient dbClient) {
    this.dbClient = dbClient;
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

    persistEvent(event, issue.projectUuid());
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
        persistEvent(event, entry.getValue().branchUuid());
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
