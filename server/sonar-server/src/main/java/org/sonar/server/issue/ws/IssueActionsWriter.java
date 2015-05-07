/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.issue.ws;

import org.sonar.api.ServerSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.action.Action;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.user.UserSession;

import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

@ServerSide
public class IssueActionsWriter {

  private final IssueService issueService;
  private final ActionService actionService;

  public IssueActionsWriter(IssueService issueService, ActionService actionService) {
    this.issueService = issueService;
    this.actionService = actionService;
  }

  public void writeTransitions(Issue issue, JsonWriter json) {
    json.name("transitions").beginArray();
    if (UserSession.get().isLoggedIn()) {
      for (Transition transition : issueService.listTransitions(issue)) {
        json.value(transition.key());
      }
    }
    json.endArray();
  }

  public void writeActions(Issue issue, JsonWriter json) {
    json.name("actions").beginArray();
    for (String action : actions(issue)) {
      json.value(action);
    }
    json.endArray();
  }

  private List<String> actions(Issue issue) {
    List<String> actions = newArrayList();
    String login = UserSession.get().login();
    if (login != null) {
      actions.add("comment");
      if (issue.resolution() == null) {
        actions.add("assign");
        actions.add("set_tags");
        if (!login.equals(issue.assignee())) {
          actions.add("assign_to_me");
        }
        actions.add("plan");
        String projectUuid = issue.projectUuid();
        if (projectUuid != null && UserSession.get().hasProjectPermissionByUuid(UserRole.ISSUE_ADMIN, projectUuid)) {
          actions.add("set_severity");
        }
        for (Action action : actionService.listAvailableActions(issue)) {
          actions.add(action.key());
        }
      }
    }
    return actions;
  }

}
