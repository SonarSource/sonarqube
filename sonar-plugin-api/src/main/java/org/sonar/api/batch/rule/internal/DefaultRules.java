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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.rule.Rule;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.List;

@Immutable
class DefaultRules implements Rules {

  // TODO use disk-backed cache (persistit) instead of full in-memory cache ?
  private final ListMultimap<String, Rule> rulesByRepository;

  DefaultRules(Collection<NewRule> newRules) {
    ImmutableListMultimap.Builder<String, Rule> builder = ImmutableListMultimap.builder();
    for (NewRule newRule : newRules) {
      DefaultRule r = new DefaultRule(newRule);
      builder.put(r.key().repository(), r);
    }
    rulesByRepository = builder.build();
  }

  @Override
  public Rule find(RuleKey ruleKey) {
    List<Rule> rules = rulesByRepository.get(ruleKey.repository());
    for (Rule rule : rules) {
      if (StringUtils.equals(rule.key().rule(), ruleKey.rule())) {
        return rule;
      }
    }
    return null;
  }

  @Override
  public Collection<Rule> findAll() {
    return rulesByRepository.values();
  }

  @Override
  public Collection<Rule> findByRepository(String repository) {
    return rulesByRepository.get(repository);
  }
}
