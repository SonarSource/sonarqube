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
  private WorkUnit factor;
  private WorkUnit offset;

  private Date createdAt;
  private Date updatedAt;

  public DefaultRequirement() {
    this.factor = new WorkUnit.Builder().setDays(0).build();
    this.offset = new WorkUnit.Builder().setDays(0).build();
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

  public WorkUnit factor() {
    return factor;
  }

  public DefaultRequirement setFactor(WorkUnit factor) {
    this.factor = factor;
    return this;
  }

  public WorkUnit offset() {
    return offset;
  }

  public DefaultRequirement setOffset(WorkUnit offset) {
    this.offset = offset;
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
