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
package org.sonar.core.persistence.migration.v45;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

/**
 * SONAR-5575
 * <p/>
 * Used in the Active Record Migration 601.
 *
 * @since 4.5
 */
public class RuleParameter {

  private Integer id;
  private Integer ruleId;
  private Integer ruleTemplateId;
  private String name;
  private String type;
  private String defaultValue;
  private String description;

  public Integer getId() {
    return id;
  }

  public RuleParameter setId(Integer id) {
    this.id = id;
    return this;
  }

  public Integer getRuleId() {
    return ruleId;
  }

  public RuleParameter setRuleId(Integer ruleId) {
    this.ruleId = ruleId;
    return this;
  }

  @CheckForNull
  public Integer getRuleTemplateId() {
    return ruleTemplateId;
  }

  public RuleParameter setRuleTemplateId(@Nullable Integer ruleTemplateId) {
    this.ruleTemplateId = ruleTemplateId;
    return this;
  }

  public String getName() {
    return name;
  }

  public RuleParameter setName(String name) {
    this.name = name;
    return this;
  }

  public String getType() {
    return type;
  }

  public RuleParameter setType(String type) {
    this.type = type;
    return this;
  }

  @CheckForNull
  public String getDefaultValue() {
    return defaultValue;
  }

  public RuleParameter setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  public String getDescription() {
    return description;
  }

  public RuleParameter setDescription(String description) {
    this.description = description;
    return this;
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE).toString();
  }
}
