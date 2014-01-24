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
package org.sonar.server.qualityprofile;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.server.rule.RuleParam;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class QProfileRuleParam {

  private RuleParam param;
  private final String value;

  public QProfileRuleParam(RuleParam param, @Nullable String value) {
    this.param = param;
    this.value = value;
  }

  @CheckForNull
  public String value() {
    return value;
  }

  public String key() {
    return param.key();
  }

  public String description() {
    return param.description();
  }

  @CheckForNull
  public String defaultValue() {
    return param.defaultValue();
  }

  public RuleParamType type() {
    return param.type();
  }

  @Override
  public String toString() {
    return new ReflectionToStringBuilder(this).toString();
  }
}
