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
package org.sonar.db.jira.dto;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.io.Serializable;

public final class JiraProjectBindingDto implements Serializable {

  private String id;
  private String sonarProjectId;
  private String jiraOrganizationBindingId;
  private String jiraProjectKey;

  private long createdAt;
  private long updatedAt;

  public JiraProjectBindingDto() {
    // Sonar S1258
  }

  @CheckForNull
  public String getId() {
    return id;
  }

  public JiraProjectBindingDto setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  public String getSonarProjectId() {
    return sonarProjectId;
  }

  public JiraProjectBindingDto setSonarProjectId(String sonarProjectId) {
    this.sonarProjectId = sonarProjectId;
    return this;
  }

  public String getJiraOrganizationBindingId() {
    return jiraOrganizationBindingId;
  }

  public JiraProjectBindingDto setJiraOrganizationBindingId(String jiraOrganizationBindingId) {
    this.jiraOrganizationBindingId = jiraOrganizationBindingId;
    return this;
  }

  public String getJiraProjectKey() {
    return jiraProjectKey;
  }

  public JiraProjectBindingDto setJiraProjectKey(String jiraProjectKey) {
    this.jiraProjectKey = jiraProjectKey;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public JiraProjectBindingDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public JiraProjectBindingDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  @Override
  public String toString() {
    return "JiraProjectBindingDto{" +
      "id='" + id + '\'' +
      ", sonarProjectId='" + sonarProjectId + '\'' +
      ", jiraOrganizationBindingId='" + jiraOrganizationBindingId + '\'' +
      ", jiraProjectKey='" + jiraProjectKey + '\'' +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      '}';
  }

}
