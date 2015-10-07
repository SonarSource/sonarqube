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

import org.sonar.api.issue.Issue;
import org.sonar.api.server.ServerSide;
import org.sonar.api.utils.text.JsonWriter;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.server.issue.ActionService;
import org.sonar.server.issue.IssueService;
import org.sonar.server.user.UserSession;

@ServerSide
public class IssueActionsWriter {

  private final IssueService issueService;
  private final ActionService actionService;
  private final UserSession userSession;

  public IssueActionsWriter(IssueService issueService, ActionService actionService, UserSession userSession) {
    this.issueService = issueService;
    this.actionService = actionService;
    this.userSession = userSession;
  }

  public void writeTransitions(Issue issue, JsonWriter json) {
    json.name("transitions").beginArray();
    if (userSession.isLoggedIn()) {
      for (Transition transition : issueService.listTransitions(issue)) {
        json.value(transition.key());
      }
    }
    json.endArray();
  }

  public void writeActions(Issue issue, JsonWriter json) {
    json.name("actions").beginArray();
    for (String action : actionService.listAvailableActions(issue)) {
      json.value(action);
    }
    json.endArray();
  }

}
