/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.Immutable;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.utils.WildcardPattern;

import static java.util.Collections.emptySet;

@Immutable
public class DefaultActiveRules implements ActiveRules {
  private final Map<RuleKey, Set<String>> deprecatedRuleKeysByRuleKey = new LinkedHashMap<>();
  private final Map<String, List<ActiveRule>> activeRulesByRepository = new HashMap<>();
  private final Map<String, Map<String, ActiveRule>> activeRulesByRepositoryAndKey = new HashMap<>();
  private final Map<String, Map<String, ActiveRule>> activeRulesByRepositoryAndInternalKey = new HashMap<>();
  private final Map<String, List<ActiveRule>> activeRulesByLanguage = new HashMap<>();

  public DefaultActiveRules(Collection<NewActiveRule> newActiveRules) {
    for (NewActiveRule newAR : newActiveRules) {
      DefaultActiveRule ar = new DefaultActiveRule(newAR);
      String repo = ar.ruleKey().repository();
      activeRulesByRepository.computeIfAbsent(repo, x -> new ArrayList<>()).add(ar);
      if (ar.language() != null) {
        activeRulesByLanguage.computeIfAbsent(ar.language(), x -> new ArrayList<>()).add(ar);
      }

      activeRulesByRepositoryAndKey.computeIfAbsent(repo, r -> new HashMap<>()).put(ar.ruleKey().rule(), ar);
      String internalKey = ar.internalKey();
      if (internalKey != null) {
        activeRulesByRepositoryAndInternalKey.computeIfAbsent(repo, r -> new HashMap<>()).put(internalKey, ar);
      }

      deprecatedRuleKeysByRuleKey.put(ar.ruleKey(), ar.getDeprecatedKeys().stream().map(RuleKey::toString).collect(Collectors.toSet()));
    }
  }

  public Set<String> getDeprecatedRuleKeys(RuleKey ruleKey) {
    return deprecatedRuleKeysByRuleKey.getOrDefault(ruleKey, emptySet());
  }

  public boolean matchesDeprecatedKeys(RuleKey ruleKey, WildcardPattern rulePattern) {
    return getDeprecatedRuleKeys(ruleKey).contains(rulePattern.toString());
  }

  @Override
  public ActiveRule find(RuleKey ruleKey) {
    return activeRulesByRepositoryAndKey.getOrDefault(ruleKey.repository(), Collections.emptyMap())
      .get(ruleKey.rule());
  }

  @Override
  public Collection<ActiveRule> findAll() {
    return activeRulesByRepository.entrySet().stream().flatMap(x -> x.getValue().stream()).toList();
  }

  @Override
  public Collection<ActiveRule> findByRepository(String repository) {
    return activeRulesByRepository.getOrDefault(repository, Collections.emptyList());
  }

  @Override
  public Collection<ActiveRule> findByLanguage(String language) {
    return activeRulesByLanguage.getOrDefault(language, Collections.emptyList());
  }

  @Override
  public ActiveRule findByInternalKey(String repository, String internalKey) {
    return activeRulesByRepositoryAndInternalKey.containsKey(repository) ? activeRulesByRepositoryAndInternalKey.get(repository).get(internalKey) : null;
  }
}
