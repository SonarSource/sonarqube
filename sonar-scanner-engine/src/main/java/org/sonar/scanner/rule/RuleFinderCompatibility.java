/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.scanner.rule;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.rule.Rules;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

@Immutable
public class RuleFinderCompatibility implements RuleFinder {

  private final Rules rules;

  public RuleFinderCompatibility(Rules rules) {
    this.rules = rules;
  }

  @Override
  public Rule findById(int ruleId) {
    throw new UnsupportedOperationException("Unable to find rule by id");
  }

  @Override
  public Rule findByKey(String repositoryKey, String key) {
    return findByKey(RuleKey.of(repositoryKey, key));
  }

  @Override
  public Rule findByKey(RuleKey key) {
    return toRule(rules.find(key));
  }

  @Override
  public Rule find(RuleQuery query) {
    Collection<Rule> all = findAll(query);
    if (all.size() > 1) {
      throw new IllegalArgumentException("Non unique result for rule query: " + ReflectionToStringBuilder.toString(query, ToStringStyle.SHORT_PREFIX_STYLE));
    } else if (all.isEmpty()) {
      return null;
    } else {
      return all.iterator().next();
    }
  }

  @Override
  public Collection<Rule> findAll(RuleQuery query) {
    if (query.getConfigKey() != null) {
      if (query.getRepositoryKey() != null && query.getKey() == null) {
        return byInternalKey(query);
      }
    } else if (query.getRepositoryKey() != null) {
      if (query.getKey() != null) {
        return byKey(query);
      } else {
        return byRepository(query);
      }
    }
    throw new UnsupportedOperationException("Unable to find rule by query");
  }

  private Collection<Rule> byRepository(RuleQuery query) {
    return rules.findByRepository(query.getRepositoryKey()).stream()
      .map(RuleFinderCompatibility::toRule)
      .collect(Collectors.toList());
  }

  private Collection<Rule> byKey(RuleQuery query) {
    Rule rule = toRule(rules.find(RuleKey.of(query.getRepositoryKey(), query.getKey())));
    return rule != null ? Arrays.asList(rule) : Collections.<Rule>emptyList();
  }

  private Collection<Rule> byInternalKey(RuleQuery query) {
    return rules.findByInternalKey(query.getRepositoryKey(), query.getConfigKey()).stream()
      .map(RuleFinderCompatibility::toRule)
      .collect(Collectors.toList());
  }

  @CheckForNull
  private static Rule toRule(@Nullable org.sonar.api.batch.rule.Rule ar) {
    return ar == null ? null : Rule.create(ar.key().repository(), ar.key().rule()).setName(ar.name()).setConfigKey(ar.internalKey());
  }

}
