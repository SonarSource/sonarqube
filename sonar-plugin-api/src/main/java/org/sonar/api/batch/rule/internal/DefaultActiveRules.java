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
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Immutable
public class DefaultActiveRules implements ActiveRules {

  // TODO use disk-backed cache (persistit) instead of full in-memory cache ?
  private final ListMultimap<String, ActiveRule> activeRulesByRepository;
  private final Map<String, Map<String, ActiveRule>> activeRulesByRepositoryAndKey = new HashMap<String, Map<String, ActiveRule>>();
  private final Map<String, Map<String, ActiveRule>> activeRulesByRepositoryAndInternalKey = new HashMap<String, Map<String, ActiveRule>>();
  private final ListMultimap<String, ActiveRule> activeRulesByLanguage;

  public DefaultActiveRules(Collection<NewActiveRule> newActiveRules) {
    ImmutableListMultimap.Builder<String, ActiveRule> repoBuilder = ImmutableListMultimap.builder();
    ImmutableListMultimap.Builder<String, ActiveRule> langBuilder = ImmutableListMultimap.builder();
    for (NewActiveRule newAR : newActiveRules) {
      DefaultActiveRule ar = new DefaultActiveRule(newAR);
      repoBuilder.put(ar.ruleKey().repository(), ar);
      if (ar.language() != null) {
        langBuilder.put(ar.language(), ar);
      }
      if (!activeRulesByRepositoryAndKey.containsKey(ar.ruleKey().repository())) {
        activeRulesByRepositoryAndKey.put(ar.ruleKey().repository(), new HashMap<String, ActiveRule>());
        activeRulesByRepositoryAndInternalKey.put(ar.ruleKey().repository(), new HashMap<String, ActiveRule>());
      }
      activeRulesByRepositoryAndKey.get(ar.ruleKey().repository()).put(ar.ruleKey().rule(), ar);
      String internalKey = ar.internalKey();
      if (internalKey != null) {
        activeRulesByRepositoryAndInternalKey.get(ar.ruleKey().repository()).put(internalKey, ar);
      }
    }
    activeRulesByRepository = repoBuilder.build();
    activeRulesByLanguage = langBuilder.build();
  }

  @Override
  public ActiveRule find(RuleKey ruleKey) {
    return activeRulesByRepositoryAndKey.containsKey(ruleKey.repository()) ? activeRulesByRepositoryAndKey.get(ruleKey.repository()).get(ruleKey.rule()) : null;
  }

  @Override
  public Collection<ActiveRule> findAll() {
    return activeRulesByRepository.values();
  }

  @Override
  public Collection<ActiveRule> findByRepository(String repository) {
    return activeRulesByRepository.get(repository);
  }

  @Override
  public Collection<ActiveRule> findByLanguage(String language) {
    return activeRulesByLanguage.get(language);
  }

  @Override
  public ActiveRule findByInternalKey(String repository, String internalKey) {
    return activeRulesByRepositoryAndInternalKey.containsKey(repository) ? activeRulesByRepositoryAndInternalKey.get(repository).get(internalKey) : null;
  }
}
