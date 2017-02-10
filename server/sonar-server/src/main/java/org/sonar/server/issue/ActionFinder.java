/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonar.server.issue;

import java.util.Collections;
import java.util.List;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Objects.requireNonNull;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;
import static org.sonar.server.issue.AssignAction.ASSIGN_KEY;
import static org.sonar.server.issue.CommentAction.COMMENT_KEY;
import static org.sonar.server.issue.SetSeverityAction.SET_SEVERITY_KEY;
import static org.sonar.server.issue.SetTypeAction.SET_TYPE_KEY;

public class ActionFinder {

  private final UserSession userSession;

  public ActionFinder(UserSession userSession) {
    this.userSession = userSession;
  }

  public List<String> listAvailableActions(IssueDto issue) {
    List<String> availableActions = newArrayList();
    String login = userSession.getLogin();
    if (login == null) {
      return Collections.emptyList();
    }
    availableActions.add(COMMENT_KEY);
    if (issue.getResolution() != null) {
      return availableActions;
    }
    availableActions.add(ASSIGN_KEY);
    availableActions.add("set_tags");
    if (!login.equals(issue.getAssignee())) {
      // This action will be removed by
      availableActions.add("assign_to_me");
    }
    if (userSession.hasComponentUuidPermission(ISSUE_ADMIN, requireNonNull(issue.getProjectUuid()))) {
      availableActions.add(SET_TYPE_KEY);
      availableActions.add(SET_SEVERITY_KEY);
    }
    return availableActions;
  }

}
