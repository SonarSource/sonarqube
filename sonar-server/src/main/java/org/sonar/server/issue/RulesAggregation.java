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

package org.sonar.server.issue;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.rule.RuleDto;

import java.util.Collection;

public class RulesAggregation {

  private Multiset<Rule> rules;

  public RulesAggregation() {
    this.rules = HashMultiset.create();
  }

  public RulesAggregation add(RuleDto ruleDto) {
    rules.add(new Rule().setRuleKey(ruleDto.getKey()).setName(ruleDto.getName()));
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

    public RuleKey ruleKey() {
      return ruleKey;
    }

    public Rule setRuleKey(RuleKey ruleKey) {
      this.ruleKey = ruleKey;
      return this;
    }

    public String name() {
      return name;
    }

    public Rule setName(String name) {
      this.name = name;
      return this;
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

      if (!name.equals(rule.name)) {
        return false;
      }
      if (!ruleKey.equals(rule.ruleKey)) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result = ruleKey.hashCode();
      result = 31 * result + name.hashCode();
      return result;
    }
  }
}
