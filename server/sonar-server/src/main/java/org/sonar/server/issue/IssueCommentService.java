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

import com.google.common.base.Strings;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.utils.System2;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.db.DbClient;
import org.sonar.db.issue.IssueChangeDto;
import org.sonar.server.exceptions.BadRequestException;
import org.sonar.server.exceptions.ForbiddenException;
import org.sonar.server.exceptions.NotFoundException;
import org.sonar.server.user.UserSession;

public class IssueCommentService {

  private final DbClient dbClient;
  private final IssueService issueService;
  private final UserSession userSession;

  public IssueCommentService(DbClient dbClient, IssueService issueService, UserSession userSession) {
    this.dbClient = dbClient;
    this.issueService = issueService;
    this.userSession = userSession;
  }

  public IssueComment deleteComment(String commentKey) {
    DefaultIssueComment comment = dbClient.issueChangeDao().selectDefaultCommentByKey(commentKey);
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equals(comment.userLogin(), userSession.getLogin())) {
      throw new ForbiddenException("You can only delete your own comments");
    }

    // check authorization
    issueService.getByKey(comment.issueKey());

    dbClient.issueChangeDao().delete(commentKey);
    return comment;
  }

  public IssueComment editComment(String commentKey, String text) {
    DefaultIssueComment comment = dbClient.issueChangeDao().selectDefaultCommentByKey(commentKey);
    if (StringUtils.isBlank(text)) {
      throw new BadRequestException("Cannot add empty comments to an issue");
    }
    if (comment == null) {
      throw new NotFoundException("Comment not found: " + commentKey);
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equals(comment.userLogin(), userSession.getLogin())) {
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
}
