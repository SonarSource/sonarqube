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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.IssueQuery;
import org.sonar.api.issue.IssueQueryResult;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.IssueNotifications;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.UserSession;

import java.util.Arrays;
import java.util.Date;

/**
 * @since 3.6
 */
public class IssueCommentService implements ServerComponent {

  private final IssueUpdater updater;
  private final IssueChangeDao changeDao;
  private final IssueStorage storage;
  private final DefaultIssueFinder finder;
  private final IssueNotifications issueNotifications;

  public IssueCommentService(IssueUpdater updater, IssueChangeDao changeDao, IssueStorage storage, DefaultIssueFinder finder, IssueNotifications issueNotifications) {
    this.updater = updater;
    this.changeDao = changeDao;
    this.storage = storage;
    this.finder = finder;
    this.issueNotifications = issueNotifications;
  }

  public IssueComment findComment(String commentKey) {
    return changeDao.selectCommentByKey(commentKey);
  }

  public IssueComment addComment(String issueKey, String text, UserSession userSession) {
    verifyLoggedIn(userSession);
    IssueQueryResult queryResult = loadIssue(issueKey);

    if(StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }

    DefaultIssue issue = (DefaultIssue) queryResult.first();

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    updater.addComment(issue, text, context);
    storage.save(issue);
    issueNotifications.sendChanges(issue, context, queryResult, text);
    return issue.comments().get(issue.comments().size() - 1);
  }

  public IssueComment deleteComment(String commentKey, UserSession userSession) {
    DefaultIssueComment comment = changeDao.selectCommentByKey(commentKey);
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.login())) {
      throw new ForbiddenException("You can only delete your own comments");
    }

    // check authorization
    finder.findByKey(comment.issueKey(), UserRole.USER);

    changeDao.delete(commentKey);
    return comment;
  }

  public IssueComment editComment(String commentKey, String text, UserSession userSession) {
    DefaultIssueComment comment = changeDao.selectCommentByKey(commentKey);
    if (StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }
    if(comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.login())) {
      throw new ForbiddenException("You can only edit your own comments");
    }

    // check authorization
    finder.findByKey(comment.issueKey(), UserRole.USER);

    IssueChangeDto dto = IssueChangeDto.of(comment);
    dto.setUpdatedAt(new Date());
    dto.setChangeData(text);
    changeDao.update(dto);

    return comment;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      throw new UnauthorizedException("User is not logged in");
    }
  }

  // TODO remove this duplication from IssueService
  public IssueQueryResult loadIssue(String issueKey) {
    IssueQuery query = IssueQuery.builder().issueKeys(Arrays.asList(issueKey)).requiredRole(UserRole.USER).build();
    IssueQueryResult result = finder.find(query);
    if (result.issues().size() != 1) {
      throw new IllegalStateException("Issue not found: " + issueKey);
    }
    return result;
  }
}
