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
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;

import java.util.Collections;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class RuleCacheTest {

  @Test
  public void should_lazy_load_rules_on_first_call() throws Exception {

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(Collections.EMPTY_LIST);

    RuleCache ruleCache = new RuleCache(ruleFinder);
    ruleCache.getRule("", "");
    ruleCache.getRule("", "");

    verify(ruleFinder, times(1)).findAll(any(RuleQuery.class));
  }

  @Test
  public void should_return_matching_rule() throws Exception {

    Rule rule1 = Rule.create("repo1", "rule1");
    Rule rule2 = Rule.create("repo2", "rule2");

    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(Lists.newArrayList(rule1, rule2));

    RuleCache ruleCache = new RuleCache(ruleFinder);
    Rule actualRule1 = ruleCache.getRule("repo1", "rule1");
    Rule actualRule2 = ruleCache.getRule("repo2", "rule2");

    assertThat(actualRule1).isEqualTo(rule1);
    assertThat(actualRule2).isEqualTo(rule2);
  }
}
