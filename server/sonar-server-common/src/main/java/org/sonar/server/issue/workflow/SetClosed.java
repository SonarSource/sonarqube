/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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

import org.sonar.api.issue.Issue;
import org.sonar.core.issue.DefaultIssue;

public enum SetClosed implements Function {
  INSTANCE;

  @Override
  public void execute(Context context) {
    DefaultIssue issue = (DefaultIssue) context.issue();
    if (issue.isOnDisabledRule()) {
      context.setResolution(Issue.RESOLUTION_REMOVED);
    } else {
      context.setResolution(Issue.RESOLUTION_FIXED);
    }

    // closed issues are not "tracked" -> the line number does not evolve anymore
    // when code changes. That's misleading for end-users, so line number
    // is unset.
    context.setLine(null);
  }
}
