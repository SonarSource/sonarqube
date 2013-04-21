/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.core.rule;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Collection;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

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

    assertThat(rules).hasSize(2);
  }

  @Test
  public void findAllEnabled() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create());

    assertThat(rules).onProperty("id").containsOnly(1, 3, 4);
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
