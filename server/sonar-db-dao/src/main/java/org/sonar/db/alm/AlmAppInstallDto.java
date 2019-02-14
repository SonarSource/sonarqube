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
package org.sonar.db.alm;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AlmAppInstallDto {

  /**
   * Technical unique identifier, can't be null
   */
  private String uuid;
  /**
   * Identifier of the ALM, like 'bitbucketcloud' or 'github', can't be null.
   * Note that the db column is named alm_id.
   *
   * @see ALM for the list of available values
   */
  private String rawAlm;
  /**
   * ALM specific identifier of the organization, like team or user uuid for Bitbucket Cloud or organization id for Github, can't be null.
   * Note that the column is badly named owner_id, in the first place it was only possible to install personal organizations.
   * The column name has been kept to prevent doing a db migration.
   */
  private String organizationAlmId;
  /**
   * ALM specific identifier of the app installation, can't be null
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

  public ALM getAlm() {
    return ALM.fromId(rawAlm);
  }

  public AlmAppInstallDto setAlm(ALM alm) {
    this.rawAlm = alm.getId();
    return this;
  }

  public String getOrganizationAlmId() {
    return organizationAlmId;
  }

  public AlmAppInstallDto setOrganizationAlmId(String organizationAlmId) {
    this.organizationAlmId = organizationAlmId;
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
