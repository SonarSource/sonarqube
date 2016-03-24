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
import org.sonar.api.issue.Issue;
import org.sonar.api.server.ServerSide;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.server.user.UserSession;

import static com.google.common.collect.Lists.newArrayList;
import static org.sonar.api.web.UserRole.ISSUE_ADMIN;

/**
 * @since 3.6
 */
@ServerSide
public class ActionService {

  private final DbClient dbClient;
  private final UserSession userSession;
  private final IssueService issueService;

  public ActionService(DbClient dbClient, UserSession userSession, IssueService issueService) {
    this.dbClient = dbClient;
    this.userSession = userSession;
    this.issueService = issueService;
  }

  public List<String> listAvailableActions(String issueKey) {
    DbSession session = dbClient.openSession(false);
    try {
      return listAvailableActions(issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue());
    } finally {
      dbClient.closeSession(session);
    }
  }

  public List<String> listAvailableActions(Issue issue) {
    List<String> availableActions = newArrayList();
    String login = userSession.getLogin();
    if (login != null) {
      availableActions.add("comment");
      if (issue.resolution() == null) {
        availableActions.add("assign");
        availableActions.add("set_tags");
        availableActions.add("set_type");
        if (!login.equals(issue.assignee())) {
          availableActions.add("assign_to_me");
        }
        String projectUuid = issue.projectUuid();
        if (projectUuid != null && userSession.hasComponentUuidPermission(ISSUE_ADMIN, projectUuid)) {
          availableActions.add("set_severity");
        }
      }
    }
    return availableActions;
  }

}
