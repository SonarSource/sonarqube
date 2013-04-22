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
package org.sonar.core.issue;

import org.sonar.api.issue.IssueChange;

import java.util.Map;

public class UpdateIssueFields {

  public static void apply(DefaultIssue issue, IssueChange change) {
    if (change.description() != null) {
      issue.setDescription(change.description());
    }
    if (change.manualSeverity() != null) {
      change.setManualSeverity(change.manualSeverity());
    }
    if (change.severity() != null) {
      issue.setSeverity(change.severity());
    }
    if (change.title() != null) {
      issue.setTitle(change.title());
    }
    if (change.isAssigneeChanged()) {
      issue.setAssignee(change.assignee());
    }
    if (change.isLineChanged()) {
      issue.setLine(change.line());
    }
    if (change.isCostChanged()) {
      issue.setCost(change.cost());
    }
    for (Map.Entry<String, String> entry : change.attributes().entrySet()) {
      issue.setAttribute(entry.getKey(), entry.getValue());
    }
  }

}
