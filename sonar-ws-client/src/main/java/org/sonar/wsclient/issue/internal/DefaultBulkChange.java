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
package org.sonar.wsclient.issue.internal;

import org.sonar.wsclient.issue.BulkChange;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 3.7
 */
public class DefaultBulkChange implements BulkChange {

  private int totalIssuesChanged;
  private int totalIssuesNotChanged;
  private final List<String> issuesNotChangedKeys = new ArrayList<String>();

  public List<String> issuesNotChangedKeys() {
    return issuesNotChangedKeys;
  }

  public int totalIssuesChanged() {
    return totalIssuesChanged;
  }

  public int totalIssuesNotChanged() {
    return totalIssuesNotChanged;
  }

  DefaultBulkChange setTotalIssuesChanged(int totalIssuesChanged) {
    this.totalIssuesChanged = totalIssuesChanged;
    return this;
  }

  DefaultBulkChange setTotalIssuesNotChanged(int totalIssuesNotChanged) {
    this.totalIssuesNotChanged = totalIssuesNotChanged;
    return this;
  }

  DefaultBulkChange setIssuesNotChanged(List<String> issueKeys) {
    issuesNotChangedKeys.addAll(issueKeys);
    return this;
  }

  DefaultBulkChange addIssueNotchanged(String issueKey) {
    issuesNotChangedKeys.add(issueKey);
    return this;
  }

}
