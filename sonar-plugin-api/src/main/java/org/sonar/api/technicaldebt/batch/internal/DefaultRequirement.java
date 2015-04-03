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

package org.sonar.api.technicaldebt.batch.internal;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.picocontainer.annotations.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.utils.WorkUnit;
import org.sonar.api.utils.internal.WorkDuration;

import javax.annotation.CheckForNull;

import java.util.Date;

/**
 * @deprecated since 4.3
 */
@Deprecated
public class DefaultRequirement implements Requirement {

  public static final String FUNCTION_LINEAR = "linear";
  public static final String FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";
  public static final String CONSTANT_ISSUE = "constant_issue";

  private Integer id;
  private RuleKey ruleKey;
  private DefaultCharacteristic characteristic;
  private DefaultCharacteristic rootCharacteristic;

  private String function;
  private int factorValue;
  private WorkDuration.UNIT factorUnit;
  private int offsetValue;
  private WorkDuration.UNIT offsetUnit;

  private Date createdAt;
  private Date updatedAt;

  @Override
  public Integer id() {
    return id;
  }

  public DefaultRequirement setId(Integer id) {
    this.id = id;
    return this;
  }

  @Override
  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultRequirement setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  @Override
  public DefaultCharacteristic characteristic() {
    return characteristic;
  }

  public DefaultRequirement setCharacteristic(DefaultCharacteristic characteristic) {
    this.characteristic = characteristic;
    this.characteristic.addRequirement(this);
    return this;
  }

  @Override
  public DefaultCharacteristic rootCharacteristic() {
    return rootCharacteristic;
  }

  public DefaultRequirement setRootCharacteristic(DefaultCharacteristic rootCharacteristic) {
    this.rootCharacteristic = rootCharacteristic;
    return this;
  }

  @Override
  public String function() {
    return function;
  }

  public DefaultRequirement setFunction(String function) {
    this.function = function;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Override
  @Deprecated
  public WorkUnit factor() {
    return WorkUnit.create((double) factorValue, fromUnit(factorUnit));
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultRequirement setFactor(WorkUnit factor) {
    this.factorValue = (int) factor.getValue();
    this.factorUnit = toUnit(factor.getUnit());
    return this;
  }

  @Override
  public int factorValue() {
    return factorValue;
  }

  public DefaultRequirement setFactorValue(int factorValue) {
    this.factorValue = factorValue;
    return this;
  }

  @Override
  @CheckForNull
  public WorkDuration.UNIT factorUnit() {
    return factorUnit;
  }

  public DefaultRequirement setFactorUnit(@Nullable WorkDuration.UNIT factorUnit) {
    this.factorUnit = factorUnit;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Override
  @Deprecated
  public WorkUnit offset() {
    return WorkUnit.create((double) offsetValue, fromUnit(offsetUnit));
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultRequirement setOffset(WorkUnit offset) {
    this.offsetValue = (int) offset.getValue();
    this.offsetUnit = toUnit(offset.getUnit());
    return this;
  }

  @Override
  public int offsetValue() {
    return offsetValue;
  }

  public DefaultRequirement setOffsetValue(int offsetValue) {
    this.offsetValue = offsetValue;
    return this;
  }

  @Override
  @CheckForNull
  public WorkDuration.UNIT offsetUnit() {
    return offsetUnit;
  }

  public DefaultRequirement setOffsetUnit(@Nullable WorkDuration.UNIT offsetUnit) {
    this.offsetUnit = offsetUnit;
    return this;
  }

  @Override
  public Date createdAt() {
    return createdAt;
  }

  public DefaultRequirement setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  @Override
  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultRequirement setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public static WorkDuration.UNIT toUnit(String requirementUnit){
    if (WorkUnit.DAYS.equals(requirementUnit)) {
      return WorkDuration.UNIT.DAYS;
    } else if (WorkUnit.HOURS.equals(requirementUnit)) {
      return WorkDuration.UNIT.HOURS;
    } else if (WorkUnit.MINUTES.equals(requirementUnit)) {
      return WorkDuration.UNIT.MINUTES;
    }
    throw new IllegalStateException("Invalid unit : " + requirementUnit);
  }

  private static String fromUnit(WorkDuration.UNIT unit){
    if (WorkDuration.UNIT.DAYS.equals(unit)) {
      return WorkUnit.DAYS;
    } else if (WorkDuration.UNIT.HOURS.equals(unit)) {
      return WorkUnit.HOURS;
    } else if (WorkDuration.UNIT.MINUTES.equals(unit)) {
      return WorkUnit.MINUTES;
    }
    throw new IllegalStateException("Invalid unit : " + unit);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultRequirement that = (DefaultRequirement) o;

    if (!characteristic.equals(that.characteristic)) {
      return false;
    }
    return ruleKey.equals(that.ruleKey);

  }

  @Override
  public int hashCode() {
    int result = ruleKey.hashCode();
    result = 31 * result + characteristic.hashCode();
    return result;
  }
}
