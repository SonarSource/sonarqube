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
package org.sonar.core.technicaldebt;

import org.sonar.api.rule.RuleKey;

import javax.annotation.CheckForNull;

public class DefaultRequirement {

  public static final String FUNCTION_CONSTANT = "constant_resource";
  public static final String FUNCTION_LINEAR = "linear";
  public static final String FUNCTION_LINEAR_WITH_OFFSET = "linear_offset";
  public static final String FUNCTION_LINEAR_WITH_THRESHOLD = "linear_threshold";

  private RuleKey ruleKey;
  private DefaultCharacteristic rootCharacteristic;
  private DefaultCharacteristic characteristic;

  private String function;
  private WorkUnit factor;
  private WorkUnit offset;

  public RuleKey ruleKey() {
    return ruleKey;
  }

  public DefaultRequirement setRuleKey(RuleKey ruleKey) {
    this.ruleKey = ruleKey;
    return this;
  }

  public DefaultCharacteristic rootCharacteristic() {
    return rootCharacteristic;
  }

  public DefaultRequirement setRootCharacteristic(DefaultCharacteristic rootCharacteristic) {
    this.rootCharacteristic = rootCharacteristic;
    return this;
  }

  public DefaultCharacteristic characteristic() {
    return characteristic;
  }

  public DefaultRequirement setCharacteristic(DefaultCharacteristic characteristic) {
    this.characteristic = characteristic;
    return this;
  }

  public DefaultRequirement addProperty(DefaultRequirementProperty property) {
    if (property.key().equals(DefaultRequirementProperty.PROPERTY_REMEDIATION_FUNCTION)) {
      // TODO check function is valid
      this.function = property.textValue();
    } else if (property.key().equals(DefaultRequirementProperty.PROPERTY_REMEDIATION_FACTOR)) {
      this.factor = WorkUnit.create(property.value(), property.textValue());
    } else if (property.key().equals(DefaultRequirementProperty.PROPERTY_OFFSET)) {
      this.offset = WorkUnit.create(property.value(), property.textValue());
    } else {
      // TODO fail
    }
    return this;
  }

  public String function() {
    return function;
  }

  public WorkUnit factor() {
    return factor;
  }

  @CheckForNull
  public WorkUnit offset() {
    return offset;
  }

}
