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

public class RemoveGroupRequest {
  private String organization;
  private String groupId;
  private String groupName;
  private String permission;
  private String projectId;
  private String projectKey;

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public RemoveGroupRequest setOrganization(@Nullable String s) {
    this.organization = s;
    return this;
  }

  @CheckForNull
  public String getGroupId() {
    return groupId;
  }

  public RemoveGroupRequest setGroupId(@Nullable String groupId) {
    this.groupId = groupId;
    return this;
  }

  @CheckForNull
  public String getGroupName() {
    return groupName;
  }

  public RemoveGroupRequest setGroupName(@Nullable String groupName) {
    this.groupName = groupName;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public RemoveGroupRequest setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  @CheckForNull
  public String getProjectId() {
    return projectId;
  }

  public RemoveGroupRequest setProjectId(@Nullable String projectId) {
    this.projectId = projectId;
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  public RemoveGroupRequest setProjectKey(@Nullable String projectKey) {
    this.projectKey = projectKey;
    return this;
  }
}
