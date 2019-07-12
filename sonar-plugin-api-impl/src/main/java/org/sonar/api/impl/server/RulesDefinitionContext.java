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
package org.sonar.api.impl.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.internal.DefaultNewRepository;
import org.sonar.api.server.rule.internal.DefaultRepository;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static org.sonar.api.utils.Preconditions.checkState;

public class RulesDefinitionContext extends RulesDefinition.Context {
  private final Map<String, RulesDefinition.Repository> repositoriesByKey = new HashMap<>();
  private String currentPluginKey;

  @Override
  public RulesDefinition.NewRepository createRepository(String key, String language) {
    return new DefaultNewRepository(this, key, language, false);
  }

  @Override
  public RulesDefinition.NewRepository createExternalRepository(String engineId, String language) {
    return new DefaultNewRepository(this, RuleKey.EXTERNAL_RULE_REPO_PREFIX + engineId, language, true);
  }

  @Override
  @Deprecated
  public RulesDefinition.NewRepository extendRepository(String key, String language) {
    return createRepository(key, language);
  }

  @Override
  @CheckForNull
  public RulesDefinition.Repository repository(String key) {
    return repositoriesByKey.get(key);
  }

  @Override
  public List<RulesDefinition.Repository> repositories() {
    return unmodifiableList(new ArrayList<>(repositoriesByKey.values()));
  }

  @Override
  @Deprecated
  public List<RulesDefinition.ExtendedRepository> extendedRepositories(String repositoryKey) {
    return emptyList();
  }

  @Override
  @Deprecated
  public List<RulesDefinition.ExtendedRepository> extendedRepositories() {
    return emptyList();
  }

  public void registerRepository(DefaultNewRepository newRepository) {
    RulesDefinition.Repository existing = repositoriesByKey.get(newRepository.key());
    if (existing != null) {
      String existingLanguage = existing.language();
      checkState(existingLanguage.equals(newRepository.language()),
        "The rule repository '%s' must not be defined for two different languages: %s and %s",
        newRepository.key(), existingLanguage, newRepository.language());
    }
    repositoriesByKey.put(newRepository.key(), new DefaultRepository(newRepository, existing));
  }

  public String currentPluginKey() {
    return currentPluginKey;
  }

  @Override
  public void setCurrentPluginKey(@Nullable String pluginKey) {
    this.currentPluginKey = pluginKey;
  }
}
