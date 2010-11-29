/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.components;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Collection;

import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class DefaultRuleFinderTest extends AbstractDbUnitTestCase {

  @Test
  public void shouldFindById() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    assertThat(finder.findById(3).getConfigKey(), is("Checker/Treewalker/AnnotationUseStyleCheck"));
  }

  @Test
  public void shouldNotFindDisabledRuleById() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    assertThat(finder.findById(2), nullValue());
  }

  @Test
  public void shouldFindByKey() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Rule rule = finder.findByKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck");
    assertNotNull(rule);
    assertThat(rule.getKey(), is("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck"));
    assertThat(rule.isEnabled(), is(true));
  }

  @Test
  public void findShouldReturnNullIfNoResults() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    assertNull(finder.findByKey("checkstyle", "unknown"));
    assertNull(finder.find(RuleQuery.create().withRepositoryKey("checkstyle").withConfigKey("unknown")));
  }

  @Test
  public void findRepositoryRules() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create().withRepositoryKey("checkstyle"));
    assertNotNull(rules);
    assertThat(rules.size(), is(2)); // only enabled checkstyle rules
  }

  @Test
  public void findAllEnabled() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create());
    assertNotNull(rules);
    assertThat(rules.size(), is(3)); // only enabled checkstyle+pmd rules
    for (Rule rule : rules) {
      assertThat(rule.getId(), anyOf(is(1), is(3), is(4)));
    }
  }

  @Test
  public void doNotFindDisabledRules() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Rule rule = finder.findByKey("checkstyle", "DisabledCheck");
    assertNull(rule);
  }

  @Test
  public void doNotFindUnknownRules() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create().withRepositoryKey("unknown_repository"));
    assertThat(rules.size(), is(0));
  }
}
