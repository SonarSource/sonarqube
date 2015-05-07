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
import org.sonar.api.ServerSide;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.DefaultIssueComment;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueChangeDto;
import org.sonar.core.persistence.DbSession;
import org.sonar.server.db.DbClient;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.exceptions.UnauthorizedException;
import org.sonar.server.user.UserSession;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * @since 3.6
 */
@ServerSide
public class IssueCommentService {

  private final DbClient dbClient;
  private final IssueService issueService;
  private final IssueUpdater updater;
  private final IssueChangeDao changeDao;

  public IssueCommentService(DbClient dbClient, IssueService issueService, IssueUpdater updater, IssueChangeDao changeDao) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.updater = updater;
    this.changeDao = changeDao;
  }

  public List<DefaultIssueComment> findComments(String issueKey) {
    return findComments(newArrayList(issueKey));
  }

  public List<DefaultIssueComment> findComments(DbSession session, String issueKey) {
    return findComments(session, newArrayList(issueKey));
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
    return changeDao.selectCommentsByIssues(session, issueKeys);
  }

  public IssueComment findComment(String commentKey) {
    return changeDao.selectCommentByKey(commentKey);
  }

  public IssueComment addComment(String issueKey, String text, UserSession userSession) {
    verifyLoggedIn(userSession);
    if (StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }

    DbSession session = dbClient.openSession(false);
    try {
      DefaultIssue issue = issueService.getByKeyForUpdate(session, issueKey).toDefaultIssue();
      IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
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

  public IssueComment deleteComment(String commentKey, UserSession userSession) {
    DefaultIssueComment comment = changeDao.selectCommentByKey(commentKey);
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.login())) {
      throw new ForbiddenException("You can only delete your own comments");
    }

    // check authorization
    issueService.getByKey(comment.issueKey());

    changeDao.delete(commentKey);
    return comment;
  }

  public IssueComment editComment(String commentKey, String text, UserSession userSession) {
    DefaultIssueComment comment = changeDao.selectCommentByKey(commentKey);
    if (StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.login())) {
      throw new ForbiddenException("You can only edit your own comments");
    }

    // check authorization
    issueService.getByKey(comment.issueKey());

    IssueChangeDto dto = IssueChangeDto.of(comment);
    dto.setUpdatedAt(System2.INSTANCE.now());
    dto.setChangeData(text);
    changeDao.update(dto);

    return comment;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      throw new UnauthorizedException("User is not logged in");
    }
  }
}
