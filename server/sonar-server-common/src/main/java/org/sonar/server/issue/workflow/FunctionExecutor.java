/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.server.issue.workflow;

import javax.annotation.Nullable;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.issue.Issue;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.ServerSide;
import org.sonar.core.issue.DefaultIssue;
import org.sonar.core.issue.IssueChangeContext;
import org.sonar.db.user.UserDto;
import org.sonar.server.issue.IssueFieldsSetter;

@ServerSide
@ComputeEngineSide
public class FunctionExecutor {

  private final IssueFieldsSetter updater;

  public FunctionExecutor(IssueFieldsSetter updater) {
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
    private final IssueFieldsSetter updater;
    private final DefaultIssue issue;
    private final IssueChangeContext changeContext;

    FunctionContext(IssueFieldsSetter updater, DefaultIssue issue, IssueChangeContext changeContext) {
      this.updater = updater;
      this.issue = issue;
      this.changeContext = changeContext;
    }

    @Override
    public Issue issue() {
      return issue;
    }

    @Override
    public Function.Context setAssignee(@Nullable UserDto user) {
      updater.assign(issue, user, changeContext);
      return this;
    }

    @Override
    public Function.Context setResolution(@Nullable String s) {
      updater.setResolution(issue, s, changeContext);
      return this;
    }

    @Override
    public Function.Context setCloseDate() {
      updater.setCloseDate(issue, changeContext.date(), changeContext);
      return this;
    }

    @Override
    public Function.Context unsetCloseDate() {
      updater.setCloseDate(issue, null, changeContext);
      return this;
    }

    @Override
    public Function.Context unsetLine() {
      updater.unsetLine(issue, changeContext);
      return this;
    }

    @Override
    public Function.Context setType(RuleType type) {
      updater.setType(issue, type, changeContext);
      return this;
    }
  }
}
