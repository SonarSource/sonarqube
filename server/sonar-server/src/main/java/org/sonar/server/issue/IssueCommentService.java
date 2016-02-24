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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.IssueComment;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.api.utils.System2;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.db.DbSession;
import org.sonar.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public class IssueCommentService {

  private final DbClient dbClient;
  private final IssueService issueService;
  private final IssueUpdater updater;
  private final UserSession userSession;

  public IssueCommentService(DbClient dbClient, IssueService issueService, IssueUpdater updater, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.updater = updater;
    this.userSession = userSession;
  }

  public List<DefaultIssueComment> findComments(String issueKey) {
    return findComments(newArrayList(issueKey));
  }

  public List<DefaultIssueComment> findComments(DbSession dbSession, String issueKey) {
    return findComments(dbSession, newArrayList(issueKey));
  }

  public List<DefaultIssueComment> findComments(Collection<String> issueKeys) {
    DbSession session = dbClient.openSession(false);
    try {
      return findComments(session, issueKeys);
    } finally {
      session.close();
    }
  }

  public List<DefaultIssueComment> findComments(DbSession session, Collection<String> issueKeys) {
    return dbClient.issueChangeDao().selectCommentsByIssues(session, issueKeys);
  }

  public IssueComment findComment(String commentKey) {
    return dbClient.issueChangeDao().selectCommentByKey(commentKey);
  }

  public IssueComment addComment(String issueKey, String text) {
    verifyLoggedIn(userSession);
    if (StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.getLogin());
      updater.addComment(issue, text, context);

      issueService.saveIssue(session, issue, context, text);
      session.commit();

      List<DefaultIssueComment> comments = findComments(issueKey);
      if (comments.isEmpty()) {
        throw new BadRequestException(String.format("Fail to add a comment on issue %s", issueKey));
      }
      return comments.get(comments.size() - 1);
    } finally {
      session.close();
    }
  }

  public IssueComment deleteComment(String commentKey) {
    DefaultIssueComment comment = dbClient.issueChangeDao().selectCommentByKey(commentKey);
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.getLogin())) {
      throw new ForbiddenException("You can only delete your own comments");
    }

    // check authorization
    issueService.getByKey(comment.issueKey());

    dbClient.issueChangeDao().delete(commentKey);
    return comment;
  }

  public IssueComment editComment(String commentKey, String text) {
    DefaultIssueComment comment = dbClient.issueChangeDao().selectCommentByKey(commentKey);
    if (StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.getLogin())) {
      throw new ForbiddenException("You can only edit your own comments");
    }

    // check authorization
    issueService.getByKey(comment.issueKey());

    IssueChangeDto dto = IssueChangeDto.of(comment);
    dto.setUpdatedAt(System2.INSTANCE.now());
    dto.setChangeData(text);
    dbClient.issueChangeDao().update(dto);

    return comment;
  }

  public boolean canEditOrDelete(IssueChangeDto dto) {
    return userSession.isLoggedIn() && userSession.getLogin().equals(dto.getUserLogin());
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      throw new UnauthorizedException("User is not logged in");
    }
  }
}
