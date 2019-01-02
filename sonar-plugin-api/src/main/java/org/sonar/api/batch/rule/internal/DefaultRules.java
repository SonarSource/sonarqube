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
package org.sonar.api.batch.rule.internal;

import com.google.common.collect.ImmutableTable;

import com.google.common.collect.HashBasedTable;
import org.sonar.api.batch.rule.Rule;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableListMultimap;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Immutable
class DefaultRules implements Rules {
  private final ImmutableListMultimap<String, Rule> rulesByRepository;
  private final ImmutableTable<String, String, List<Rule>> rulesByRepositoryAndInternalKey;
  private final Map<RuleKey, Rule> rulesByRuleKey;

  DefaultRules(Collection<NewRule> newRules) {
    Map<RuleKey, Rule> rulesByRuleKeyBuilder = new HashMap<>();
    ImmutableListMultimap.Builder<String, Rule> builder = ImmutableListMultimap.builder();
    Table<String, String, List<Rule>> tableBuilder = HashBasedTable.create();

    for (NewRule newRule : newRules) {
      DefaultRule r = new DefaultRule(newRule);
      rulesByRuleKeyBuilder.put(r.key(), r);
      builder.put(r.key().repository(), r);
      addToTable(tableBuilder, r);
    }

    rulesByRuleKey = Collections.unmodifiableMap(rulesByRuleKeyBuilder);
    rulesByRepository = builder.build();
    rulesByRepositoryAndInternalKey = ImmutableTable.copyOf(tableBuilder);
  }

  private static void addToTable(Table<String, String, List<Rule>> table, DefaultRule r) {
    if (r.internalKey() == null) {
      return;
    }

    List<Rule> ruleList = table.get(r.key().repository(), r.internalKey());

    if (ruleList == null) {
      ruleList = new LinkedList<>();
    }

    ruleList.add(r);
    table.put(r.key().repository(), r.internalKey(), ruleList);
  }

  @Override
  public Rule find(RuleKey ruleKey) {
    return rulesByRuleKey.get(ruleKey);
  }

  @Override
  public Collection<Rule> findAll() {
    return rulesByRepository.values();
  }

  @Override
  public Collection<Rule> findByRepository(String repository) {
    return rulesByRepository.get(repository);
  }

  @Override
  public Collection<Rule> findByInternalKey(String repository, String internalKey) {
    List<Rule> rules = rulesByRepositoryAndInternalKey.get(repository, internalKey);

    return rules != null ? rules : Collections.<Rule>emptyList();
  }
}
