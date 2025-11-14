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
package org.sonar.db.project;

public class ProjectBadgeTokenDto {

  private String uuid;
  private String token;
  private String projectUuid;
  private long createdAt;
  private long updatedAt;

  public ProjectBadgeTokenDto() {
    // to keep for mybatis
  }

  public ProjectBadgeTokenDto(String uuid, String token, String projectUuid, long createdAt, long updatedAt) {
    this.uuid = uuid;
    this.token = token;
    this.projectUuid = projectUuid;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getUuid() {
    return uuid;
  }

  public ProjectBadgeTokenDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getToken() {
    return token;
  }

  public ProjectBadgeTokenDto setToken(String token) {
    this.token = token;
    return this;
  }

  public String getProjectUuid() {
    return projectUuid;
  }

  public ProjectBadgeTokenDto setProjectUuid(String projectUuid) {
    this.projectUuid = projectUuid;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public ProjectBadgeTokenDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public ProjectBadgeTokenDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
