/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.db.organization;

import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class OrganizationDto {
  /** Technical unique identifier, can't be null */
  private String uuid;
  /** Functional unique identifier, can't be null */
  private String key;
  /** Name, can't be null */
  private String name;
  /** description can be null */
  private String description;
  /** url can be null */
  private String url;
  /** avatar url can be null */
  private String avatarUrl;

  /**
   * Flag indicated whether being root is required to be able to delete this organization.
   */
  private boolean guarded = false;
  /** If of the user for whom the organization was created, can be null. */
  private Integer userId;
  private Integer defaultGroupId;
  private String defaultQualityGateUuid;
  private long createdAt;
  private long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public OrganizationDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getKey() {
    return key;
  }

  public OrganizationDto setKey(String key) {
    this.key = key;
    return this;
  }

  public String getName() {
    return name;
  }

  public OrganizationDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public OrganizationDto setDescription(@Nullable String description) {
    this.description = description;
    return this;
  }

  public String getUrl() {
    return url;
  }

  public OrganizationDto setUrl(@Nullable String url) {
    this.url = url;
    return this;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public OrganizationDto setAvatarUrl(@Nullable String avatarUrl) {
    this.avatarUrl = avatarUrl;
    return this;
  }

  public boolean isGuarded() {
    return guarded;
  }

  public OrganizationDto setGuarded(boolean guarded) {
    this.guarded = guarded;
    return this;
  }

  @CheckForNull
  public Integer getUserId() {
    return userId;
  }

  public OrganizationDto setUserId(@Nullable Integer userId) {
    this.userId = userId;
    return this;
  }

  @CheckForNull
  public Integer getDefaultGroupId() {
    return defaultGroupId;
  }

  public OrganizationDto setDefaultGroupId(@Nullable Integer defaultGroupId) {
    this.defaultGroupId = defaultGroupId;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public OrganizationDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  public OrganizationDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getDefaultQualityGateUuid() {
    return defaultQualityGateUuid;
  }

  public OrganizationDto setDefaultQualityGateUuid(String defaultQualityGateUuid) {
    this.defaultQualityGateUuid = defaultQualityGateUuid;
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
    OrganizationDto that = (OrganizationDto) o;
    return Objects.equals(uuid, that.uuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }

  @Override
  public String toString() {
    return "OrganizationDto{" +
      "uuid='" + uuid + '\'' +
      ", key='" + key + '\'' +
      ", name='" + name + '\'' +
      ", description='" + description + '\'' +
      ", url='" + url + '\'' +
      ", avatarUrl='" + avatarUrl + '\'' +
      ", guarded=" + guarded +
      ", userId=" + userId +
      ", defaultQualityGateUuid=" + defaultQualityGateUuid +
      ", createdAt=" + createdAt +
      ", updatedAt=" + updatedAt +
      '}';
  }

}
