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
package org.sonar.core.rule;

import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RuleQuery;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Collection;

import static com.google.common.collect.Lists.newArrayList;
import static org.fest.assertions.Assertions.assertThat;

public class DefaultRuleFinderTest extends AbstractDbUnitTestCase {

  @Test
  public void should_find_by_id() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    assertThat(finder.findById(3).getConfigKey()).isEqualTo("Checker/Treewalker/AnnotationUseStyleCheck");
  }

  @Test
  public void should_not_find_disabled_rule_by_id() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    assertThat(finder.findById(2)).isNull();
  }

  @Test
  public void should_find_by_ids() {
    setupData("shared");
    DefaultRuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    // 2 is returned even its status is REMOVED
    assertThat(finder.findByIds(newArrayList(2, 3))).hasSize(2);
  }

  @Test
  public void should_find_by_ids_empty() {
    Collection<Integer> newArrayList = newArrayList();
    assertThat(new DefaultRuleFinder(getSessionFactory()).findByIds(newArrayList)).isEmpty();
  }

  @Test
  public void should_find_by_key() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Rule rule = finder.findByKey("checkstyle", "com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck");
    assertThat(rule).isNotNull();
    assertThat(rule.getKey()).isEqualTo(("com.puppycrawl.tools.checkstyle.checks.header.HeaderCheck"));
    assertThat(rule.isEnabled()).isTrue();
  }

  @Test
  public void find_should_return_null_if_no_results() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    assertThat(finder.findByKey("checkstyle", "unknown")).isNull();
    assertThat(finder.find(RuleQuery.create().withRepositoryKey("checkstyle").withConfigKey("unknown"))).isNull();
  }

  @Test
  public void find_repository_rules() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create().withRepositoryKey("checkstyle"));

    assertThat(rules).hasSize(2);
  }

  @Test
  public void find_all_enabled() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create());

    assertThat(rules).onProperty("id").containsOnly(1, 3, 4);
  }

  @Test
  public void do_not_find_disabled_rules() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Rule rule = finder.findByKey("checkstyle", "DisabledCheck");
    assertThat(rule).isNull();
  }

  @Test
  public void do_not_find_unknown_rules() {
    setupData("shared");
    RuleFinder finder = new DefaultRuleFinder(getSessionFactory());
    Collection<Rule> rules = finder.findAll(RuleQuery.create().withRepositoryKey("unknown_repository"));
    assertThat(rules).isEmpty();
  }
}
