/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.core.qualityprofile.db;

public class ActiveRuleParamDto {

  private Integer id;
  private Integer activeRuleId;
  private Integer rulesParameterId;
  private String kee;
  private String value;

  public Integer getId() {
    return id;
  }

  public ActiveRuleParamDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getActiveRuleId() {
    return activeRuleId;
  }

  public ActiveRuleParamDto setActiveRuleId(Integer activeRuleId) {
    this.activeRuleId = activeRuleId;
    return this;
  }

  public Integer getRulesParameterId() {
    return rulesParameterId;
  }

  public ActiveRuleParamDto setRulesParameterId(Integer rulesParameterId) {
    this.rulesParameterId = rulesParameterId;
    return this;
  }

  public String getKey() {
    return kee;
  }

  public ActiveRuleParamDto setKey(String key) {
    this.kee = key;
    return this;
  }

  public String getValue() {
    return value;
  }

  public ActiveRuleParamDto setValue(String value) {
    this.value = value;
    return this;
  }

}
