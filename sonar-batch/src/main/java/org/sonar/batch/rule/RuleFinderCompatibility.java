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

import org.sonar.api.batch.rule.ActiveRules;
import org.sonar.api.batch.rule.internal.DefaultActiveRule;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

import java.util.Collection;

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
    DefaultActiveRule ar = (DefaultActiveRule) activeRules.find(key);
    return ar == null ? null : Rule.create(key.repository(), key.rule()).setName(ar.name());
  }

  @Override
  public Rule find(RuleQuery query) {
    throw new UnsupportedOperationException("Unable to find rule by query");
  }

  @Override
  public Collection<Rule> findAll(RuleQuery query) {
    throw new UnsupportedOperationException("Unable to find rule by query");
  }

}
