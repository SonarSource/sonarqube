/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
package org.sonar.db.rule;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import static com.google.common.base.Preconditions.checkArgument;

public class RuleParamDto {

  private Integer id;
  private Integer ruleId;
  private String name;
  private String type;
  private String defaultValue;
  private String description;

  public Integer getId() {
    return id;
  }

  public RuleParamDto setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public RuleParamDto setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleParamDto setName(String s) {
    checkArgument(s.length() <= 128, "Rule parameter name is too long: %s", s);
    this.name = s;
    return this;
  }

  public String getType() {
    return type;
  }

  public RuleParamDto setType(String type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public String getDefaultValue() {
    return defaultValue;
  }

  public RuleParamDto setDefaultValue(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Rule parameter default value is too long: %s", s);
    this.defaultValue = s;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RuleParamDto setDescription(@Nullable String s) {
    checkArgument(s == null || s.length() <= 4000, "Rule parameter description is too long: %s", s);
    this.description = s;
    return this;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }

  public static RuleParamDto createFor(RuleDefinitionDto rule) {
    // Should eventually switch to RuleKey (RuleKey is available before insert)
    return new RuleParamDto().setRuleId(rule.getId());
  }

}
