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
package org.sonar.server.issue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import java.util.Collection;
import org.sonar.api.rule.RuleKey;
import org.sonar.db.rule.RuleDefinitionDto;

public class RulesAggregation {

  private Multiset<Rule> rules;

  public RulesAggregation() {
    this.rules = HashMultiset.create();
  }

  public RulesAggregation add(RuleDefinitionDto ruleDto) {
    rules.add(new Rule(ruleDto.getKey(), ruleDto.getName()));
    return this;
  }

  public Collection<Rule> rules() {
    return rules.elementSet();
  }

  public int countRule(Rule rule) {
    return rules.count(rule);
  }

  public static class Rule {

    private RuleKey ruleKey;
    private String name;

    public Rule(RuleKey ruleKey, String name) {
      this.ruleKey = ruleKey;
      this.name = name;
    }

    public RuleKey ruleKey() {
      return ruleKey;
    }

    public String name() {
      return name;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Rule rule = (Rule) o;

      return ruleKey.equals(rule.ruleKey);
    }

    @Override
    public int hashCode() {
      return ruleKey.hashCode();
    }
  }
}
