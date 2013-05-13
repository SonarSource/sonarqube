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

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import org.sonar.api.ServerComponent;
import org.sonar.api.issue.IssueComment;
import org.sonar.api.web.UserRole;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.DefaultIssueComment;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.issue.db.IssueChangeDao;
import org.sonar.core.issue.db.IssueStorage;
import org.sonar.server.platform.UserSession;

import java.util.Date;

public class IssueCommentService implements ServerComponent {

  private final IssueUpdater updater;
  private final IssueChangeDao changeDao;
  private final IssueStorage storage;
  private final DefaultIssueFinder finder;

  public IssueCommentService(IssueUpdater updater, IssueChangeDao changeDao, IssueStorage storage, DefaultIssueFinder finder) {
    this.updater = updater;
    this.changeDao = changeDao;
    this.storage = storage;
    this.finder = finder;
  }

  public IssueComment addComment(String issueKey, String text, UserSession userSession) {
    verifyLoggedIn(userSession);
    DefaultIssue issue = finder.findByKey(issueKey, UserRole.USER);

    IssueChangeContext context = IssueChangeContext.createUser(new Date(), userSession.login());
    updater.addComment(issue, text, context);
    storage.save(issue);
    return issue.comments().get(issue.comments().size() - 1);
  }

  public IssueComment deleteComment(String commentKey, UserSession userSession) {
    DefaultIssueComment comment = changeDao.selectCommentByKey(commentKey);
    if (comment == null) {
      // TODO throw 404
      throw new IllegalStateException();
    }
    if (Strings.isNullOrEmpty(comment.userLogin()) || !Objects.equal(comment.userLogin(), userSession.login())) {
      // TODO throw unauthorized
      throw new IllegalStateException();
    }

    // check authorization
    finder.findByKey(comment.issueKey(), UserRole.USER);

    changeDao.delete(commentKey);
    return comment;
  }

  public IssueComment editComment(String key, String text, UserSession userSession) {
    //TODO
    return null;
  }

  private void verifyLoggedIn(UserSession userSession) {
    if (!userSession.isLoggedIn()) {
      // must be logged
      throw new IllegalStateException("User is not logged in");
    }
  }
}
