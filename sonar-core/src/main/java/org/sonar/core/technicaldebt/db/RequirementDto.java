/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2013 SonarSource
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
package org.sonar.core.technicaldebt.db;

import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.technicaldebt.DefaultRequirement;

import java.util.List;

public class RequirementDto {

  private Long id;
  private RuleDto rule;
  private List<RequirementPropertyDto> properties;
  private CharacteristicDto characteristic;
  private CharacteristicDto rootCharacteristic;

  public Long getId() {
    return id;
  }

  public RequirementDto setId(Long id) {
    this.id = id;
    return this;
  }

  public RuleDto getRule() {
    return rule;
  }

  public RequirementDto setRule(RuleDto rule) {
    this.rule = rule;
    return this;
  }

  public CharacteristicDto getRootCharacteristic() {
    return rootCharacteristic;
  }

  public RequirementDto setRootCharacteristic(CharacteristicDto rootCharacteristic) {
    this.rootCharacteristic = rootCharacteristic;
    return this;
  }

  public CharacteristicDto getCharacteristic() {
    return characteristic;
  }

  public RequirementDto setCharacteristic(CharacteristicDto characteristic) {
    this.characteristic = characteristic;

    return this;
  }

  public List<RequirementPropertyDto> getProperties() {
    return properties;
  }

  public RequirementDto setProperties(List<RequirementPropertyDto> properties) {
    this.properties = properties;
    return this;
  }

  public DefaultRequirement toDefaultRequirement() {
    DefaultRequirement requirement = new DefaultRequirement()
      .setRuleKey(RuleKey.of(rule.getRepositoryKey(), rule.getRuleKey()))
      .setCharacteristic(characteristic.toDefaultCharacteristic())
      .setRootCharacteristic(rootCharacteristic.toDefaultCharacteristic());
    for (RequirementPropertyDto property : properties) {
      requirement.addProperty(property.toDefaultRequirementProperty());
    }
    return requirement;
  }
}
