/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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

import org.sonar.api.ServerComponent;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.platform.UserSession;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @since 3.6
 */
public class ServerIssueActions implements ServerComponent {

  private final IssueWorkflow workflow;
  private final IssueFinder finder;
  private final IssueDao issueDao;
  private final AuthorizationDao authorizationDao;

  public ServerIssueActions(IssueWorkflow workflow, IssueFinder finder, IssueDao issueDao, AuthorizationDao authorizationDao) {
    this.workflow = workflow;
    this.finder = finder;
    this.issueDao = issueDao;
    this.authorizationDao = authorizationDao;
  }

  public List<Transition> listTransitions(String issueKey, @Nullable Integer userId) {
    Issue issue = finder.findByKey(issueKey /*, userId */);
    // TODO check authorization
    if (issue == null) {
      return Collections.emptyList();
    }
    return workflow.outManualTransitions(issue);
  }

  public Issue doTransition(String issueKey, String transition, UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
    IssueDto dto = issueDao.selectByKey(issueKey);
    if (dto == null) {
      throw new IllegalStateException("Unknown issue: " + issueKey);
    }
    String requiredRole = UserRole.USER;
    if (!authorizationDao.isAuthorizedComponentId(dto.getResourceId(), userSession.userId(), requiredRole)) {
      throw new IllegalStateException("User does not have the role " + requiredRole + " required to change the issue: " + issueKey);
    }
    DefaultIssue issue = dto.toDefaultIssue();
    if (workflow.doManualTransition(issue, transition)) {
      issueDao.update(Arrays.asList(IssueDto.toDto(issue, dto.getResourceId(), dto.getRuleId())));
    }
    return issue;
  }
}
