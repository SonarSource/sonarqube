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
import org.sonar.api.batch.rule.ModuleRule;
import org.sonar.api.batch.rule.ModuleRules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;
import java.util.Collection;
import java.util.List;

@Immutable
class DefaultModuleRules implements ModuleRules {

  // TODO use disk-backed cache (persistit) instead of full in-memory cache ?
  private final ListMultimap<String, ModuleRule> moduleRulesByRepository;

  public DefaultModuleRules(Collection<NewModuleRule> newModuleRules) {
    ImmutableListMultimap.Builder<String, ModuleRule> builder = ImmutableListMultimap.builder();
    for (NewModuleRule newMr : newModuleRules) {
      DefaultModuleRule mr = new DefaultModuleRule(newMr);
      builder.put(mr.ruleKey().repository(), mr);
    }
    moduleRulesByRepository = builder.build();
  }


  @Override
  public ModuleRule find(RuleKey ruleKey) {
    List<ModuleRule> rules = moduleRulesByRepository.get(ruleKey.repository());
    for (ModuleRule rule : rules) {
      if (StringUtils.equals(rule.ruleKey().rule(), ruleKey.rule())) {
        return rule;
      }
    }
    return null;
  }

  @Override
  public Collection<ModuleRule> findAll() {
    return moduleRulesByRepository.values();
  }

  @Override
  public Collection<ModuleRule> findByRepository(String repository) {
    return moduleRulesByRepository.get(repository);
  }
}
