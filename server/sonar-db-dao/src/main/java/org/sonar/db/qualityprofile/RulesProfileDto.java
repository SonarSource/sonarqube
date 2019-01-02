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
package org.sonar.db.qualityprofile;

import java.util.Date;
import org.sonar.core.util.UtcDateUtils;

/**
 * Maps the table "rules_profiles", which represents
 * a group of active rules.
 *
 * Can be:
 * - a built-in profile, referenced by multiple organizations
 *   through table "org_qprofiles".
 * - a profile created by user and referenced by one, and only one,
 *   organization in the table "org_qprofiles"
 */
public class RulesProfileDto {

  /**
   * Legacy db-generated ID. Usages should be replaced by {@link #kee}.
   */
  private Integer id;

  /**
   * UUID. Can be a unique slug on legacy rows, for example "abap-sonar-way-38370".
   */
  private String kee;

  /**
   * Name displayed to users, for example "Sonar way". Not null.
   */
  private String name;

  /**
   * Language key, for example "java". Not null.
   */
  private String language;

  /**
   * Date of last update of rule configuration (activation/deactivation/change of parameter).
   * It does not include profile renaming.
   * Not null.
   */
  private String rulesUpdatedAt;

  /**
   * Whether profile is built-in or created by a user.
   * A built-in profile is read-only. Its definition is provided by a language plugin.
   */
  private boolean isBuiltIn;

  public String getKee() {
    return kee;
  }

  public RulesProfileDto setKee(String s) {
    this.kee = s;
    return this;
  }

  public Integer getId() {
    return id;
  }

  public RulesProfileDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public String getName() {
    return name;
  }

  public RulesProfileDto setName(String name) {
    this.name = name;
    return this;
  }

  public String getLanguage() {
    return language;
  }

  public RulesProfileDto setLanguage(String language) {
    this.language = language;
    return this;
  }

  public String getRulesUpdatedAt() {
    return rulesUpdatedAt;
  }

  public RulesProfileDto setRulesUpdatedAt(String s) {
    this.rulesUpdatedAt = s;
    return this;
  }

  public RulesProfileDto setRulesUpdatedAtAsDate(Date d) {
    this.rulesUpdatedAt = UtcDateUtils.formatDateTime(d);
    return this;
  }

  public boolean isBuiltIn() {
    return isBuiltIn;
  }

  public RulesProfileDto setIsBuiltIn(boolean b) {
    this.isBuiltIn = b;
    return this;
  }

  public static RulesProfileDto from(QProfileDto qProfileDto) {
    return new RulesProfileDto()
      .setKee(qProfileDto.getRulesProfileUuid())
      .setLanguage(qProfileDto.getLanguage())
      .setName(qProfileDto.getName())
      .setIsBuiltIn(qProfileDto.isBuiltIn())
      .setId(qProfileDto.getId())
      .setRulesUpdatedAt(qProfileDto.getRulesUpdatedAt());
  }
}
