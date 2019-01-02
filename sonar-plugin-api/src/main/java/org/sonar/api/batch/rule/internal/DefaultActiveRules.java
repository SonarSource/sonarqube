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

import com.google.common.collect.ImmutableListMultimap;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;

import javax.annotation.concurrent.Immutable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Immutable
public class DefaultActiveRules implements ActiveRules {
  private final ImmutableListMultimap<String, ActiveRule> activeRulesByRepository;
  private final Map<String, Map<String, ActiveRule>> activeRulesByRepositoryAndKey = new HashMap<>();
  private final Map<String, Map<String, ActiveRule>> activeRulesByRepositoryAndInternalKey = new HashMap<>();
  private final ImmutableListMultimap<String, ActiveRule> activeRulesByLanguage;

  public DefaultActiveRules(Collection<NewActiveRule> newActiveRules) {
    ImmutableListMultimap.Builder<String, ActiveRule> repoBuilder = ImmutableListMultimap.builder();
    ImmutableListMultimap.Builder<String, ActiveRule> langBuilder = ImmutableListMultimap.builder();
    for (NewActiveRule newAR : newActiveRules) {
      DefaultActiveRule ar = new DefaultActiveRule(newAR);
      String repo = ar.ruleKey().repository();
      repoBuilder.put(repo, ar);
      if (ar.language() != null) {
        langBuilder.put(ar.language(), ar);
      }

      activeRulesByRepositoryAndKey.computeIfAbsent(repo, r -> new HashMap<>()).put(ar.ruleKey().rule(), ar);
      String internalKey = ar.internalKey();
      if (internalKey != null) {
        activeRulesByRepositoryAndInternalKey.computeIfAbsent(repo, r -> new HashMap<>()).put(internalKey, ar);
      }
    }
    activeRulesByRepository = repoBuilder.build();
    activeRulesByLanguage = langBuilder.build();
  }

  @Override
  public ActiveRule find(RuleKey ruleKey) {
    return activeRulesByRepositoryAndKey.getOrDefault(ruleKey.repository(), Collections.emptyMap())
      .get(ruleKey.rule());
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
