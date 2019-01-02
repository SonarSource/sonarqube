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
package org.sonar.api.rules;

import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.ExtensionPoint;
import org.sonar.api.ce.ComputeEngineSide;
import org.sonar.api.server.ServerSide;

/**
 * @since 2.3
 * @deprecated in 4.2. Replaced by org.sonar.api.server.rule.RulesDefinition
 */
@Deprecated
@ServerSide
@ComputeEngineSide
@ExtensionPoint
public abstract class RuleRepository {

  private String key;
  private String language;
  private String name;

  protected RuleRepository(String key, String language) {
    this.key = key;
    this.language = language;
  }

  public final String getKey() {
    return key;
  }

  public final String getLanguage() {
    return language;
  }

  public final String getName() {
    return name;
  }

  public final String getName(boolean useKeyIfEmpty) {
    if (useKeyIfEmpty) {
      return StringUtils.defaultIfEmpty(name, key);
    }
    return name;
  }

  public final RuleRepository setName(String s) {
    this.name = s;
    return this;
  }

  public abstract List<Rule> createRules();

  @Override
  public String toString() {
    return new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE)
      .append("key", key)
      .append("language", language)
      .append("name", name)
      .toString();
  }
}
