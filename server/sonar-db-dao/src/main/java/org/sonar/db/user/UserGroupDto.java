/*
 * SonarQube
 * Copyright (C) 2009-2023 SonarSource SA
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

import java.util.Objects;

public class UserGroupDto {
  private String uuid;
  private String userUuid;
  private String groupUuid;

  public UserGroupDto() {
    //
  }

  public String getUuid() {
    return uuid;
  }

  public UserGroupDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public UserGroupDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public String getGroupUuid() {
    return groupUuid;
  }

  public UserGroupDto setGroupUuid(String groupUuid) {
    this.groupUuid = groupUuid;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UserGroupDto that = (UserGroupDto) o;
    return Objects.equals(userUuid, that.userUuid) && Objects.equals(groupUuid, that.groupUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userUuid, groupUuid);
  }
}
