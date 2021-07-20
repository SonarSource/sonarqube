/*
 * SonarQube
 * Copyright (C) 2009-2021 SonarSource SA
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
import org.sonar.db.user.GroupDto;
import org.sonar.db.user.UserDto;
import org.sonar.db.user.UserGroupDto;

public class UserGroupNewValue implements NewValue {

  @Nullable
  private String groupUuid;

  @Nullable
  private String name;

  @Nullable
  private String description;

  @Nullable
  private String userUuid;

  @Nullable
  private String userLogin;

  public UserGroupNewValue(String groupUuid, String name) {
    this.groupUuid = groupUuid;
    this.name = name;
  }

  public UserGroupNewValue(GroupDto groupDto) {
    this.groupUuid = groupDto.getUuid();
    this.name = groupDto.getName();
    this.description = groupDto.getDescription();
  }

  public UserGroupNewValue(GroupDto groupDto, UserDto userDto) {
    this.groupUuid = groupDto.getUuid();
    this.name = groupDto.getName();
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public UserGroupNewValue(UserDto userDto) {
    this.userUuid = userDto.getUuid();
    this.userLogin = userDto.getLogin();
  }

  public UserGroupNewValue(UserGroupDto userGroupDto, String groupName, String userLogin) {
    this.groupUuid = userGroupDto.getGroupUuid();
    this.userUuid = userGroupDto.getUserUuid();
    this.name = groupName;
    this.userLogin = userLogin;
  }

  @CheckForNull
  public String getGroupUuid() {
    return this.groupUuid;
  }

  @CheckForNull
  public String getName() {
    return this.name;
  }

  @CheckForNull
  public String getDescription() {
    return this.description;
  }

  @CheckForNull
  public String getUserUuid() {
    return this.userUuid;
  }

  @CheckForNull
  public String getUserLogin() {
    return this.userLogin;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("{");
    addField(sb, "\"groupUuid\": ", this.groupUuid, true);
    addField(sb, "\"name\": ", this.name, true);
    addField(sb, "\"description\": ", this.description, true);
    addField(sb, "\"userUuid\": ", this.userUuid, true);
    addField(sb, "\"userLogin\": ", this.userLogin, true);
    endString(sb);
    return sb.toString();
  }
}
