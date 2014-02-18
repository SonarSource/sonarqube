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
import org.sonar.api.technicaldebt.batch.internal.DefaultCharacteristic;
import org.sonar.api.technicaldebt.batch.internal.DefaultRequirement;
import org.sonar.api.utils.WorkDuration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.util.Date;

public class CharacteristicDto implements Serializable {

  public static final String DAYS = "d";
  public static final String MINUTES = "mn";
  public static final String HOURS = "h";

  private Integer id;
  private String kee;
  private String name;
  private Integer parentId;
  private Integer rootId;
  private Integer characteristicOrder;
  private Integer ruleId;
  private String functionKey;
  private Double factorValue;
  private String factorUnit;
  private Double offsetValue;
  private String offsetUnit;
  private Date createdAt;
  private Date updatedAt;
  private boolean enabled;

  public Integer getId() {
    return id;
  }

  public CharacteristicDto setId(Integer id) {
    this.id = id;
    return this;
  }

  @CheckForNull
  public String getKey() {
    return kee;
  }

  public CharacteristicDto setKey(@Nullable String s) {
    this.kee = s;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public CharacteristicDto setName(@Nullable String s) {
    this.name = s;
    return this;
  }

  @CheckForNull
  public Integer getParentId() {
    return parentId;
  }

  public CharacteristicDto setParentId(@Nullable Integer i) {
    this.parentId = i;
    return this;
  }

  @CheckForNull
  public Integer getRootId() {
    return rootId;
  }

  public CharacteristicDto setRootId(@Nullable Integer rootId) {
    this.rootId = rootId;
    return this;
  }

  @CheckForNull
  public Integer getOrder() {
    return characteristicOrder;
  }

  public CharacteristicDto setOrder(@Nullable Integer i) {
    this.characteristicOrder = i;
    return this;
  }

  @CheckForNull
  public Integer getRuleId() {
    return ruleId;
  }

  public CharacteristicDto setRuleId(@Nullable Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @CheckForNull
  public String getFunction() {
    return functionKey;
  }

  public CharacteristicDto setFunction(@Nullable String function) {
    this.functionKey = function;
    return this;
  }

  @CheckForNull
  public Double getFactorValue() {
    return factorValue;
  }

  public CharacteristicDto setFactorValue(Double factor) {
    this.factorValue = factor;
    return this;
  }

  @CheckForNull
  public String getFactorUnit() {
    return factorUnit;
  }

  public CharacteristicDto setFactorUnit(@Nullable String factorUnit) {
    this.factorUnit = factorUnit;
    return this;
  }

  @CheckForNull
  public Double getOffsetValue() {
    return offsetValue;
  }

  public CharacteristicDto setOffsetValue(@Nullable Double offset) {
    this.offsetValue = offset;
    return this;
  }

  @CheckForNull
  public String getOffsetUnit() {
    return offsetUnit;
  }

  public CharacteristicDto setOffsetUnit(@Nullable String offsetUnit) {
    this.offsetUnit = offsetUnit;
    return this;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public CharacteristicDto setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @CheckForNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  public CharacteristicDto setUpdatedAt(@Nullable Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public CharacteristicDto setEnabled(boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  public DefaultCharacteristic toCharacteristic(@Nullable DefaultCharacteristic parent) {
    return new DefaultCharacteristic()
      .setId(id)
      .setKey(kee)
      .setName(name)
      .setOrder(characteristicOrder)
      .setParent(parent)
      .setRoot(parent)
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt);
  }

  public static CharacteristicDto toDto(DefaultCharacteristic characteristic, @Nullable Integer parentId) {
    return new CharacteristicDto()
      .setKey(characteristic.key())
      .setName(characteristic.name())
      .setOrder(characteristic.order())
      .setParentId(parentId)
      .setRootId(parentId)
      .setEnabled(true)
      .setCreatedAt(characteristic.createdAt())
      .setUpdatedAt(characteristic.updatedAt());
  }

  public DefaultRequirement toRequirement(RuleKey ruleKey, DefaultCharacteristic characteristic, DefaultCharacteristic rootCharacteristic) {
    return new DefaultRequirement()
      .setId(id)
      .setRuleKey(ruleKey)
      .setCharacteristic(characteristic)
      .setRootCharacteristic(rootCharacteristic)
      .setFunction(functionKey)
      .setFactorValue(factorValue.intValue())
      .setFactorUnit(toUnit(factorUnit))
      .setOffsetValue(offsetValue.intValue())
      .setOffsetUnit(toUnit(offsetUnit))
      .setCreatedAt(createdAt)
      .setUpdatedAt(updatedAt);
  }

  public static CharacteristicDto toDto(DefaultRequirement requirement, Integer characteristicId, Integer rootCharacteristicId, Integer ruleId) {
    return new CharacteristicDto()
      .setRuleId(ruleId)
      .setParentId(characteristicId)
      .setRootId(rootCharacteristicId)
      .setFunction(requirement.function())
      .setFactorValue((double) requirement.factorValue())
      .setFactorUnit(fromUnit(requirement.factorUnit()))
      .setOffsetValue((double) requirement.offsetValue())
      .setOffsetUnit(fromUnit(requirement.offsetUnit()))
      .setEnabled(true)
      .setCreatedAt(requirement.createdAt())
      .setUpdatedAt(requirement.updatedAt());
  }

  private static WorkDuration.UNIT toUnit(@Nullable String requirementUnit) {
    if (requirementUnit != null) {
      if (DAYS.equals(requirementUnit)) {
        return WorkDuration.UNIT.DAYS;
      } else if (HOURS.equals(requirementUnit)) {
        return WorkDuration.UNIT.HOURS;
      } else if (MINUTES.equals(requirementUnit)) {
        return WorkDuration.UNIT.MINUTES;
      }
      throw new IllegalStateException("Invalid unit : " + requirementUnit);
    }
    return null;
  }

  private static String fromUnit(@Nullable WorkDuration.UNIT unit) {
    if (unit != null) {
      if (WorkDuration.UNIT.DAYS.equals(unit)) {
        return DAYS;
      } else if (WorkDuration.UNIT.HOURS.equals(unit)) {
        return HOURS;
      } else if (WorkDuration.UNIT.MINUTES.equals(unit)) {
        return MINUTES;
      }
      throw new IllegalStateException("Invalid unit : " + unit);
    }
    return null;
  }

}
