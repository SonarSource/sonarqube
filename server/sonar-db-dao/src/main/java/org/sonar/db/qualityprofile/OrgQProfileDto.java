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
package org.sonar.db.qualityprofile;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * Maps the table "org_qprofiles", which represents the profiles
 * available in an organization.
 *
 * Extracting organizations from table "rules_profiles"
 * allows to optimize storage and synchronization of built-in
 * profiles by implementing an alias mechanism. A built-in profile
 * is stored once in table "rules_profiles" and is referenced
 * multiple times in table "org_qprofiles".
 * User profiles are not shared across organizations, then one row
 * in "rules_profiles" is referenced by a single row in
 * "org_qprofiles".
 */
public class OrgQProfileDto {

  private String uuid;

  /**
   * UUID of organization. Not null.
   */
  private String organizationUuid;

  /**
   * UUID of referenced row in table "rules_profiles". Not null.
   */
  private String rulesProfileUuid;

  private String parentUuid;
  private Long lastUsed;
  private Long userUpdatedAt;

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public OrgQProfileDto setOrganizationUuid(String organizationUuid) {
    this.organizationUuid = organizationUuid;
    return this;
  }

  public String getUuid() {
    return uuid;
  }

  public OrgQProfileDto setUuid(String s) {
    this.uuid = s;
    return this;
  }

  public String getRulesProfileUuid() {
    return rulesProfileUuid;
  }

  public OrgQProfileDto setRulesProfileUuid(String s) {
    this.rulesProfileUuid = s;
    return this;
  }

  @CheckForNull
  public String getParentUuid() {
    return parentUuid;
  }

  public OrgQProfileDto setParentUuid(@Nullable String s) {
    this.parentUuid = s;
    return this;
  }

  @CheckForNull
  public Long getLastUsed() {
    return lastUsed;
  }

  public OrgQProfileDto setLastUsed(@Nullable Long lastUsed) {
    this.lastUsed = lastUsed;
    return this;
  }

  @CheckForNull
  public Long getUserUpdatedAt() {
    return userUpdatedAt;
  }

  public OrgQProfileDto setUserUpdatedAt(@Nullable Long userUpdatedAt) {
    this.userUpdatedAt = userUpdatedAt;
    return this;
  }

  public static OrgQProfileDto from(QProfileDto qProfileDto) {
    return new OrgQProfileDto()
      .setUuid(qProfileDto.getKee())
      .setOrganizationUuid(qProfileDto.getOrganizationUuid())
      .setRulesProfileUuid(qProfileDto.getRulesProfileUuid())
      .setParentUuid(qProfileDto.getParentKee())
      .setLastUsed(qProfileDto.getLastUsed())
      .setUserUpdatedAt(qProfileDto.getUserUpdatedAt());
  }
}
