/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.Date;
import java.util.Objects;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.core.util.UtcDateUtils;
import org.sonar.db.organization.OrganizationDto;

/**
 * Represents the join of "org_qprofiles" and "rules_profiles"
 */
public class QProfileDto implements Comparable<QProfileDto> {

  /**
   * The organization, that this quality profile belongs to.
   * Must not be null, but can be the default organization's uuid.
   * Refers to {@link OrganizationDto#getUuid()}.
   */
  private String organizationUuid;
  private String kee;
  private String name;
  private String language;
  private String parentKee;
  private String rulesUpdatedAt;
  private Long lastUsed;
  private Long userUpdatedAt;
  private boolean isBuiltIn;
  private boolean isDefault;
  private String rulesProfileUuid;

  public String getOrganizationUuid() {
    return organizationUuid;
  }

  public QProfileDto setOrganizationUuid(String s) {
    this.organizationUuid = s;
    return this;
  }

  public String getKee() {
    return kee;
  }

  public QProfileDto setKee(String s) {
    this.kee = s;
    return this;
  }

  public String getRulesProfileUuid() {
    return rulesProfileUuid;
  }

  public QProfileDto setRulesProfileUuid(String s) {
    this.rulesProfileUuid = s;
    return this;
  }

  public String getName() {
    return name;
  }

  public QProfileDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public QProfileDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  @CheckForNull
  public String getParentKee() {
    return parentKee;
  }

  public QProfileDto setParentKee(@Nullable String s) {
    this.parentKee = s;
    return this;
  }

  public String getRulesUpdatedAt() {
    return rulesUpdatedAt;
  }

  public QProfileDto setRulesUpdatedAt(String s) {
    this.rulesUpdatedAt = s;
    return this;
  }

  public QProfileDto setRulesUpdatedAtAsDate(Date d) {
    this.rulesUpdatedAt = UtcDateUtils.formatDateTime(d);
    return this;
  }

  @CheckForNull
  public Long getLastUsed() {
    return lastUsed;
  }

  public QProfileDto setLastUsed(@Nullable Long lastUsed) {
    this.lastUsed = lastUsed;
    return this;
  }

  @CheckForNull
  public Long getUserUpdatedAt() {
    return userUpdatedAt;
  }

  public QProfileDto setUserUpdatedAt(@Nullable Long userUpdatedAt) {
    this.userUpdatedAt = userUpdatedAt;
    return this;
  }

  public boolean isBuiltIn() {
    return isBuiltIn;
  }

  public QProfileDto setIsBuiltIn(boolean b) {
    this.isBuiltIn = b;
    return this;
  }

  public boolean isDefault() {
    return isDefault;
  }

  public QProfileDto setIsDefault(boolean b) {
    this.isDefault = b;
    return this;
  }

  public static QProfileDto from(OrgQProfileDto org, RulesProfileDto rules) {
    return new QProfileDto()
      .setIsBuiltIn(rules.isBuiltIn())
      .setIsDefault(rules.isDefault())
      .setKee(org.getUuid())
      .setParentKee(org.getParentUuid())
      .setRulesProfileUuid(rules.getUuid())
      .setLanguage(rules.getLanguage())
      .setName(rules.getName())
      .setRulesUpdatedAt(rules.getRulesUpdatedAt())
      .setLastUsed(org.getLastUsed())
      .setUserUpdatedAt(org.getUserUpdatedAt());
  }

  @Override
  public int compareTo(QProfileDto o) {
    return kee.compareTo(o.kee);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    QProfileDto that = (QProfileDto) o;
    return kee.equals(that.kee);
  }

  @Override
  public int hashCode() {
    return Objects.hash(kee);
  }
}
