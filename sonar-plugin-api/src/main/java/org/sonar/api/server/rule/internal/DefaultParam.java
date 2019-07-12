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
import javax.annotation.concurrent.Immutable;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;

@Immutable
public class DefaultParam implements RulesDefinition.Param {
  private final String key;
  private final String name;
  private final String description;
  private final String defaultValue;
  private final RuleParamType type;

  DefaultParam(DefaultNewParam newParam) {
    this.key = newParam.key();
    this.name = newParam.name();
    this.description = newParam.description();
    this.defaultValue = newParam.defaultValue();
    this.type = newParam.type();
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  @Nullable
  public String description() {
    return description;
  }

  @Override
  @Nullable
  public String defaultValue() {
    return defaultValue;
  }

  @Override
  public RuleParamType type() {
    return type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RulesDefinition.Param that = (RulesDefinition.Param) o;
    return key.equals(that.key());
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

}
