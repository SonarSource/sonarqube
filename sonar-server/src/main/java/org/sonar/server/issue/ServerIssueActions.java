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
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.*;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueDao;
import org.sonar.core.issue.db.IssueDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.core.issue.workflow.IssueWorkflow;
import org.sonar.core.issue.workflow.Transition;
import org.sonar.core.user.AuthorizationDao;
import org.sonar.server.platform.UserSession;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * @since 3.6
 */
public class ServerIssueActions implements ServerComponent {

  private final IssueWorkflow workflow;
  private final IssueUpdater issueUpdater;
  private final IssueDao issueDao;
  private final IssueChangeDao issueChangeDao;
  private final IssueStorage issueStorage;
  private final AuthorizationDao authorizationDao;

  public ServerIssueActions(IssueWorkflow workflow,
                            IssueDao issueDao,
                            IssueStorage issueStorage,
                            AuthorizationDao authorizationDao,
                            IssueUpdater issueUpdater, IssueChangeDao issueChangeDao) {
    this.workflow = workflow;
    this.issueDao = issueDao;
    this.issueStorage = issueStorage;
    this.issueUpdater = issueUpdater;
    this.authorizationDao = authorizationDao;
    this.issueChangeDao = issueChangeDao;
  }

  public List<Transition> listTransitions(String issueKey, UserSession userSession) {
    DefaultIssue issue = loadIssue(issueKey, userSession);
    List<Transition> transitions = workflow.outTransitions(issue);
    Collections.sort(transitions, new Comparator<Transition>() {
      @Override
      public int compare(Transition transition, Transition transition2) {
        return transition.key().compareTo(transition2.key());
      }
    });
    return transitions;
  }

  public Issue doTransition(String issueKey, String transition, UserSession userSession) {
    DefaultIssue issue = loadIssue(issueKey, userSession);
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (workflow.doTransition(issue, transition, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public Issue assign(String issueKey, @Nullable String assigneeLogin, UserSession userSession) {
    DefaultIssue issue = loadIssue(issueKey, userSession);

    // TODO check that assignee exists
    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.assign(issue, assigneeLogin, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public Issue setSeverity(String issueKey, String severity, UserSession userSession) {
    DefaultIssue issue = loadIssue(issueKey, userSession);

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    if (issueUpdater.setManualSeverity(issue, severity, context)) {
      issueStorage.save(issue);
    }
    return issue;
  }

  public IssueComment addComment(String issueKey, String comment, UserSession userSession) {
    DefaultIssue issue = loadIssue(issueKey, userSession);

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    issueUpdater.addComment(issue, comment, context);
    issueStorage.save(issue);
    return issue.newComments().get(0);
  }

  public IssueComment[] comments(String issueKey, UserSession userSession) {
    // TODO verify authorization
    return issueChangeDao.selectIssueComments(issueKey);
  }

  public FieldDiffs[] changes(String issueKey, UserSession userSession) {
    // TODO verify authorization
    return issueChangeDao.selectIssueChanges(issueKey);
  }

  public Issue create(DefaultIssue issue, UserSession userSession) {
    // TODO merge with JRubyInternalIssues
    issue.setManual(true);
    issue.setUserLogin(userSession.login());

    issueStorage.save(issue);

    return issue;
  }

  private DefaultIssue loadIssue(String issueKey, UserSession userSession) {
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
    return dto.toDefaultIssue();
  }
}
