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
package org.sonar.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.jpa.dao.RulesDao;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class RulesBackupTest extends AbstractDbUnitTestCase {

  private RulesBackup rulesBackup;
  private SonarConfig sonarConfig;

  private Rule rule;

  @Before
  public void setUp() {
    rulesBackup = new RulesBackup(getSession());
    sonarConfig = new SonarConfig();

    rule = Rule.create("repo", "key", "name").setDescription("description").setLanguage("language");
    rule.createParameter("param").setDefaultValue("value");
  }

  @Test
  public void shouldExportRules() {
    Rule userRule = createUserRule();
    RulesBackup rulesBackup = new RulesBackup(Arrays.asList(userRule));
    rulesBackup.exportXml(sonarConfig);

    assertThat(sonarConfig.getRules()).containsOnly(userRule);
  }

  @Test
  public void shouldImportRules() {
    getSession().save(rule);

    sonarConfig.setRules(Arrays.asList(createUserRule()));
    rulesBackup.importXml(sonarConfig);

    verify();
  }

  @Test
  public void shouldUpdateRules() {
    getSession().save(rule);
    getSession().save(Rule.create("repo", "key2", ""));

    sonarConfig.setRules(Arrays.asList(createUserRule()));
    rulesBackup.importXml(sonarConfig);

    verify();
  }

  @Test
  public void shouldIgnoreIncorrectRule() {
    sonarConfig.setRules(Arrays.asList(createUserRule()));
    rulesBackup.importXml(sonarConfig);

    assertThat(getSession().getResults(Rule.class).size(), is(0));
  }

  @Test
  public void shouldIgnoreIncorrectParam() {
    Rule rule = Rule.create("repo", "key", "name").setDescription("description");
    getSession().save(rule);
    sonarConfig.setRules(Arrays.asList(createUserRule()));
    rulesBackup.importXml(sonarConfig);

    assertThat(getSession().getResults(Rule.class).size(), is(2));
    RulesDao rulesDao = new RulesDao(getSession());
    Rule importedRule = rulesDao.getRuleByKey("repo", "key2");
    assertThat(importedRule, notNullValue());
    assertThat(rulesDao.getRuleParam(importedRule, "param"), nullValue());
  }

  private Rule createUserRule() {
    Rule userRule = Rule.create("repo", "key2", "name2").setDescription("description2");
    userRule.setParent(rule);
    userRule.setSeverity(RulePriority.INFO);
    userRule.createParameter("param").setDefaultValue("value");
    return userRule;
  }

  private void verify() {
    assertThat(getSession().getResults(Rule.class).size(), is(2));
    Rule importedRule = new RulesDao(getSession()).getRuleByKey("repo", "key2");
    assertThat(importedRule.getParent(), is(rule));
    assertThat(importedRule.isEnabled(), is(true));
    assertThat(importedRule.getName(), is("name2"));
    assertThat(importedRule.getDescription(), is("description2"));
    assertThat(importedRule.getSeverity(), is(RulePriority.INFO));
    assertThat(importedRule.getParams().size(), is(1));
    assertThat(importedRule.getLanguage(), is("language"));
    RuleParam param = importedRule.getParams().get(0);
    assertThat(param.getKey(), is("param"));
    assertThat(param.getDefaultValue(), is("value"));
  }
}
