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
package org.sonar.batch.issue.tracking;

import org.sonar.api.BatchSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueHandler;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.user.User;
import org.sonar.core.issue.IssueUpdater;
import org.sonar.core.user.DefaultUser;

import javax.annotation.Nullable;

@BatchSide
public class IssueHandlers {
  private final IssueHandler[] handlers;
  private final DefaultContext context;

  public IssueHandlers(IssueUpdater updater, IssueHandler[] handlers) {
    this.handlers = handlers;
    this.context = new DefaultContext(updater);
  }

  public IssueHandlers(IssueUpdater updater) {
    this(updater, new IssueHandler[0]);
  }

  public void execute(DefaultIssue issue, IssueChangeContext changeContext) {
    context.reset(issue, changeContext);
    for (IssueHandler handler : handlers) {
      handler.onIssue(context);
    }
  }

  static class DefaultContext implements IssueHandler.Context {
    private final IssueUpdater updater;
    private DefaultIssue issue;
    private IssueChangeContext changeContext;

    private DefaultContext(IssueUpdater updater) {
      this.updater = updater;
    }

    private void reset(DefaultIssue i, IssueChangeContext changeContext) {
      this.issue = i;
      this.changeContext = changeContext;
    }

    @Override
    public Issue issue() {
      return issue;
    }

    @Override
    public boolean isNew() {
      return issue.isNew();
    }

    @Override
    public boolean isEndOfLife() {
      return issue.isEndOfLife();
    }

    @Override
    public IssueHandler.Context setLine(@Nullable Integer line) {
      updater.setLine(issue, line);
      return this;
    }

    @Override
    public IssueHandler.Context setMessage(@Nullable String s) {
      updater.setMessage(issue, s, changeContext);
      return this;
    }

    @Override
    public IssueHandler.Context setSeverity(String severity) {
      updater.setSeverity(issue, severity, changeContext);
      return this;
    }

    @Override
    public IssueHandler.Context setAuthorLogin(@Nullable String login) {
      updater.setAuthorLogin(issue, login, changeContext);
      return this;
    }

    @Override
    public IssueHandler.Context setEffortToFix(@Nullable Double d) {
      updater.setEffortToFix(issue, d, changeContext);
      return this;
    }

    @Override
    public IssueHandler.Context setAttribute(String key, @Nullable String value) {
      throw new UnsupportedOperationException("TODO");
    }

    @Override
    public IssueHandler.Context assign(@Nullable String assignee) {
      User user = null;
      if (assignee != null) {
        user = new DefaultUser().setLogin(assignee).setName(assignee);
      }
      updater.assign(issue, user, changeContext);
      return this;
    }

    @Override
    public IssueHandler.Context assign(@Nullable User user) {
      updater.assign(issue, user, changeContext);
      return this;
    }

    @Override
    public IssueHandler.Context addComment(String text) {
      updater.addComment(issue, text, changeContext);
      return this;
    }
  }

}
