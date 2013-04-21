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
package org.sonar.jpa.dao;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class RulesDaoTest extends AbstractDbUnitTestCase {

  private RulesDao rulesDao;

  @Before
  public void setup() {
    rulesDao = new RulesDao(getSession());
  }

  @Test
  public void shouldGetRules() {
    setupData("shouldGetRules");

    List<Rule> rules = rulesDao.getRules();
    assertThat(rules, notNullValue());
    assertThat(rules.size(), is(2));

    assertEquals("rule_one", rules.get(0).getKey());
    assertEquals(1, rules.get(0).getParams().size());
  }

  @Test
  public void shouldGetRuleWithRuleKeyAndPluginKey() {
    setupData("shouldGetRuleWithRuleKeyAndPluginKey");

    Rule rule = rulesDao.getRuleByKey("plugin", "checkstyle.rule1");
    assertThat(rule, notNullValue());
    assertThat(rule.getId(), notNullValue());

    Rule rule2 = rulesDao.getRuleByKey("plugin", "key not found");
    assertThat(rule2, nullValue());
  }

}
