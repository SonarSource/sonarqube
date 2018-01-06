/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.api.rules;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

public class RuleParam {

  private Integer id;
  private Rule rule;
  private String key;
  private String description;
  private String type = "STRING";
  private String defaultValue;

  /**
   * @deprecated since 2.3 use the factory method Rule.setParameter()
   */
  @Deprecated
  public RuleParam() {
  }

  /**
   * @deprecated since 2.3 use the factory method setParameter()
   */
  @Deprecated
  public RuleParam(Rule rule, String key, String description, String type) {
    this.rule = rule;
    this.key = key;
    this.description = description;
    this.type = type;
  }

  public Integer getId() {
    return id;
  }

  public Rule getRule() {
    return rule;
  }

  RuleParam setRule(Rule rule) {
    this.rule = rule;
    return this;
  }

  public String getKey() {
    return key;
  }

  public RuleParam setKey(String key) {
    this.key = key;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RuleParam setDescription(String s) {
    this.description = StringUtils.defaultString(s, "");
    return this;
  }

  public String getType() {
    return type;
  }

  public RuleParam setType(String type) {
    this.type = type;
    return this;
  }

  public String getDefaultValue() {
    return defaultValue;
  }

  public Boolean getDefaultValueAsBoolean() {
    if (defaultValue != null) {
      return Boolean.parseBoolean(defaultValue);
    }
    return null;
  }

  public Integer getDefaultValueAsInteger() {
    if (defaultValue != null) {
      return Integer.parseInt(defaultValue);
    }
    return null;
  }

  public RuleParam setDefaultValue(String s) {
    this.defaultValue = s;
    return this;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuleParam)) {
      return false;
    }
    if (this == obj) {
      return true;
    }
    RuleParam other = (RuleParam) obj;
    return other.key.equals(key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this)
      .append("id", id)
      .append("key", key)
      .append("desc", description)
      .append("type", type)
      .toString();
  }
}
