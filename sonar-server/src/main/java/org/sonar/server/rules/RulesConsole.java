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
package org.sonar.server.rules;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import org.sonar.api.ServerComponent;
import org.sonar.api.rules.RuleRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RulesConsole implements ServerComponent {

  private List<RuleRepository> repositories = Lists.newArrayList();
  private Map<String, RuleRepository> repositoryByKey = Maps.newHashMap();
  private SetMultimap<String, RuleRepository> repositoriesByLanguage = HashMultimap.create();


  public RulesConsole(RuleRepository[] repositories) {
    initRepositories(repositories);
  }

  private void initRepositories(RuleRepository[] repositories) {
    this.repositories.addAll(Arrays.asList(repositories));
    for (RuleRepository repository : this.repositories) {
      if (!repositoryByKey.containsKey(repository.getKey())) {
        repositoriesByLanguage.put(repository.getLanguage(), repository);
        repositoryByKey.put(repository.getKey(), repository);
      }
    }
  }

  public Set<RuleRepository> getRepositoriesByLanguage(String language) {
    return repositoriesByLanguage.get(language);
  }

  public List<RuleRepository> getRepositories() {
    return repositories;
  }

  public RuleRepository getRepository(String key) {
    return repositoryByKey.get(key);
  }
}
