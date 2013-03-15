/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2008-2012 SonarSource
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

package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.ActiveRuleParam;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleParam;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.SonarException;
import org.sonar.check.Status;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.rule.RuleStatus;
import org.sonar.jpa.test.AbstractDbUnitTestCase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.number.OrderingComparisons.greaterThan;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RegisterRulesTest extends AbstractDbUnitTestCase {

  private RegisterRules task;

  @Before
  public void init() {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new FakeRepository()}, null);
  }

  @Test
  public void should_save_new_repositories() {
    setupData("shared");
    task.start();

    List<Rule> result = getSession().getResults(Rule.class, "pluginName", "fake");
    assertThat(result.size(), is(2));

    Rule first = result.get(0);
    assertThat(first.getKey(), is("rule1"));
    assertThat(first.getRepositoryKey(), is("fake"));
    assertThat(first.isEnabled(), is(true));
    assertThat(first.getCreatedAt(), notNullValue());
    assertThat(first.getStatus(), is(Status.READY.name()));
    assertThat(first.getLanguage(), is("java"));
    assertThat(first.getParams().size(), is(2));
  }

  @Test
  public void should_disable_deprecated_repositories() {
    setupData("shared");
    task.start();

    List<Rule> rules = getSession()
        .createQuery("from " + Rule.class.getSimpleName() + " where pluginName<>'fake'")
        .getResultList();
    assertThat(rules.size(), greaterThan(0));
    for (Rule rule : rules) {
      assertThat(rule.isEnabled(), is(false));
      assertThat(rule.getUpdatedAt(), notNullValue());
    }
  }

  @Test
  public void should_reactivate_disabled_rules() {
    setupData("reactivateDisabledRules");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.getStatus(), is(RuleStatus.READY.name()));
    assertThat(rule.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_not_update_already_disabled_rules() {
    setupData("notUpdateAlreadyDisabledRule");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.getStatus(), is(RuleStatus.REMOVED.name()));
    assertThat(rule.getUpdatedAt(), nullValue());
  }

  @Test
  public void should_disable_deprecated_active_rules() {
    setupData("disableDeprecatedActiveRules");
    task.start();

    List<Rule> result = getSession().getResults(Rule.class, "pluginName", "fake");
    assertThat(result.size(), is(3));

    Rule deprecated = result.get(0);
    assertThat(deprecated.getKey(), is("deprecated"));
    assertThat(deprecated.isEnabled(), is(false));
    assertThat(deprecated.getUpdatedAt(), notNullValue());

    assertThat(result.get(1).isEnabled(), is(true));
    assertThat(result.get(2).isEnabled(), is(true));
  }

  @Test
  public void should_disable_Deprecated_Active_Rule_Parameters() {
    setupData("disableDeprecatedActiveRuleParameters");
    task.start();

    ActiveRule arule = getSession().getSingleResult(ActiveRule.class, "id", 1);
    assertThat(arule.getActiveRuleParams().size(), is(2));
    assertNull(getSession().getSingleResult(ActiveRuleParam.class, "id", 3));
  }

  @Test
  public void should_disable_deprecated_rules() {
    setupData("disableDeprecatedRules");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.isEnabled(), is(false));
    assertThat(rule.getUpdatedAt(), notNullValue());

    rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.isEnabled(), is(false));
    assertThat(rule.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_update_rule_fields() {
    setupData("updadeRuleFields");
    task.start();

    // fields have been updated with new values
    Rule rule1 = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule1.getName(), is("One"));
    assertThat(rule1.getDescription(), is("Description of One"));
    assertThat(rule1.getSeverity(), is(RulePriority.BLOCKER));
    assertThat(rule1.getConfigKey(), is("config1"));
    assertThat(rule1.getUpdatedAt(), notNullValue());

    Rule rule2 = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule2.getStatus(), is(Status.DEPRECATED.name()));
    assertThat(rule2.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_update_rule_parameters() {
    setupData("updateRuleParameters");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.getParams().size(), is(2));

    // new parameter
    assertNotNull(rule.getParam("param2"));
    assertThat(rule.getParam("param2").getDescription(), is("parameter two"));
    assertThat(rule.getParam("param2").getDefaultValue(), is("default value two"));

    // updated parameter
    assertNotNull(rule.getParam("param1"));
    assertThat(rule.getParam("param1").getDescription(), is("parameter one"));
    assertThat(rule.getParam("param1").getDefaultValue(), is("default value one"));

    // deleted parameter
    assertNull(rule.getParam("deprecated_param"));
    assertNull(getSession().getSingleResult(RuleParam.class, "id", 2)); // id of deprecated_param is 2
  }

  @Test
  public void should_not_disable_user_rules_if_parent_is_enabled() {
    setupData("doNotDisableUserRulesIfParentIsEnabled");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.isEnabled(), is(true));
  }

  @Test
  public void should_disable_user_rules_if_parent_is_disabled() {
    setupData("disableUserRulesIfParentIsDisabled");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.isEnabled(), is(false));
    assertThat(rule.getUpdatedAt(), notNullValue());
  }

  @Test
  public void should_not_disable_manual_rules() {
    // the hardcoded repository "manual" is used for manual violations
    setupData("shouldNotDisableManualRules");
    task.start();

    assertThat(getSession().getSingleResult(Rule.class, "id", 1).isEnabled(), is(true));
    assertThat(getSession().getSingleResult(Rule.class, "id", 2).isEnabled(), is(false));
  }

  @Test
  public void volume_testing() {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new VolumeRepository()}, null);
    setupData("shared");
    task.start();

    List<Rule> result = getSession().getResults(Rule.class, "status", RuleStatus.READY.name());
    assertThat(result.size(), is(VolumeRepository.SIZE));
  }

  // SONAR-3305
  @Test
  public void should_fail_with_rule_without_name() throws Exception {
    RuleI18nManager ruleI18nManager = mock(RuleI18nManager.class);
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutNameRepository()}, ruleI18nManager);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a name");
    } catch (SonarException e) {
      assertThat(e.getMessage(), containsString("must have a name"));
    }

    // now it is ok, the rule has a name in the English bundle
    when(ruleI18nManager.getName(anyString(), anyString(), any(Locale.class))).thenReturn("Name");
    when(ruleI18nManager.getDescription(anyString(), anyString(), any(Locale.class))).thenReturn("Description");
    task.start();
  }

  // SONAR-3769
  @Test
  public void should_fail_with_rule_with_blank_name() throws Exception {
    RuleI18nManager ruleI18nManager = mock(RuleI18nManager.class);
    when(ruleI18nManager.getName(anyString(), anyString(), any(Locale.class))).thenReturn("");
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutNameRepository()}, ruleI18nManager);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a name");
    } catch (SonarException e) {
      assertThat(e.getMessage(), containsString("must have a name"));
    }
  }

  // SONAR-3305
  @Test
  public void should_fail_with_rule_without_description() throws Exception {
    RuleI18nManager ruleI18nManager = mock(RuleI18nManager.class);
    when(ruleI18nManager.getName(anyString(), anyString(), any(Locale.class))).thenReturn("Name");
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutDescriptionRepository()}, ruleI18nManager);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a description");
    } catch (SonarException e) {
      assertThat(e.getMessage(), containsString("must have a description"));
    }

    // now it is ok, the rule has a name & a description in the English bundle
    when(ruleI18nManager.getName(anyString(), anyString(), any(Locale.class))).thenReturn("Name");
    when(ruleI18nManager.getDescription(anyString(), anyString(), any(Locale.class))).thenReturn("Description");
    task.start();
  }

  // http://jira.codehaus.org/browse/SONAR-3722
  @Test
  public void should_fail_with_rule_without_name_in_bundle() throws Exception {
    RuleI18nManager ruleI18nManager = mock(RuleI18nManager.class);
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutDescriptionRepository()}, ruleI18nManager);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a description");
    } catch (SonarException e) {
      assertThat(e.getMessage(), containsString("No description found for the rule 'Rule 1' (repository: rule-without-description-repo) " +
        "because the entry 'rule.rule-without-description-repo.rule1.name' is missing from the bundle."));
    }
  }

  // http://jira.codehaus.org/browse/SONAR-3879
  @Test
  public void should_fail_with_rule_with_unknown_status() throws Exception {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithUnkownStatusRepository()}, null);
    try {
      task.start();
      fail("Rule status must be unknown");
    } catch (SonarException e) {
      assertThat(e.getMessage(), containsString("The status of a rule can only contains : "));
    }
  }
}

