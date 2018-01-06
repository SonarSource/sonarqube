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

import com.google.common.collect.ImmutableTable;

import com.google.common.collect.HashBasedTable;
import org.sonar.api.batch.rule.Rule;
import com.google.common.collect.Table;
import com.google.common.collect.ImmutableListMultimap;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Immutable
class DefaultRules implements Rules {
  private final ImmutableListMultimap<String, Rule> rulesByRepository;
  private final ImmutableTable<String, String, List<Rule>> rulesByRepositoryAndInternalKey;

  DefaultRules(Collection<NewRule> newRules) {
    ImmutableListMultimap.Builder<String, Rule> builder = ImmutableListMultimap.builder();
    Table<String, String, List<Rule>> tableBuilder = HashBasedTable.create();

    for (NewRule newRule : newRules) {
      DefaultRule r = new DefaultRule(newRule);
      builder.put(r.key().repository(), r);
      addToTable(tableBuilder, r);
    }

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

  @Override
  public Collection<Rule> findByInternalKey(String repository, String internalKey) {
    List<Rule> rules = rulesByRepositoryAndInternalKey.get(repository, internalKey);

    return rules != null ? rules : Collections.<Rule>emptyList();
  }
}
