/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.db.scim;

import java.util.Objects;

public class ScimUserDto {

  private final String scimUserUuid;
  private final String userUuid;

  public ScimUserDto(String scimUserUuid, String userUuid) {
    this.scimUserUuid = scimUserUuid;
    this.userUuid = userUuid;
  }

  public String getScimUserUuid() {
    return scimUserUuid;
  }


  public String getUserUuid() {
    return userUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ScimUserDto that = (ScimUserDto) o;
    return Objects.equals(scimUserUuid, that.scimUserUuid) && Objects.equals(userUuid, that.userUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(scimUserUuid, userUuid);
  }
}
