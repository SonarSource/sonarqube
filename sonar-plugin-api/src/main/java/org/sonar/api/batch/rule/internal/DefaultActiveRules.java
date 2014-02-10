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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;

@Immutable
class DefaultActiveRules implements ActiveRules {

  // TODO use disk-backed cache (persistit) instead of full in-memory cache ?
  private final ListMultimap<String, ActiveRule> activeRulesByRepository;

  public DefaultActiveRules(Collection<NewActiveRule> newActiveRules) {
    ImmutableListMultimap.Builder<String, ActiveRule> builder = ImmutableListMultimap.builder();
    for (NewActiveRule newAR : newActiveRules) {
      DefaultActiveRule ar = new DefaultActiveRule(newAR);
      builder.put(ar.ruleKey().repository(), ar);
    }
    activeRulesByRepository = builder.build();
  }

  @Override
  public ActiveRule find(RuleKey ruleKey) {
    List<ActiveRule> rules = activeRulesByRepository.get(ruleKey.repository());
    for (ActiveRule rule : rules) {
      if (StringUtils.equals(rule.ruleKey().rule(), ruleKey.rule())) {
        return rule;
      }
    }
    return null;
  }

  @Override
  public Collection<ActiveRule> findAll() {
    return activeRulesByRepository.values();
  }

  @Override
  public Collection<ActiveRule> findByRepository(String repository) {
    return activeRulesByRepository.get(repository);
  }
}
