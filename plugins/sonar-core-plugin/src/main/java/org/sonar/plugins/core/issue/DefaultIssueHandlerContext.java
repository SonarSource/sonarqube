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
package org.sonar.plugins.core.issue;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueHandler;
import org.sonar.core.issue.DefaultIssue;

import javax.annotation.Nullable;

class DefaultIssueHandlerContext implements IssueHandler.IssueContext {

  private final DefaultIssue issue;

  DefaultIssueHandlerContext(DefaultIssue issue) {
    this.issue = issue;
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
  public boolean isAlive() {
    return issue.isAlive();
  }

  @Override
  public IssueHandler.IssueContext setLine(@Nullable Integer line) {
    issue.setLine(line);
    return this;
  }

  @Override
  public IssueHandler.IssueContext setDescription(String description) {
    issue.setDescription(description);
    return this;
  }

  @Override
  public IssueHandler.IssueContext setSeverity(String severity) {
    issue.setSeverity(severity);
    return this;
  }

  @Override
  public IssueHandler.IssueContext setAuthorLogin(@Nullable String login) {
    issue.setAuthorLogin(login);
    return this;
  }

  @Override
  public IssueHandler.IssueContext setAttribute(String key, @Nullable String value) {
    issue.setAttribute(key, value);
    return this;
  }

  @Override
  public IssueHandler.IssueContext assignTo(@Nullable String login) {
    issue.setAssignee(login);
    return this;
  }
}
