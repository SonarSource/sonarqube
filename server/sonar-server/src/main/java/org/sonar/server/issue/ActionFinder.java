/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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

import java.util.List;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;

/**
 * @since 3.6
 */
public class ActionFinder {

  private final UserSession userSession;

  public ActionFinder(UserSession userSession) {
    this.userSession = userSession;
  }

  public List<String> listAvailableActions(IssueDto issue) {
    List<String> availableActions = newArrayList();
    String login = userSession.getLogin();
    if (login != null) {
      availableActions.add("comment");
      if (issue.getResolution() == null) {
        availableActions.add("assign");
        availableActions.add("set_tags");
        availableActions.add("set_type");
        if (!login.equals(issue.getAssignee())) {
          availableActions.add("assign_to_me");
        }
        String projectUuid = issue.getProjectUuid();
        if (projectUuid != null && userSession.hasComponentUuidPermission(ISSUE_ADMIN, projectUuid)) {
          availableActions.add("set_severity");
        }
      }
    }
    return availableActions;
  }

}