class FakeRepository extends RuleRepository {
  public FakeRepository() {
    super("fake", "java");
  }

  @Override
  public List<Rule> createRules() {
    Rule rule1 = Rule.create("fake", "rule1", "One");
    rule1.setDescription("Description of One");
    rule1.setSeverity(RulePriority.BLOCKER);
    rule1.setConfigKey("config1");
    rule1.createParameter("param1").setDescription("parameter one").setDefaultValue("default value one");
    rule1.createParameter("param2").setDescription("parameter two").setDefaultValue("default value two");

    Rule rule2 = Rule.create("fake", "rule2", "Two");
    rule2.setDescription("Description of Two");
    rule2.setSeverity(RulePriority.INFO);
    rule2.setStatus(Status.DEPRECATED.name());

    return Arrays.asList(rule1, rule2);
  }
}

class RuleWithoutNameRepository extends RuleRepository {
  public RuleWithoutNameRepository() {
    super("rule-without-name-repo", "java");
  }

  @Override
  public List<Rule> createRules() {
    // Rules must not have empty name
    Rule rule1 = Rule.create("fake", "rule1", null);
    return Arrays.asList(rule1);
  }
}

class RuleWithoutDescriptionRepository extends RuleRepository {
  public RuleWithoutDescriptionRepository() {
    super("rule-without-description-repo", "java");
  }

  @Override
  public List<Rule> createRules() {
    // Rules must not have empty description
    Rule rule1 = Rule.create("fake", "rule1", "Rule 1");
    return Arrays.asList(rule1);
  }
}

class VolumeRepository extends RuleRepository {
  static final int SIZE = 500;

  public VolumeRepository() {
    super("volume", "java");
  }

  @Override
  public List<Rule> createRules() {
    List<Rule> rules = new ArrayList<Rule>();
    for (int i = 0; i < SIZE; i++) {
      Rule rule = Rule.create("volume", "rule" + i, "name of " + i);
      rule.setDescription("description of " + i);
      rule.setSeverity(RulePriority.BLOCKER);
      for (int j = 0; j < 20; j++) {
        rule.createParameter("param" + j);
      }
      rules.add(rule);
    }
    return rules;
  }
}

class RuleWithUnkownStatusRepository extends RuleRepository {
  public RuleWithUnkownStatusRepository() {
    super("rule-with-unknwon-status-repo", "java");
  }

  @Override
  public List<Rule> createRules() {
    Rule rule1 = Rule.create("fake", "rule1", "rule1").setDescription("Description").setStatus("UNKNOWN");
    return Arrays.asList(rule1);
  }
}
