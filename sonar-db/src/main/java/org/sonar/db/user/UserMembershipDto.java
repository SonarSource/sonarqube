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
package org.sonar.db.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserMembershipDto {

  private Long id;
  private Long groupId;
  private String login;
  private String name;

  public Long getId() {
    return id;
  }

  public UserMembershipDto setId(Long id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public UserMembershipDto setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getLogin() {
    return login;
  }

  public UserMembershipDto setLogin(@Nullable String login) {
    this.login = login;
    return this;
  }

  @CheckForNull
  public Long getGroupId() {
    return groupId;
  }

  public UserMembershipDto setGroupId(@Nullable Long groupId) {
    this.groupId = groupId;
    return this;
  }
}
