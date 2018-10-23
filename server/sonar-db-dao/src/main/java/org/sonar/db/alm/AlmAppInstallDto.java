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

  private long createdAt;
  private long updatedAt;

  public AlmAppInstallDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public AlmAppInstallDto setAlmId(String almId) {
    this.almId = almId;
    return this;
  }

  public AlmAppInstallDto setOwnerId(String ownerId) {
    this.ownerId = ownerId;
    return this;
  }

  public AlmAppInstallDto setInstallId(String installId) {
    this.installId = installId;
    return this;
  }

  public AlmAppInstallDto setIsOwnerUser(@Nullable Boolean isOwnerUser) {
    this.isOwnerUser = isOwnerUser;
    return this;
  }

  AlmAppInstallDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  AlmAppInstallDto setUpdatedAt(long updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public String getAlmId() {
    return almId;
  }

  public ALM getAlm() {
    return ALM.fromId(almId);
  }

  public String getOwnerId() {
    return ownerId;
  }

  public String getInstallId() {
    return installId;
  }

  @Nullable
  public Boolean isOwnerUser() {
    return isOwnerUser;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public long getUpdatedAt() {
    return updatedAt;
  }
}
