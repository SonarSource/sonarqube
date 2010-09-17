/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.server.rules;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.sonar.api.ServerComponent;
import org.sonar.api.rules.RuleRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class RulesConsole implements ServerComponent {

  private List<RuleRepository> repositories = Lists.newArrayList();
  private Map<String, RuleRepository> repositoryByKey = Maps.newHashMap();
  private ListMultimap<String, RuleRepository> repositoriesByLanguage = ArrayListMultimap.create();


  public RulesConsole(RuleRepository[] repositories, DeprecatedRuleRepositories deprecatedRuleRepositories) {
    initRepositories(repositories, deprecatedRuleRepositories);
  }

  private void initRepositories(RuleRepository[] repositories, DeprecatedRuleRepositories deprecatedBridge) {
    this.repositories.addAll(Arrays.asList(repositories));
    if (deprecatedBridge != null) {
      this.repositories.addAll(deprecatedBridge.create());
    }
    for (RuleRepository repository : this.repositories) {
      repositoriesByLanguage.put(repository.getLanguage(), repository);
      repositoryByKey.put(repository.getKey(), repository);
    }
  }

  public List<RuleRepository> getRepositoriesByLanguage(String language) {
    return repositoriesByLanguage.get(language);
  }

  public List<RuleRepository> getRepositories() {
    return repositories;
  }

  public RuleRepository getRepository(String key) {
    return repositoryByKey.get(key);
  }
}
