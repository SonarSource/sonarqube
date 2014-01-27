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

package org.sonar.server.startup;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.*;
import org.sonar.api.utils.SonarException;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.jpa.test.AbstractDbUnitTestCase;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.rule.RuleRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class RegisterRulesTest extends AbstractDbUnitTestCase {

  RegisterRules task;
  ProfilesManager profilesManager;
  RuleRegistry ruleRegistry;
  RuleI18nManager ruleI18nManager;

  @Before
  public void init() {
    profilesManager = mock(ProfilesManager.class);
    ruleRegistry = mock(RuleRegistry.class);
    ruleI18nManager = mock(RuleI18nManager.class);
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new FakeRepository()}, ruleI18nManager, profilesManager, ruleRegistry);
  }

  @Test
  public void should_save_new_repositories() {
    setupData("shared");
    task.start();

    verify(ruleRegistry).bulkRegisterRules();

    List<Rule> result = getSession().getResults(Rule.class, "pluginName", "fake");
    assertThat(result.size()).isEqualTo(2);

    Rule first = result.get(0);
    assertThat(first.getKey()).isEqualTo("rule1");
    assertThat(first.getRepositoryKey()).isEqualTo("fake");
    assertThat(first.isEnabled()).isEqualTo(true);
    assertThat(first.getCreatedAt()).isNotNull();
    assertThat(first.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(first.getLanguage()).isEqualTo("java");
    assertThat(first.getParams().size()).isEqualTo(2);
  }

  @Test
  public void save_rule_param_description_from_bundle() {
    setupData("empty");
    when(ruleI18nManager.getParamDescription("fake", "rule2", "param")).thenReturn("Param description of rule2");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.getParams()).hasSize(1);
    assertThat(rule.getParams().get(0).getDescription()).isEqualTo("Param description of rule2");
  }

  @Test
  public void not_save_rule_param_description_from_bundle_on_empty_value() {
    setupData("empty");
    when(ruleI18nManager.getParamDescription("fake", "rule2", "param")).thenReturn("");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.getParams()).hasSize(1);
    assertThat(rule.getParams().get(0).getDescription()).isNull();
  }

  @Test
  public void should_update_template_rule() {
    setupData("should_update_template_rule_language");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.getRepositoryKey()).isEqualTo("fake");
    assertThat(rule.getLanguage()).isEqualTo("java");
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_READY);

    rule = getSession().getSingleResult(Rule.class, "id", 4);
    assertThat(rule.getRepositoryKey()).isEqualTo("fake");
    assertThat(rule.getLanguage()).isEqualTo("java");
    // parent status is now DEPRECATED but template should not be changed
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_READY);
  }

  @Test
  public void should_disable_deprecated_repositories() {
    setupData("shared");
    task.start();

    List<Rule> rules = getSession()
        .createQuery("from " + Rule.class.getSimpleName() + " where pluginName<>'fake'")
        .getResultList();
    assertThat(rules.size()).isGreaterThan(0);
    for (Rule rule : rules) {
      assertThat(rule.isEnabled()).isEqualTo(false);
      assertThat(rule.getUpdatedAt()).isNotNull();
    }
  }

  @Test
  public void should_notify_for_removed_rules() {
    setupData("shared");
    task.start();

    verify(profilesManager).removeActivatedRules(any(Rule.class));
  }

  @Test
  public void should_reactivate_disabled_rules() {
    setupData("reactivateDisabledRules");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_READY);
    assertThat(rule.getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_not_reactivate_disabled_template_rules() {
    setupData("should_reactivate_disabled_template_rules");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_REMOVED);
    assertThat(rule.getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_not_update_already_disabled_rules() {
    setupData("notUpdateAlreadyDisabledRule");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.getStatus()).isEqualTo(Rule.STATUS_REMOVED);
    assertThat(rule.getUpdatedAt()).isNull();
  }

  @Test
  public void should_disable_deprecated_active_rules() {
    setupData("disableDeprecatedActiveRules");
    task.start();

    List<Rule> result = getSession().getResults(Rule.class, "pluginName", "fake");
    assertThat(result.size()).isEqualTo(3);

    Rule deprecated = result.get(0);
    assertThat(deprecated.getKey()).isEqualTo("deprecated");
    assertThat(deprecated.isEnabled()).isEqualTo(false);
    assertThat(deprecated.getUpdatedAt()).isNotNull();

    assertThat(result.get(1).isEnabled()).isEqualTo(true);
    assertThat(result.get(2).isEnabled()).isEqualTo(true);
  }

  @Test
  public void should_disable_deprecated_active_rule_parameters() {
    setupData("disableDeprecatedActiveRuleParameters");
    task.start();

    ActiveRule arule = getSession().getSingleResult(ActiveRule.class, "id", 1);
    assertThat(arule.getActiveRuleParams().size()).isEqualTo(2);
    assertThat(getSession().getSingleResult(ActiveRuleParam.class, "id", 3)).isNull();
  }

  @Test
  public void should_disable_deprecated_rules() {
    setupData("disableDeprecatedRules");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.isEnabled()).isEqualTo(false);
    assertThat(rule.getUpdatedAt()).isNotNull();

    rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.isEnabled()).isEqualTo(false);
    assertThat(rule.getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_update_rule_fields() {
    setupData("updadeRuleFields");
    task.start();

    // fields have been updated with new values
    Rule rule1 = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule1.getName()).isEqualTo("One");
    assertThat(rule1.getDescription()).isEqualTo("Description of One");
    assertThat(rule1.getSeverity()).isEqualTo(RulePriority.BLOCKER);
    assertThat(rule1.getConfigKey()).isEqualTo("config1");
    assertThat(rule1.getUpdatedAt()).isNotNull();

    Rule rule2 = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule2.getStatus()).isEqualTo(Rule.STATUS_DEPRECATED);
    assertThat(rule2.getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_store_bundle_name_and_description_in_database() {
    setupData("updadeRuleFields");
    String i18nName = "The One";
    String i18nDescription = "The Description of One";
    when(ruleI18nManager.getName("fake", "rule1")).thenReturn(i18nName);
    when(ruleI18nManager.getDescription("fake", "rule1")).thenReturn(i18nDescription);
    task.start();

    // fields have been updated with new values
    Rule rule1 = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule1.getName()).isEqualTo(i18nName);
    assertThat(rule1.getDescription()).isEqualTo(i18nDescription);
    assertThat(rule1.getSeverity()).isEqualTo(RulePriority.BLOCKER);
    assertThat(rule1.getConfigKey()).isEqualTo("config1");
    assertThat(rule1.getUpdatedAt()).isNotNull();

    Rule rule2 = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule2.getStatus()).isEqualTo(Rule.STATUS_DEPRECATED);
    assertThat(rule2.getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_update_rule_parameters() {
    setupData("updateRuleParameters");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 1);
    assertThat(rule.getParams().size()).isEqualTo(2);

    // new parameter
    assertThat(rule.getParam("param2")).isNotNull();
    assertThat(rule.getParam("param2").getDescription()).isEqualTo("parameter two");
    assertThat(rule.getParam("param2").getDefaultValue()).isEqualTo("default value two");

    // updated parameter
    assertThat(rule.getParam("param1")).isNotNull();
    assertThat(rule.getParam("param1").getDescription()).isEqualTo("parameter one");
    assertThat(rule.getParam("param1").getDefaultValue()).isEqualTo("default value one");

    // deleted parameter
    assertThat(rule.getParam("deprecated_param")).isNull();
    assertThat(getSession().getSingleResult(RuleParam.class, "id", 2)).isNull(); // id of deprecated_param is 2
  }

  @Test
  public void should_not_disable_template_rules_if_parent_is_enabled() {
    setupData("doNotDisableUserRulesIfParentIsEnabled");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.isEnabled()).isEqualTo(true);
  }

  @Test
  public void should_disable_template_rules_if_parent_is_disabled() {
    setupData("disableUserRulesIfParentIsDisabled");
    task.start();

    Rule rule = getSession().getSingleResult(Rule.class, "id", 2);
    assertThat(rule.isEnabled()).isEqualTo(false);
    assertThat(rule.getUpdatedAt()).isNotNull();

    assertThat(getSession().getSingleResult(Rule.class, "id", 4).isEnabled()).isEqualTo(false);
  }

  @Test
  public void should_not_disable_manual_rules() {
    // the hardcoded repository "manual" is used for manual violations
    setupData("shouldNotDisableManualRules");
    task.start();

    assertThat(getSession().getSingleResult(Rule.class, "id", 1).isEnabled()).isEqualTo(true);
    assertThat(getSession().getSingleResult(Rule.class, "id", 2).isEnabled()).isEqualTo(false);
  }

  @Test
  public void volume_testing() {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new VolumeRepository()}, ruleI18nManager, profilesManager, ruleRegistry);
    setupData("shared");
    task.start();

    List<Rule> result = getSession().getResults(Rule.class, "status", Rule.STATUS_READY);
    assertThat(result.size()).isEqualTo(VolumeRepository.SIZE);
  }

  // SONAR-3305
  @Test
  public void should_fail_with_rule_without_name() throws Exception {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutNameRepository()}, ruleI18nManager, profilesManager, ruleRegistry);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a name");
    } catch (SonarException e) {
      assertThat(e.getMessage()).contains("must have a name");
    }

    // now it is ok, the rule has a name in the English bundle
    when(ruleI18nManager.getName(anyString(), anyString())).thenReturn("Name");
    when(ruleI18nManager.getDescription(anyString(), anyString())).thenReturn("Description");
    task.start();
  }

  // SONAR-3769
  @Test
  public void should_fail_with_rule_with_blank_name() throws Exception {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutNameRepository()}, ruleI18nManager, profilesManager, ruleRegistry);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a name");
    } catch (SonarException e) {
      assertThat(e.getMessage()).contains("must have a name");
    }
  }

  // SONAR-3305
  @Test
  public void should_fail_with_rule_without_description() throws Exception {
    when(ruleI18nManager.getName(anyString(), anyString())).thenReturn("Name");
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutDescriptionRepository()}, ruleI18nManager, profilesManager, ruleRegistry);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a description");
    } catch (SonarException e) {
      assertThat(e.getMessage()).contains("must have a description");
    }

    // now it is ok, the rule has a name & a description in the English bundle
    when(ruleI18nManager.getName(anyString(), anyString())).thenReturn("Name");
    when(ruleI18nManager.getDescription(anyString(), anyString())).thenReturn("Description");
    task.start();
  }

  // http://jira.codehaus.org/browse/SONAR-3722
  @Test
  public void should_fail_with_rule_without_name_in_bundle() throws Exception {
    task = new RegisterRules(getSessionFactory(), new RuleRepository[] {new RuleWithoutDescriptionRepository()}, ruleI18nManager, profilesManager, ruleRegistry);
    setupData("shared");

    // the rule has no name, it should fail
    try {
      task.start();
      fail("Rule must have a description");
    } catch (SonarException e) {
      assertThat(e.getMessage()).contains("No description found for the rule 'Rule 1' (repository: rule-without-description-repo) " +
        "because the entry 'rule.rule-without-description-repo.rule1.name' is missing from the bundle.");
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
    rule2.setStatus(Rule.STATUS_DEPRECATED);
    rule2.createParameter("param");

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
