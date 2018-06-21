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
package org.sonar.server.computation.task.projectanalysis.qualityprofile;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.sonar.api.rule.RuleKey;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Objects.requireNonNull;

public class ActiveRulesHolderImpl implements ActiveRulesHolder {

  private Map<RuleKey, ActiveRule> activeRulesByKey = null;

  @Override
  public Optional<ActiveRule> get(RuleKey ruleKey) {
    checkState(activeRulesByKey != null, "Active rules have not been initialized yet");
    return Optional.fromNullable(activeRulesByKey.get(ruleKey));
  }

  public Collection<ActiveRule> getAll() {
    checkState(activeRulesByKey != null, "Active rules have not been initialized yet");
    return activeRulesByKey.values();
  }

  public void set(Collection<ActiveRule> activeRules) {
    requireNonNull(activeRules, "Active rules cannot be null");
    checkState(activeRulesByKey == null, "Active rules have already been initialized");

    Map<RuleKey, ActiveRule> temp = new HashMap<>();
    for (ActiveRule activeRule : activeRules) {
      ActiveRule previousValue = temp.put(activeRule.getRuleKey(), activeRule);
      if (previousValue != null) {
        throw new IllegalArgumentException("Active rule must not be declared multiple times: " + activeRule.getRuleKey());
      }
    }
    activeRulesByKey = ImmutableMap.copyOf(temp);
  }
}
