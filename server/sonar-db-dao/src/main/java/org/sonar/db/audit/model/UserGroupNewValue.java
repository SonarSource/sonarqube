/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import static com.google.common.base.Preconditions.checkState;

public class UserGroupNewValue extends NewValue {

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  private final String groupUuid;
  private final String name;

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
  private final String userUuid;
  private final String userLogin;
  private final String description;

  public UserGroupNewValue(String groupUuid, String name) {
    this(groupUuid, name, null, null, null);
  }

  public UserGroupNewValue(GroupDto groupDto) {
    this(groupDto.getUuid(), groupDto.getName(), null, null, groupDto.getDescription());
  }

  public UserGroupNewValue(GroupDto groupDto, UserDto userDto) {
    this(groupDto.getUuid(), groupDto.getName(), userDto.getUuid(), userDto.getLogin(), null);
  }

  public UserGroupNewValue(UserDto userDto) {
    this(null, null, userDto.getUuid(), userDto.getLogin(), null);
  }

  public UserGroupNewValue(UserGroupDto userGroupDto, String groupName, String userLogin) {
    this(userGroupDto.getGroupUuid(), groupName, userGroupDto.getUserUuid(), userLogin, null);
  }

  private UserGroupNewValue(@Nullable String groupUuid, @Nullable String name, @Nullable String userUuid, @Nullable String userLogin, @Nullable String description) {
    checkState((groupUuid != null && name != null) || (userUuid != null && userLogin != null));
    this.groupUuid = groupUuid;
    this.name = name;
    this.userUuid = userUuid;
    this.userLogin = userLogin;
    this.description = description;
  }

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
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

  /**
   * @deprecated The uuids in the audit logs are not product requirement anymore and will be removed in 11.x
   */
  @Deprecated(since = "10.2")
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
