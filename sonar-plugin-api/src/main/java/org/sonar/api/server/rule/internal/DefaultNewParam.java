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
package org.sonar.api.server.rule.internal;

import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

import static org.apache.commons.lang.StringUtils.defaultIfEmpty;

public class DefaultNewParam extends RulesDefinition.NewParam {
  private final String key;
  private String name;
  private String description;
  private String defaultValue;
  private RuleParamType type = RuleParamType.STRING;

  DefaultNewParam(String key) {
    this.key = this.name = key;
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public DefaultNewParam setName(@Nullable String s) {
    // name must never be null.
    this.name = StringUtils.defaultIfBlank(s, key);
    return this;
  }

  @Override
  public DefaultNewParam setType(RuleParamType t) {
    this.type = t;
    return this;
  }

  @Override
  public DefaultNewParam setDescription(@Nullable String s) {
    this.description = StringUtils.defaultIfBlank(s, null);
    return this;
  }

  @Override
  public DefaultNewParam setDefaultValue(@Nullable String s) {
    this.defaultValue = defaultIfEmpty(s, null);
    return this;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public String defaultValue() {
    return defaultValue;
  }

  public RuleParamType type() {
    return type;
  }
}
