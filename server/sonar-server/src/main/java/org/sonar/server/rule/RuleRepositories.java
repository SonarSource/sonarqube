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
package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.SetMultimap;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.sonar.api.ServerSide;
import org.sonar.api.server.rule.RulesDefinition;

import javax.annotation.CheckForNull;

import java.util.Collection;
import java.util.Map;
import java.util.SortedSet;

/**
 * This component keeps metadata of rule repositories.
 * <p/>
 * Rule repositories are not persisted into database, so their metadata (name)
 * is kept by this component.
 *
 * @since 4.2
 */
@ServerSide
public class RuleRepositories {

  public static class Repository implements Comparable<Repository> {
    private final String key, name, language;

    private Repository(RulesDefinition.Repository repoDef) {
      this.key = repoDef.key();
      this.name = repoDef.name();
      this.language = repoDef.language();
    }

    public String key() {
      return key;
    }

    public String name() {
      return name;
    }

    public String language() {
      return language;
    }

    /**
     * Kept for backward-compatibility in Ruby code
     */
    public String getKey() {
      return key;
    }

    /**
     * Kept for backward-compatibility in Ruby code
     */
    public String getName(boolean b) {
      return name;
    }

    /**
     * Kept for backward-compatibility in SQALE
     */
    public String getName() {
      return name;
    }

    /**
     * Kept for backward-compatibility in Ruby code
     */
    public String getLanguage() {
      return language;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Repository that = (Repository) o;
      return key.equals(that.key);
    }

    @Override
    public int hashCode() {
      return key.hashCode();
    }

    @Override
    public int compareTo(Repository o) {
      return new CompareToBuilder()
        .append(name.toLowerCase(), o.name.toLowerCase())
        .append(key, o.key)
        .toComparison();
    }

    @Override
    public String toString() {
      return "Repository{" +
        "key='" + key + '\'' +
        ", name='" + name + '\'' +
        ", language='" + language + '\'' +
        '}';
    }
  }

  private SortedSet<Repository> repositories;
  private Map<String, Repository> repositoriesByKey;
  private SetMultimap<String, Repository> repositoriesByLang;

  void register(RulesDefinition.Context context) {
    ImmutableSortedSet.Builder<Repository> listBuilder = ImmutableSortedSet.naturalOrder();
    ImmutableSetMultimap.Builder<String, Repository> langBuilder = ImmutableSetMultimap.builder();
    ImmutableMap.Builder<String, Repository> keyBuilder = ImmutableMap.builder();
    for (RulesDefinition.Repository repoDef : context.repositories()) {
      Repository repository = new Repository(repoDef);
      listBuilder.add(repository);
      langBuilder.put(repository.language(), repository);
      keyBuilder.put(repository.key(), repository);
    }
    repositories = listBuilder.build();
    repositoriesByLang = langBuilder.build();
    repositoriesByKey = keyBuilder.build();
  }

  @CheckForNull
  public Repository repository(String key) {
    return repositoriesByKey.get(key);
  }

  /**
   * Repositories for a given language, sorted by name.
   */
  public Collection<Repository> repositoriesForLang(String lang) {
    return repositoriesByLang.get(lang);
  }

  /**
   * Repositories, sorted by name.
   */
  public Collection<Repository> repositories() {
    return repositories;
  }
}
