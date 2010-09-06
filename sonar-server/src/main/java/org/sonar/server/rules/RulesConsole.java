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
import org.apache.commons.lang.StringUtils;
import org.sonar.api.ServerComponent;
import org.sonar.api.rules.RuleRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class RulesConsole implements ServerComponent {

  private List<RuleRepository> repositories = new ArrayList<RuleRepository>();
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
    }
  }

  public List<RuleRepository> getRepositoriesByLanguage(String language) {
    return repositoriesByLanguage.get(language);
  }

  public RuleRepository getRepository(String key) {
    for (RuleRepository repository : repositories) {
      if (StringUtils.equals(key, repository.getKey())) {
        return repository;
      }
    }
    return null;
  }
}
