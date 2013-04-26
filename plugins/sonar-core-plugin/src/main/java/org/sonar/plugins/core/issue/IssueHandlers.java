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

import org.sonar.api.BatchExtension;
import org.sonar.api.issue.IssueHandler;
import org.sonar.core.issue.DefaultIssue;

public class IssueHandlers implements BatchExtension {
  private final IssueHandler[] handlers;

  public IssueHandlers(IssueHandler[] handlers) {
    this.handlers = handlers;
  }

  public IssueHandlers() {
    this(new IssueHandler[0]);
  }

  public void execute(DefaultIssue issue) {
    DefaultIssueHandlerContext context = new DefaultIssueHandlerContext(issue);
    for (IssueHandler handler : handlers) {
      handler.onIssue(context);
    }
  }

}
