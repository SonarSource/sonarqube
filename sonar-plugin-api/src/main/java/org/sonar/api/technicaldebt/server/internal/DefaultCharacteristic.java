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

package org.sonar.api.technicaldebt.server.internal;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.technicaldebt.server.Characteristic;
import org.sonar.api.utils.WorkUnit;
import org.sonar.api.utils.internal.WorkDuration;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * @since 4.1
 * @deprecated since 4.3.
 */
@Deprecated
public class DefaultCharacteristic implements Characteristic {

  private Integer id;
  private String key;
  private String name;
  private Integer order;
  private Integer parentId;
  private Integer rootId;
  private RuleKey ruleKey;
  private String function;
  private Integer factorValue;
  private WorkDuration.UNIT factorUnit;
  private Integer offsetValue;
  private WorkDuration.UNIT offsetUnit;

  @Override
  public Integer id() {
    return id;
  }

  public DefaultCharacteristic setId(Integer id) {
    this.id = id;
    return this;
  }

  @Override
  @CheckForNull
  public String key() {
    return key;
  }

  public DefaultCharacteristic setKey(@Nullable String key) {
    this.key = key;
    return this;
  }

  @Override
  @CheckForNull
  public String name() {
    return name;
  }

  public DefaultCharacteristic setName(@Nullable String name) {
    this.name = name;
    return this;
  }

  @Override
  @CheckForNull
  public Integer order() {
    return order;
  }

  public DefaultCharacteristic setOrder(@Nullable Integer order) {
    this.order = order;
    return this;
  }

  @Override
  @CheckForNull
  public Integer parentId() {
    return parentId;
  }

  public DefaultCharacteristic setParentId(@Nullable Integer parentId) {
    this.parentId = parentId;
    return this;
  }

  @Override
  @CheckForNull
  public Integer rootId() {
    return rootId;
  }

  public DefaultCharacteristic setRootId(@Nullable Integer rootId) {
    this.rootId = rootId;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Override
  @Deprecated
  @CheckForNull
  public RuleKey ruleKey() {
    return ruleKey;
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultCharacteristic setRuleKey(@Nullable RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Override
  @Deprecated
  @CheckForNull
  public String function() {
    return function;
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultCharacteristic setFunction(@Nullable String function) {
    this.function = function;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Override
  @Deprecated
  @CheckForNull
  public WorkUnit factor() {
    if (factorValue != null && factorUnit != null) {
      return WorkUnit.create((double) factorValue, fromUnit(factorUnit));
    }
    return null;
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultCharacteristic setFactor(@Nullable WorkUnit factor) {
    if (factor != null) {
      this.factorValue = (int) factor.getValue();
      this.factorUnit = toUnit(factor.getUnit());
    }
    return this;
  }

  /**
   * @deprecated since 4.3
   */
  @Override
  @Deprecated
  @CheckForNull
  public Integer factorValue() {
    return factorValue;
  }

  /**
   * @deprecated since 4.3
   */
  @Deprecated
  public DefaultCharacteristic setFactorValue(@Nullable Integer factorValue) {
    this.factorValue = factorValue;
    return this;
  }

  @Override
  @CheckForNull
  public WorkDuration.UNIT factorUnit() {
    return factorUnit;
  }

  /**
   * @deprecated since 4.3
   */
  @Deprecated
  public DefaultCharacteristic setFactorUnit(@Nullable WorkDuration.UNIT factorUnit) {
    this.factorUnit = factorUnit;
    return this;
  }

  /**
   * @deprecated since 4.2
   */
  @Override
  @Deprecated
  public WorkUnit offset() {
    if (offsetValue != null && offsetUnit != null) {
      return WorkUnit.create((double) offsetValue, fromUnit(offsetUnit));
    }
    return null;
  }

  /**
   * @deprecated since 4.2
   */
  @Deprecated
  public DefaultCharacteristic setOffset(@Nullable WorkUnit offset) {
    if (offset != null) {
      this.offsetValue = (int) offset.getValue();
      this.offsetUnit = toUnit(offset.getUnit());
    }
    return this;
  }

  /**
   * @deprecated since 4.3
   */
  @Override
  @Deprecated
  @CheckForNull
  public Integer offsetValue() {
    return offsetValue;
  }

  /**
   * @deprecated since 4.3
   */
  @Deprecated
  public DefaultCharacteristic setOffsetValue(@Nullable Integer offsetValue) {
    this.offsetValue = offsetValue;
    return this;
  }

  /**
   * @deprecated since 4.3
   */
  @Override
  @Deprecated
  @CheckForNull
  public WorkDuration.UNIT offsetUnit() {
    return offsetUnit;
  }

  /**
   * @deprecated since 4.3
   */
  @Deprecated
  public DefaultCharacteristic setOffsetUnit(@Nullable WorkDuration.UNIT offsetUnit) {
    this.offsetUnit = offsetUnit;
    return this;
  }

  /**
   * @deprecated since 4.3
   */
  @Deprecated
  public static WorkDuration.UNIT toUnit(@Nullable String requirementUnit) {
    if (requirementUnit != null) {
      if (WorkUnit.DAYS.equals(requirementUnit)) {
        return WorkDuration.UNIT.DAYS;
      } else if (WorkUnit.HOURS.equals(requirementUnit)) {
        return WorkDuration.UNIT.HOURS;
      } else if (WorkUnit.MINUTES.equals(requirementUnit)) {
        return WorkDuration.UNIT.MINUTES;
      }
      throw new IllegalStateException("Invalid unit : " + requirementUnit);
    }
    return null;
  }

  private static String fromUnit(WorkDuration.UNIT unit) {
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
  public boolean isRoot() {
    return parentId == null;
  }

  /**
   * @deprecated since 4.3
   */
  @Override
  @Deprecated
  public boolean isRequirement() {
    return ruleKey != null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DefaultCharacteristic that = (DefaultCharacteristic) o;

    if ((key != null) ? !key.equals(that.key) : (that.key != null)) {
      return false;
    }
    return !((ruleKey != null) ? !ruleKey.equals(that.ruleKey) : (that.ruleKey != null));

  }

  @Override
  public int hashCode() {
    int result = (key != null) ? key.hashCode() : 0;
    result = 31 * result + ((ruleKey != null) ? ruleKey.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
  }

}
