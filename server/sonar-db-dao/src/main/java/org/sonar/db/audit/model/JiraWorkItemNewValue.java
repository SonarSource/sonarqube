/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.audit.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class JiraWorkItemNewValue extends NewValue {

  @Nullable
  private String workItemId;

  @Nullable
  private String jiraIssueId;

  @Nullable
  private String jiraIssueKey;

  @Nullable
  private String jiraIssueUrl;

  @Nullable
  private String jiraProjectBindingId;

  public JiraWorkItemNewValue(String workItemId, String jiraIssueId, String jiraIssueKey, String jiraIssueUrl, String jiraProjectBindingId) {
    this.workItemId = workItemId;
    this.jiraIssueId = jiraIssueId;
    this.jiraIssueKey = jiraIssueKey;
    this.jiraIssueUrl = jiraIssueUrl;
    this.jiraProjectBindingId = jiraProjectBindingId;
  }

  @CheckForNull
  public String getWorkItemId() {
    return this.workItemId;
  }

  @CheckForNull
  public String getJiraIssueId() {
    return this.jiraIssueId;
  }

  @CheckForNull
  public String getJiraIssueKey() {
    return this.jiraIssueKey;
  }

  @CheckForNull
  public String getJiraIssueUrl() {
    return this.jiraIssueUrl;
  }

  @CheckForNull
  public String getJiraProjectBindingId() {
    return this.jiraProjectBindingId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"workItemId\": ", this.workItemId, true);
    addField(sb, "\"jiraIssueId\": ", this.jiraIssueId, true);
    addField(sb, "\"jiraIssueKey\": ", this.jiraIssueKey, true);
    addField(sb, "\"jiraIssueUrl\": ", this.jiraIssueUrl, true);
    addField(sb, "\"jiraProjectBindingId\": ", this.jiraProjectBindingId, true);
    endString(sb);
    return sb.toString();
  }
}
