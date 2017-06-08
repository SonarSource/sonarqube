/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
 * Represents the table "rules_profiles"
 */
public class RulesProfileDto {

  private Integer id;
  private String kee;
  private String name;
  private String language;
  private String rulesUpdatedAt;
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
