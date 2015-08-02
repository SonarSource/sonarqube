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

package org.sonar.server.issue;

import java.util.List;
import org.sonar.api.server.ServerSide;
import org.sonar.api.web.UserRole;
import org.sonar.db.issue.IssueDto;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
@ServerSide
public class ActionService {

  private final UserSession userSession;

  public ActionService(UserSession userSession) {
    this.userSession = userSession;
  }

  public List<String> listAvailableActions(IssueDto issue) {
    List<String> actions = newArrayList();
    String login = userSession.getLogin();
    if (login != null) {
      actions.add("comment");
      if (issue.getResolution() == null) {
        actions.add("assign");
        actions.add("set_tags");
        if (!login.equals(issue.getAssignee())) {
          actions.add("assign_to_me");
        }
        actions.add("plan");
        String projectUuid = issue.getProjectUuid();
        if (projectUuid != null && userSession.hasProjectPermissionByUuid(UserRole.ISSUE_ADMIN, projectUuid)) {
          actions.add("set_severity");
        }
      }
    }
    return actions;
  }
}
