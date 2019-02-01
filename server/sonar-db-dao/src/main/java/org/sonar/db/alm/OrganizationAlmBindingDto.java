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

import java.util.Arrays;

/**
 * This DTO is used to link an {@link org.sonar.db.organization.OrganizationDto} to a {@link AlmAppInstallDto}
 */
public class OrganizationAlmBindingDto {

  /**
   * Not empty. Max size is 40. Obviously it is unique.
   */
  private String uuid;
  /**
   * The UUID of the organization. Can't be null. Max size is 40.
   * It's unique, as an organization is only linked to one installation (at least for the moment).
   */
  private String organizationUuid;
  /**
   * The UUID of ALM installation. Can't be null. Max size is 40.
   * It's unique, as an installation is related to only one organization.
   */
  private String almAppInstallUuid;
  /**
   * The id of the ALM. Can't be null. Max size is 40.
   */
  private String rawAlmId;
  /**
   * The url of the ALM organization. Can't be null. Max size is 2000.
   */
  private String url;
  /**
   * The UUID of the user who has created the link between the organization and the ALM installation. Can't be null. Max size is 255.
   */
  private String userUuid;
  /**
   * If the members of the org are automatically sync with the ALM org
   */
  private boolean membersSyncEnabled;
  /**
   * Technical creation date
   */
  private long createdAt;

  public String getUuid() {
    return uuid;
  }

  OrganizationAlmBindingDto setUuid(String uuid) {
    this.uuid = uuid;
    return this;
  }

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public OrganizationAlmBindingDto setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
    return this;
  }

  public String getAlmAppInstallUuid() {
    return almAppInstallUuid;
  }

  public OrganizationAlmBindingDto setAlmAppInstallUuid(String almAppInstallUuid) {
    this.almAppInstallUuid = almAppInstallUuid;
    return this;
  }

  public ALM getAlm() {
    return Arrays.stream(ALM.values())
      .filter(a -> a.getId().equals(rawAlmId))
      .findAny()
      .orElseThrow(() -> new IllegalStateException("ALM id " + rawAlmId + " is invalid"));
  }

  public OrganizationAlmBindingDto setAlmId(ALM alm) {
    this.rawAlmId = alm.getId();
    return this;
  }

  public String getUrl() {
    return url;
  }

  public OrganizationAlmBindingDto setUrl(String url) {
    this.url = url;
    return this;
  }

  public String getUserUuid() {
    return userUuid;
  }

  public OrganizationAlmBindingDto setUserUuid(String userUuid) {
    this.userUuid = userUuid;
    return this;
  }

  public boolean isMembersSyncEnable() {
    return membersSyncEnabled;
  }

  public OrganizationAlmBindingDto setMembersSyncEnabled(boolean membersSyncEnabled) {
    this.membersSyncEnabled = membersSyncEnabled;
    return this;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  OrganizationAlmBindingDto setCreatedAt(long createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
