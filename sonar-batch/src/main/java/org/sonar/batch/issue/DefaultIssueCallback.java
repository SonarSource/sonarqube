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
package org.sonar.batch.issue;

import org.sonar.batch.bootstrapper.IssueListener;

import org.sonar.core.issue.DefaultIssue;

public class DefaultIssueCallback implements IssueCallback {
  private final IssueCache issues;
  private final IssueListener listener;

  public DefaultIssueCallback(IssueCache issues, IssueListener listener) {
    this.issues = issues;
    this.listener = listener;
  }

  /**
   * If no listener exists, this constructor will be used by pico.
   */
  public DefaultIssueCallback(IssueCache issues) {
    this(issues, null);
  }

  @Override
  public void execute() {
    if (listener == null) {
      return;
    }

    for (DefaultIssue issue : issues.all()) {
      listener.handle(issue);
    }
  }
}
