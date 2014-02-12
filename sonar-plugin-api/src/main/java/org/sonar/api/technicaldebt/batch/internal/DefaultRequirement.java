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

package org.sonar.api.technicaldebt.batch.internal;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.batch.Requirement;
import org.sonar.api.utils.WorkDuration;
import org.sonar.api.utils.WorkUnit;

import java.util.Date;

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
  private WorkUnit factor;
  private WorkUnit offset;

  private Date createdAt;
  private Date updatedAt;

  public DefaultRequirement() {
    this.factor = WorkUnit.create(0d, WorkUnit.DAYS);
    this.offset = WorkUnit.create(0d, WorkUnit.DAYS);
  }

  public Integer id() {
    return id;
  }

  public DefaultRequirement setId(Integer id) {
    this.id = id;
    return this;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultRequirement setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public DefaultCharacteristic characteristic() {
    return characteristic;
  }

  public DefaultRequirement setCharacteristic(DefaultCharacteristic characteristic) {
    this.characteristic = characteristic;
    this.characteristic.addRequirement(this);
    return this;
  }

  public DefaultCharacteristic rootCharacteristic() {
    return rootCharacteristic;
  }

  public DefaultRequirement setRootCharacteristic(DefaultCharacteristic rootCharacteristic) {
    this.rootCharacteristic = rootCharacteristic;
    return this;
  }

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
  @Deprecated
  public WorkUnit factor() {
    return factor;
//    return WorkUnit.create((double) factorValue, fromUnit(factorUnit));
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultRequirement setFactor(WorkUnit factor) {
    this.factor = factor;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public WorkUnit offset() {
    return offset;
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultRequirement setOffset(WorkUnit offset) {
    this.offset = offset;
    return this;
  }

  public int factorValue() {
    return factorValue;
  }

  public DefaultRequirement setFactorValue(int factorValue) {
    this.factorValue = factorValue;
    return this;
  }

  public WorkDuration.UNIT factorUnit() {
    return factorUnit;
  }

  public DefaultRequirement setFactorUnit(WorkDuration.UNIT factorUnit) {
    this.factorUnit = factorUnit;
    return this;
  }

  public int offsetValue() {
    return offsetValue;
  }

  public DefaultRequirement setOffsetValue(int offsetValue) {
    this.offsetValue = offsetValue;
    return this;
  }

  public WorkDuration.UNIT offsetUnit() {
    return offsetUnit;
  }

  public DefaultRequirement setOffsetUnit(WorkDuration.UNIT offsetUnit) {
    this.offsetUnit = offsetUnit;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public DefaultRequirement setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public DefaultRequirement setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  private static WorkDuration.UNIT toUnit(String requirementUnit){
    if (requirementUnit.equals(WorkUnit.DAYS)) {
      return WorkDuration.UNIT.DAYS;
    } else if (requirementUnit.equals(WorkUnit.HOURS)) {
      return WorkDuration.UNIT.HOURS;
    } else if (requirementUnit.equals(WorkUnit.MINUTES)) {
      return WorkDuration.UNIT.MINUTES;
    }
    throw new IllegalStateException("Invalid unit : " + requirementUnit);
  }

  private static String fromUnit(WorkDuration.UNIT unit){
    if (unit.equals(WorkDuration.UNIT.DAYS)) {
      return WorkUnit.DAYS;
    } else if (unit.equals(WorkDuration.UNIT.HOURS)) {
      return WorkUnit.HOURS;
    } else if (unit.equals(WorkDuration.UNIT.MINUTES)) {
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
    if (!ruleKey.equals(that.ruleKey)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = ruleKey.hashCode();
    result = 31 * result + characteristic.hashCode();
    return result;
  }
}
