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

import static java.util.Objects.requireNonNull;

public class AddGroupWsRequest {
  private String permission;
  private String groupId;
  private String organization;
  private String groupName;
  private String projectId;
  private String projectKey;

  public String getPermission() {
    return permission;
  }

  public AddGroupWsRequest setPermission(String permission) {
    this.permission = requireNonNull(permission, "permission must not be null");
    return this;
  }

  @CheckForNull
  public String getGroupId() {
    return groupId;
  }

  public AddGroupWsRequest setGroupId(@Nullable String groupId) {
    this.groupId = groupId;
    return this;
  }

  @CheckForNull
  public String getOrganization() {
    return organization;
  }

  public AddGroupWsRequest setOrganization(@Nullable String s) {
    this.organization = s;
    return this;
  }

  @CheckForNull
  public String getGroupName() {
    return groupName;
  }

  public AddGroupWsRequest setGroupName(@Nullable String groupName) {
    this.groupName = groupName;
    return this;
  }

  @CheckForNull
  public String getProjectId() {
    return projectId;
  }

  public AddGroupWsRequest setProjectId(@Nullable String projectId) {
    this.projectId = projectId;
    return this;
  }

  @CheckForNull
  public String getProjectKey() {
    return projectKey;
  }

  public AddGroupWsRequest setProjectKey(@Nullable String projectKey) {
    this.projectKey = projectKey;
    return this;
  }
}
