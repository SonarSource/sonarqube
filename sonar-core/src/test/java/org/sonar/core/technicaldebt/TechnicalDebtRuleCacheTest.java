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
package org.sonar.core.technicaldebt;

import com.google.common.collect.Lists;
import org.fest.assertions.Assertions;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

import java.util.Collections;

public class TechnicalDebtRuleCacheTest {

  @Test
  public void lazy_load_rules_on_first_call() throws Exception {

    RuleFinder ruleFinder = Mockito.mock(RuleFinder.class);
    Mockito.when(ruleFinder.findAll(Matchers.any(RuleQuery.class))).thenReturn(Collections.EMPTY_LIST);

    TechnicalDebtRuleCache technicalDebtRuleCache = new TechnicalDebtRuleCache(ruleFinder);
    technicalDebtRuleCache.getRule("", "");
    technicalDebtRuleCache.getRule("", "");

    Mockito.verify(ruleFinder, Mockito.times(1)).findAll(Matchers.any(RuleQuery.class));
  }

  @Test
  public void return_matching_rule() throws Exception {

    Rule rule1 = Rule.create("repo1", "rule1");
    Rule rule2 = Rule.create("repo2", "rule2");

    RuleFinder ruleFinder = Mockito.mock(RuleFinder.class);
    Mockito.when(ruleFinder.findAll(Matchers.any(RuleQuery.class))).thenReturn(Lists.newArrayList(rule1, rule2));

    TechnicalDebtRuleCache technicalDebtRuleCache = new TechnicalDebtRuleCache(ruleFinder);
    Rule actualRule1 = technicalDebtRuleCache.getRule("repo1", "rule1");
    Rule actualRule2 = technicalDebtRuleCache.getRule("repo2", "rule2");

    Assertions.assertThat(actualRule1).isEqualTo(rule1);
    Assertions.assertThat(actualRule2).isEqualTo(rule2);
  }

  @Test
  public void return_if_rule_exists() throws Exception {

    Rule rule1 = Rule.create("repo1", "rule1");
    Rule rule2 = Rule.create("repo2", "rule2");

    RuleFinder ruleFinder = Mockito.mock(RuleFinder.class);
    Mockito.when(ruleFinder.findAll(Matchers.any(RuleQuery.class))).thenReturn(Lists.newArrayList(rule1));

    TechnicalDebtRuleCache technicalDebtRuleCache = new TechnicalDebtRuleCache(ruleFinder);

    Assertions.assertThat(technicalDebtRuleCache.exists(rule1)).isTrue();
    Assertions.assertThat(technicalDebtRuleCache.exists(rule2)).isFalse();
  }
}
