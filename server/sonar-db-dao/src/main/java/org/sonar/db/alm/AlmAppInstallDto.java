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
package org.sonar.db.alm;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AlmAppInstallDto {

  /**
   * Technical unique identifier, can't be null
   */
  private String uuid;
  /**
   * alm_id, can't be null
   */
  private String almId;
  /**
   * Owner id, can't be null
   */
  private String ownerId;
  /**
   * Installation id, can't be null
   */
  private String installId;
  /**
   * Is owner a user, can be null
   */
  private Boolean isOwnerUser;
  /**
   * The user ID provided by the ALM of the user who has installed the ALM installation. Can be null as some ALM doesn't provide this info. Max size is 255.
   */
  private String userExternalId;

  private long createdAt;
  private long updatedAt;

  public String getUuid() {
    return uuid;
  }

  public AlmAppInstallDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getAlmId() {
    return almId;
  }

  public ALM getAlm() {
    return ALM.fromId(almId);
  }

  public AlmAppInstallDto setAlmId(String almId) {
    this.almId = almId;
    return this;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public AlmAppInstallDto setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public String getInstallId() {
    return installId;
  }

  public AlmAppInstallDto setInstallId(String installId) {
    this.installId = installId;
    return this;
  }

  public boolean isOwnerUser() {
    return isOwnerUser;
  }

  public AlmAppInstallDto setIsOwnerUser(boolean isOwnerUser) {
    this.isOwnerUser = isOwnerUser;
    return this;
  }

  @CheckForNull
  public String getUserExternalId() {
    return userExternalId;
  }

  public AlmAppInstallDto setUserExternalId(@Nullable String userExternalId) {
    this.userExternalId = userExternalId;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  AlmAppInstallDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }

  AlmAppInstallDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}
