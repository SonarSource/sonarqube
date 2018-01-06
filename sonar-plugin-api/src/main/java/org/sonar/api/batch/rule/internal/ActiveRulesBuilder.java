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
package org.sonar.api.batch.rule.internal;

import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds instances of {@link org.sonar.api.batch.rule.ActiveRules}.
 * <b>For unit testing and internal use only</b>.
 *
 * @since 4.2
 */
public class ActiveRulesBuilder {

  private final Map<RuleKey, NewActiveRule> map = new LinkedHashMap<>();

  public NewActiveRule create(RuleKey ruleKey) {
    return new NewActiveRule(this, ruleKey);
  }

  void activate(NewActiveRule newActiveRule) {
    if (map.containsKey(newActiveRule.ruleKey)) {
      throw new IllegalStateException(String.format("Rule '%s' is already activated", newActiveRule.ruleKey));
    }
    map.put(newActiveRule.ruleKey, newActiveRule);
  }

  public ActiveRules build() {
    return new DefaultActiveRules(map.values());
  }
}
