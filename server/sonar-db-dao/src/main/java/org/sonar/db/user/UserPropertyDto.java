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

public class UserPropertyDto {

  /**
   * Unique UUID identifier. Max size is 40. Can't be null.
   */
  private String uuid;

  /**
   * The UUID of the user the settings belongs to. Max size is 255. Can't be null.
   */
  private String userUuid;

  /**
   * The key of the settings. Max size is 100. Can't be null.
   */
  private String key;

  /**
   * The value of the settings. Max size is 4000. Can't be null.
   */
  private String value;

  public String getUuid() {
    return uuid;
  }

  UserPropertyDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public UserPropertyDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public String getKey() {
    return key;
  }

  public UserPropertyDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public UserPropertyDto setValue(String value) {
    this.value = value;
    return this;
  }

}
