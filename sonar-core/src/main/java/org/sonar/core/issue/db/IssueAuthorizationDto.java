/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
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

package org.sonar.core.issue.db;

import org.sonar.core.persistence.Dto;

import java.io.Serializable;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

public final class IssueAuthorizationDto extends Dto<String> implements Serializable {

  private String projectUuid;
  private String permission;
  private List<String> groups = newArrayList();
  private List<String> users = newArrayList();

  @Override
  public String getKey() {
    return projectUuid;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public IssueAuthorizationDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public String getPermission() {
    return permission;
  }

  public IssueAuthorizationDto setPermission(String permission) {
    this.permission = permission;
    return this;
  }

  public List<String> getGroups() {
    return groups;
  }

  public IssueAuthorizationDto setGroups(List<String> groups) {
    this.groups = groups;
    return this;
  }

  public IssueAuthorizationDto addGroup(String group) {
    groups.add(group);
    return this;
  }

  public List<String> getUsers() {
    return users;
  }

  public IssueAuthorizationDto setUsers(List<String> users) {
    this.users = users;
    return this;
  }

  public IssueAuthorizationDto addUser(String user) {
    users.add(user);
    return this;
  }

}
