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

package org.sonar.api.technicaldebt;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.rule.RuleKey;

import java.util.Date;
import java.util.List;

public class Requirement {

  public static final String FUNCTION_LINEAR = "linear";
  public static final String FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";
  public static final String CONSTANT_ISSUE = "constant_issue";

  public static final List<String> FUNCTIONS = ImmutableList.of(FUNCTION_LINEAR, FUNCTION_LINEAR_WITH_OFFSET, CONSTANT_ISSUE);

  private Integer id;
  private RuleKey ruleKey;
  private Characteristic characteristic;

  private String function;
  private WorkUnit factor;
  private WorkUnit offset;

  private Date createdAt;
  private Date updatedAt;

  public Requirement() {
    this.factor = WorkUnit.create(0d, WorkUnit.DEFAULT_UNIT);
    this.offset = WorkUnit.create(0d, WorkUnit.DEFAULT_UNIT);
  }

  public Integer id() {
    return id;
  }

  public Requirement setId(Integer id) {
    this.id = id;
    return this;
  }

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public Requirement setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public Characteristic characteristic() {
    return characteristic;
  }

  public Requirement setCharacteristic(Characteristic characteristic) {
    this.characteristic = characteristic;
    this.characteristic.addRequirement(this);
    return this;
  }

  public String function() {
    return function;
  }

  public Requirement setFunction(String function) {
    if (!FUNCTIONS.contains(function)) {
      throw new IllegalArgumentException("Function '"+ function +"' is unknown.");
    }
    this.function = function;
    return this;
  }

  public WorkUnit factor() {
    return factor;
  }

  public Requirement setFactor(WorkUnit factor) {
    this.factor = factor;
    return this;
  }

  public WorkUnit offset() {
    return offset;
  }

  public Requirement setOffset(WorkUnit offset) {
    this.offset = offset;
    return this;
  }

  public Date createdAt() {
    return createdAt;
  }

  public Requirement setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Date updatedAt() {
    return updatedAt;
  }

  public Requirement setUpdatedAt(Date updatedAt) {
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

    Requirement that = (Requirement) o;

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
