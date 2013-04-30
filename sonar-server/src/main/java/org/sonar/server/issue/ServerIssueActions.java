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
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.resource.ResourceDao;
import org.sonar.core.resource.ResourceDto;
import org.sonar.core.resource.ResourceQuery;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.platform.UserSession;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @since 3.6
 */
public class ServerIssueActions implements ServerComponent {

  private final IssueWorkflow workflow;
  private final IssueDao issueDao;
  private final AuthorizationDao authorizationDao;
  private final ResourceDao resourceDao;
  private final RuleFinder ruleFinder;
  private final IssueUpdater issueUpdater;

  public ServerIssueActions(IssueWorkflow workflow, IssueDao issueDao,
                            AuthorizationDao authorizationDao, ResourceDao resourceDao, RuleFinder ruleFinder, IssueUpdater issueUpdater) {
    this.workflow = workflow;
    this.issueDao = issueDao;
    this.authorizationDao = authorizationDao;
    this.resourceDao = resourceDao;
    this.ruleFinder = ruleFinder;
    this.issueUpdater = issueUpdater;
  }

  public List<Transition> listTransitions(String issueKey, UserSession userSession) {
    IssueDto dto = loadDto(issueKey, userSession);
    DefaultIssue issue = dto.toDefaultIssue();
    return workflow.outTransitions(issue);
  }

  public Issue doTransition(String issueKey, String transition, UserSession userSession) {
    IssueDto dto = loadDto(issueKey, userSession);
    DefaultIssue issue = dto.toDefaultIssue();
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (workflow.doTransition(issue, transition, context)) {
      issueDao.update(Arrays.asList(IssueDto.toDto(issue, dto.getResourceId(), dto.getRuleId())));
    }
    return issue;
  }

  public Issue assign(String issueKey, @Nullable String assigneeLogin, UserSession userSession) {
    IssueDto dto = loadDto(issueKey, userSession);
    DefaultIssue issue = dto.toDefaultIssue();

    // TODO check that assignee exists
    if (issueUpdater.assign(issue, assigneeLogin)) {
      issueDao.update(Arrays.asList(IssueDto.toDto(issue, dto.getResourceId(), dto.getRuleId())));
    }
    return issue;
  }

  public Issue setSeverity(String issueKey, String severity, UserSession userSession) {
    IssueDto dto = loadDto(issueKey, userSession);
    DefaultIssue issue = dto.toDefaultIssue();

    if (issueUpdater.setSeverity(issue, severity)) {
      issueDao.update(Arrays.asList(IssueDto.toDto(issue, dto.getResourceId(), dto.getRuleId())));
    }
    return issue;
  }

  public Issue create(DefaultIssue issue, UserSession userSession) {
    issue.setManual(true);
    issue.setUserLogin(userSession.login());

    // TODO check that rule and component exist
    Rule rule = ruleFinder.findByKey(issue.ruleKey());
    ResourceDto resourceDto = resourceDao.getResource(ResourceQuery.create().setKey(issue.componentKey()));
    IssueDto dto = IssueDto.toDto(issue, resourceDto.getId().intValue(), rule.getId());
    issueDao.insert(dto);

    return issue;
  }

  private IssueDto loadDto(String issueKey, UserSession userSession) {
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
    return dto;
  }
}
