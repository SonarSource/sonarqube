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

import java.io.Serializable;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public final class JiraSelectedWorkTypeDto implements Serializable {

  private String id;
  private String jiraProjectBindingId;
  private String workTypeId;

  private long createdAt;
  private long updatedAt;

  public JiraSelectedWorkTypeDto() {
    // Sonar S1258
  }

  @CheckForNull
  public String getId() {
    return id;
  }

  public String getJiraProjectBindingId() {
    return jiraProjectBindingId;
  }

  public String getWorkTypeId() {
    return workTypeId;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public JiraSelectedWorkTypeDto setId(@Nullable String id) {
    this.id = id;
    return this;
  }

  public JiraSelectedWorkTypeDto setJiraProjectBindingId(String jiraProjectBindingId) {
    this.jiraProjectBindingId = jiraProjectBindingId;
    return this;
  }

  public JiraSelectedWorkTypeDto setWorkTypeId(String workTypeId) {
    this.workTypeId = workTypeId;
    return this;
  }

  public JiraSelectedWorkTypeDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public JiraSelectedWorkTypeDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

}
