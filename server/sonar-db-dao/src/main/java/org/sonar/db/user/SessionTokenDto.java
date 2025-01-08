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
package org.sonar.db.user;

public class SessionTokenDto {

  private String uuid;
  private String userUuid;
  private long expirationDate;
  private long createdAt;
  private long updatedAt;

  public String getUuid() {
    return uuid;
  }

  SessionTokenDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public SessionTokenDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public long getExpirationDate() {
    return expirationDate;
  }

  public SessionTokenDto setExpirationDate(long expirationDate) {
    this.expirationDate = expirationDate;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  SessionTokenDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  SessionTokenDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
