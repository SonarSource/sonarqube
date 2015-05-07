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
package org.sonar.core.issue.workflow;

import org.sonar.api.BatchSide;
import org.sonar.api.ServerSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.issue.internal.DefaultIssue;
import org.sonar.api.issue.internal.IssueChangeContext;
import org.sonar.api.user.User;
import org.sonar.core.issue.IssueUpdater;

import javax.annotation.Nullable;

@BatchSide
@ServerSide
public class FunctionExecutor {

  private final IssueUpdater updater;

  public FunctionExecutor(IssueUpdater updater) {
    this.updater = updater;
  }

  public void execute(Function[] functions, DefaultIssue issue, IssueChangeContext changeContext) {
    if (functions.length > 0) {
      FunctionContext functionContext = new FunctionContext(updater, issue, changeContext);
      for (Function function : functions) {
        function.execute(functionContext);
      }
    }
  }

  static class FunctionContext implements Function.Context {
    private final IssueUpdater updater;
    private final DefaultIssue issue;
    private final IssueChangeContext changeContext;

    FunctionContext(IssueUpdater updater, DefaultIssue issue, IssueChangeContext changeContext) {
      this.updater = updater;
      this.issue = issue;
      this.changeContext = changeContext;
    }

    @Override
    public Issue issue() {
      return issue;
    }

    @Override
    public Function.Context setAssignee(@Nullable User user) {
      updater.assign(issue, user, changeContext);
      return this;
    }

    @Override
    public Function.Context setResolution(@Nullable String s) {
      updater.setResolution(issue, s, changeContext);
      return this;
    }

    @Override
    public Function.Context setCloseDate(boolean b) {
      updater.setCloseDate(issue, b ? changeContext.date() : null, changeContext);
      return this;
    }

    @Override
    public Function.Context setLine(@Nullable Integer line) {
      updater.setLine(issue, line);
      return this;
    }
  }
}
