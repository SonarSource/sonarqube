/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.batch.issue;

import org.sonar.api.issue.Issue;
import org.sonar.api.issue.IssueChange;
import org.sonar.api.issue.IssueChanges;
import org.sonar.core.issue.DefaultIssue;

import java.util.Date;
import java.util.Map;

public class ScanIssueChanges implements IssueChanges {

  private final IssueCache cache;

  public ScanIssueChanges(IssueCache cache) {
    this.cache = cache;
  }

  @Override
  public Issue apply(Issue issue, IssueChange change) {
    if (!change.hasChanges()) {
      return issue;
    }
    DefaultIssue reloaded = reload(issue);
    doChange(reloaded, change);
    // TODO set the date of loading of issues
    reloaded.setUpdatedAt(new Date());
    cache.addOrUpdate(reloaded);
    // TODO keep history of changes
    return reloaded;
  }

  private void doChange(DefaultIssue issue, IssueChange change) {
    if (change.isCostChanged()) {
      issue.setCost(change.cost());
    }
    if (change.manualSeverity() != null) {
      change.setManualSeverity(change.manualSeverity());
    }
    if (change.severity() != null) {
      issue.setSeverity(change.severity());
    }
    if (change.isAssigneeChanged()) {
      issue.setAssignee(change.assignee());
    }
    if (change.resolution() != null) {
      issue.setResolution(change.resolution());
    }
    if (change.isLineChanged()) {
      issue.setLine(change.line());
    }
    for (Map.Entry<String, String> entry : change.attributes().entrySet()) {
      issue.setAttribute(entry.getKey(), entry.getValue());
    }
  }

  private DefaultIssue reload(Issue issue) {
    DefaultIssue reloaded = (DefaultIssue) cache.componentIssue(issue.componentKey(), issue.key());
    if (reloaded == null) {
      throw new IllegalStateException("Bad API usage. Unregistered issues can't be changed.");
    }
    return reloaded;
  }
}
