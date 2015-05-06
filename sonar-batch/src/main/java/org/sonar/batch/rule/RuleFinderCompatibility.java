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
package org.sonar.batch.rule;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.sonar.api.batch.rule.ActiveRule;
import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * FIXME Waiting for the list of all server rules on batch side this is implemented by redirecting on ActiveRules. This is not correct
 * since there is a difference between a rule that doesn't exists and a rule that is not activated in project quality profile.
 *
 */
public class RuleFinderCompatibility implements RuleFinder {

  private final ActiveRules activeRules;

  public RuleFinderCompatibility(ActiveRules activeRules) {
    this.activeRules = activeRules;
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
    return toRule(activeRules.find(key));
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
        Rule rule = toRule(activeRules.findByInternalKey(query.getRepositoryKey(), query.getConfigKey()));
        return rule != null ? Arrays.asList(rule) : Collections.<Rule>emptyList();
      }
    } else if (query.getRepositoryKey() != null) {
      if (query.getKey() != null) {
        Rule rule = toRule(activeRules.find(RuleKey.of(query.getRepositoryKey(), query.getKey())));
        return rule != null ? Arrays.asList(rule) : Collections.<Rule>emptyList();
      } else {
        return Collections2.transform(activeRules.findByRepository(query.getRepositoryKey()), new Function<ActiveRule, Rule>() {
          @Override
          public Rule apply(ActiveRule input) {
            return toRule(input);
          }
        });
      }
    }
    throw new UnsupportedOperationException("Unable to find rule by query");
  }

  @CheckForNull
  private Rule toRule(@Nullable ActiveRule rule) {
    DefaultActiveRule ar = (DefaultActiveRule) rule;
    return ar == null ? null : Rule.create(ar.ruleKey().repository(), ar.ruleKey().rule()).setName(ar.name()).setConfigKey(ar.internalKey()).setLanguage(ar.language());
  }

}
