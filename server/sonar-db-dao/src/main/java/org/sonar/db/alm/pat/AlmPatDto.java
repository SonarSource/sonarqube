/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource SA
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
package org.sonar.db.alm.pat;

public class AlmPatDto {

  private String uuid;
  private String personalAccessToken;
  private String userUuid;
  private String almSettingUuid;

  private long updatedAt;
  private long createdAt;

  public String getAlmSettingUuid() {
    return almSettingUuid;
  }

  public AlmPatDto setAlmSettingUuid(String almSettingUuid) {
    this.almSettingUuid = almSettingUuid;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public AlmPatDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public String getPersonalAccessToken() {
    return personalAccessToken;
  }

  public AlmPatDto setPersonalAccessToken(String personalAccessToken) {
    this.personalAccessToken = personalAccessToken;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
  }
}
