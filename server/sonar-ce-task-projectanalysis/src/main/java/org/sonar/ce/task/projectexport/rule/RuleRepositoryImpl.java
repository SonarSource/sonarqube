/*
 * SonarQube
 * Copyright (C) 2009-2025 SonarSource Sàrl
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
package org.sonar.ce.task.projectexport.rule;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sonar.api.rule.RuleKey;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class RuleRepositoryImpl implements RuleRepository {
  private final Map<String, Rule> rulesByUuid = new HashMap<>();

  @Override
  public Rule register(String ref, RuleKey ruleKey) {
    requireNonNull(ruleKey, "ruleKey can not be null");

    Rule rule = rulesByUuid.get(ref);
    if (rule != null) {
      if (!ruleKey.repository().equals(rule.repository()) || !ruleKey.rule().equals(rule.key())) {
        throw new IllegalArgumentException(format(
          "Specified RuleKey '%s' is not equal to the one already registered in repository for ref %s: '%s'",
          ruleKey, ref, RuleKey.of(rule.repository(), rule.key())));
      }
      return rule;
    }

    rule = new Rule(ref, ruleKey.repository(), ruleKey.rule());
    rulesByUuid.put(ref, rule);
    return rule;
  }

  @Override
  public Collection<Rule> getAll() {
    return List.copyOf(rulesByUuid.values());
  }

}
