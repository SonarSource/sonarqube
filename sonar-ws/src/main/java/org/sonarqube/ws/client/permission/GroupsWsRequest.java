/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
package org.sonarqube.ws.client.permission;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class GroupsWsRequest {
  private String permission;
  private String projectId;
  private String projectKey;
  private Integer page;
  private Integer pageSize;
  private String query;

  @CheckForNull
  public String getPermission() {
    return permission;
  }

  public GroupsWsRequest setPermission(@Nullable String permission) {
    this.permission = permission;
    return this;
  }

  @CheckForNull
  public String getProjectId() {
    return projectId;
  }

  public GroupsWsRequest setProjectId(@Nullable String projectId) {
    this.projectId = projectId;
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  public GroupsWsRequest setProjectKey(String projectKey) {
    this.projectKey = projectKey;
    return this;
  }

  @CheckForNull
  public Integer getPage() {
    return page;
  }

  public GroupsWsRequest setPage(int page) {
    this.page = page;
    return this;
  }

  @CheckForNull
  public Integer getPageSize() {
    return pageSize;
  }

  public GroupsWsRequest setPageSize(int pageSize) {
    this.pageSize = pageSize;
    return this;
  }

  @CheckForNull
  public String getQuery() {
    return query;
  }

  public GroupsWsRequest setQuery(@Nullable String query) {
    this.query = query;
    return this;
  }
}
