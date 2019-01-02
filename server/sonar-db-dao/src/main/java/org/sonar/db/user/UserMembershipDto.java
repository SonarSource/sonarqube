/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.user;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class UserMembershipDto {

  private Long id;
  private Integer groupId;
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
  public Integer getGroupId() {
    return groupId;
  }

  public UserMembershipDto setGroupId(@Nullable Integer groupId) {
    this.groupId = groupId;
    return this;
  }
}
