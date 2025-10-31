/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.jira.dto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;

public final class JiraWorkItemDto implements Serializable {

  private String id;
  private String jiraIssueId;
  private String jiraIssueKey;
  private String jiraIssueUrl;
  private String jiraProjectBindingId;

  private long createdAt;
  private long updatedAt;

  public JiraWorkItemDto() {
    // Sonar S1258
  }

  @CheckForNull
  public String getId() {
    return id;
  }

  public JiraWorkItemDto setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public JiraWorkItemDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public JiraWorkItemDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getJiraIssueId() {
    return jiraIssueId;
  }

  public JiraWorkItemDto setJiraIssueId(String jiraIssueId) {
    this.jiraIssueId = jiraIssueId;
    return this;
  }

  public String getJiraIssueKey() {
    return jiraIssueKey;
  }

  public JiraWorkItemDto setJiraIssueKey(String jiraIssueKey) {
    this.jiraIssueKey = jiraIssueKey;
    return this;
  }

  public String getJiraIssueUrl() {
    return jiraIssueUrl;
  }

  public JiraWorkItemDto setJiraIssueUrl(String jiraIssueUrl) {
    this.jiraIssueUrl = jiraIssueUrl;
    return this;
  }

  public String getJiraProjectBindingId() {
    return jiraProjectBindingId;
  }

  public JiraWorkItemDto setJiraProjectBindingId(String jiraProjectBindingId) {
    this.jiraProjectBindingId = jiraProjectBindingId;
    return this;
  }

  @Override
  public String toString() {
    return "JiraWorkItemDto{" +
      "id='" + id + '\'' +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      ", jiraIssueId='" + jiraIssueId + '\'' +
      ", jiraIssueKey='" + jiraIssueKey + '\'' +
      ", jiraIssueUrl='" + jiraIssueUrl + '\'' +
      ", jiraProjectBindingId='" + jiraProjectBindingId + '\'' +
      '}';
  }

}
