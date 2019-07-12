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
package org.sonar.api.server.rule.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.log.Loggers;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;

@Immutable
public class DefaultRepository implements RulesDefinition.Repository {
  private final String key;
  private final String language;
  private final String name;
  private final boolean isExternal;
  private final Map<String, RulesDefinition.Rule> rulesByKey;

  public DefaultRepository(DefaultNewRepository newRepository, @Nullable RulesDefinition.Repository mergeInto) {
    this.key = newRepository.key();
    this.language = newRepository.language();
    this.isExternal = newRepository.isExternal();
    Map<String, RulesDefinition.Rule> ruleBuilder = new HashMap<>();
    if (mergeInto != null) {
      if (!StringUtils.equals(newRepository.language(), mergeInto.language()) || !StringUtils.equals(newRepository.key(), mergeInto.key())) {
        throw new IllegalArgumentException(format("Bug - language and key of the repositories to be merged should be the sames: %s and %s", newRepository, mergeInto));
      }
      this.name = StringUtils.defaultIfBlank(mergeInto.name(), newRepository.name());
      for (RulesDefinition.Rule rule : mergeInto.rules()) {
        if (!newRepository.key().startsWith("common-") && ruleBuilder.containsKey(rule.key())) {
          Loggers.get(getClass()).warn("The rule '{}' of repository '{}' is declared several times", rule.key(), mergeInto.key());
        }
        ruleBuilder.put(rule.key(), rule);
      }
    } else {
      this.name = newRepository.name();
    }
    for (RulesDefinition.NewRule newRule : newRepository.newRules().values()) {
      DefaultNewRule defaultNewRule = (DefaultNewRule) newRule;
      defaultNewRule.validate();
      ruleBuilder.put(newRule.key(), new DefaultRule(this, defaultNewRule));
    }
    this.rulesByKey = unmodifiableMap(ruleBuilder);
  }

  @Override
  public String key() {
    return key;
  }

  @Override
  public String language() {
    return language;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public boolean isExternal() {
    return isExternal;
  }

  @Override
  @CheckForNull
  public RulesDefinition.Rule rule(String ruleKey) {
    return rulesByKey.get(ruleKey);
  }

  @Override
  public List<RulesDefinition.Rule> rules() {
    return unmodifiableList(new ArrayList<>(rulesByKey.values()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultRepository that = (DefaultRepository) o;
    return key.equals(that.key);
  }

  @Override
  public int hashCode() {
    return key.hashCode();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Repository{");
    sb.append("key='").append(key).append('\'');
    sb.append(", language='").append(language).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
