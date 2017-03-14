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

import com.google.common.base.Strings;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.index.IssueIndex;
import org.sonar.server.user.UserSession;

import static org.sonar.server.ws.WsUtils.checkRequest;

@ServerSide
@ComputeEngineSide
public class IssueService {

  private final DbClient dbClient;
  private final IssueIndex issueIndex;

  private final IssueFinder issueFinder;
  private final IssueFieldsSetter issueFieldsSetter;
  private final IssueUpdater issueUpdater;
  private final UserSession userSession;

  public IssueService(DbClient dbClient, IssueIndex issueIndex, IssueFinder issueFinder, IssueFieldsSetter issueFieldsSetter, IssueUpdater issueUpdater, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueIndex = issueIndex;
    this.issueFinder = issueFinder;
    this.issueFieldsSetter = issueFieldsSetter;
    this.issueUpdater = issueUpdater;
    this.userSession = userSession;
  }

  public void assign(String issueKey, @Nullable String assignee) {
    userSession.checkLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = issueFinder.getByKey(session, issueKey).toDefaultIssue();
      UserDto user = null;
      if (!Strings.isNullOrEmpty(assignee)) {
        user = dbClient.userDao().selectByLogin(session, assignee);
        checkRequest(user != null && user.isActive(), "Unknown user: %s", assignee);
      }
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      if (issueFieldsSetter.assign(issue, user, context)) {
        issueUpdater.saveIssue(session, issue, context, null);
      }

    } finally {
      session.close();
    }
  }

  /**
   * Search for all tags, whatever issue resolution or user access rights
   */
  public List<String> listTags(@Nullable String textQuery, int pageSize) {
    IssueQuery query = IssueQuery.builder()
      .checkAuthorization(false)
      .build();
    return issueIndex.listTags(query, textQuery, pageSize);
  }

  public List<String> listAuthors(@Nullable String textQuery, int pageSize) {
    IssueQuery query = IssueQuery.builder()
      .checkAuthorization(false)
      .build();
    return issueIndex.listAuthors(query, textQuery, pageSize);
  }

  public Collection<String> setTags(String issueKey, Collection<String> tags) {
    userSession.checkLoggedIn();

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = issueFinder.getByKey(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      if (issueFieldsSetter.setTags(issue, tags, context)) {
        issueUpdater.saveIssue(session, issue, context, null);
      }
      return issue.tags();

    } finally {
      session.close();
    }
  }

  public Map<String, Long> listTagsForComponent(IssueQuery query, int pageSize) {
    return issueIndex.countTags(query, pageSize);
  }

}
