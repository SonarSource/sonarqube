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

import org.apache.ibatis.session.SqlSession;
import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.utils.SonarException;
import org.sonar.core.i18n.RuleI18nManager;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.server.configuration.ProfilesManager;
import org.sonar.server.rule.RuleRegistry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RegisterRulesTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMN_NAMES = {
    "created_at", "updated_at", "note_data", "note_user_login", "note_created_at", "note_updated_at"};

  RegisterRules task;
  ProfilesManager profilesManager;
  RuleRegistry ruleRegistry;
  RuleI18nManager ruleI18nManager;
  MyBatis myBatis;
  SqlSession sqlSession;
  RuleDao ruleDao;
  ActiveRuleDao activeRuleDao;

  @Before
  public void init() {
    profilesManager = mock(ProfilesManager.class);
    ruleRegistry = mock(RuleRegistry.class);
    ruleI18nManager = mock(RuleI18nManager.class);
    myBatis = getMyBatis();
    ruleDao = new RuleDao(myBatis);
    activeRuleDao = new ActiveRuleDao(myBatis);
    task = new RegisterRules(new RuleRepository[] {new FakeRepository()}, ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
  }

  @Test
  public void should_save_new_repositories() {
    setupData("shared");
    task.start();

    verify(ruleRegistry).bulkRegisterRules();
    checkTables("should_save_new_repositories", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "rule_tags");
  }

  @Test
  public void should_update_template_rule() {
    setupData("should_update_template_rule_language");
    task.start();

    checkTables("should_update_template_rule_language", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_notify_for_removed_rules() {
    setupData("shared");
    task.start();

    verify(profilesManager).removeActivatedRules(anyInt());
  }

  @Test
  public void should_reactivate_disabled_rules() {
    setupData("reactivateDisabledRules");
    task.start();

    checkTables("reactivateDisabledRules", EXCLUDED_COLUMN_NAMES, "rules");

    assertThat(ruleDao.selectById(1).getUpdatedAt()).isNotNull();
  }

  @Test
  public void should_not_reactivate_disabled_template_rules() {
    setupData("should_reactivate_disabled_template_rules");
    task.start();

    checkTables("should_reactivate_disabled_template_rules", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_not_update_already_disabled_rules() {
    setupData("notUpdateAlreadyDisabledRule");
    task.start();

    checkTables("should_save_new_repositories", EXCLUDED_COLUMN_NAMES, "rules");

    assertThat(ruleDao.selectById(1).getUpdatedAt()).isNull();
  }

  @Test
  public void should_disable_deprecated_active_rules() {
    setupData("disableDeprecatedActiveRules");
    task.start();

    checkTables("disableDeprecatedActiveRules", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_disable_deprecated_active_rule_parameters() {
    setupData("disableDeprecatedActiveRuleParameters");
    task.start();

    checkTables("disableDeprecatedActiveRuleParameters", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "active_rules", "active_rule_parameters");
  }

  @Test
  public void should_disable_deprecated_rules() {
    setupData("disableDeprecatedRules");
    task.start();

    checkTables("disableDeprecatedRules", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters");
  }

  @Test
  public void should_update_rule_fields() {
    setupData("updadeRuleFields");
    task.start();

    checkTables("updadeRuleFields", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "rule_tags");
  }

  @Test
  public void should_store_bundle_name_and_description_in_database() {
    setupData("updadeRuleFields");
    String i18nName = "The One";
    String i18nDescription = "The Description of One";
    when(ruleI18nManager.getName("fake", "rule1")).thenReturn(i18nName);
    when(ruleI18nManager.getDescription("fake", "rule1")).thenReturn(i18nDescription);
    task.start();

    checkTables("should_store_bundle_name_and_description_in_database", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_update_rule_parameters() {
    setupData("updateRuleParameters");
    task.start();

    checkTables("updateRuleParameters", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters");
  }

  @Test
  public void should_not_disable_template_rules_if_parent_is_enabled() {
    setupData("doNotDisableUserRulesIfParentIsEnabled");
    task.start();

    checkTables("doNotDisableUserRulesIfParentIsEnabled", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_disable_template_rules_if_parent_is_disabled() {
    setupData("disableUserRulesIfParentIsDisabled");
    task.start();

    checkTables("disableUserRulesIfParentIsDisabled", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_not_disable_manual_rules() {
    // the hardcoded repository "manual" is used for manual violations
    setupData("shouldNotDisableManualRules");
    task.start();

    checkTables("shouldNotDisableManualRules", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void volume_testing() {
    task = new RegisterRules(new RuleRepository[] {new VolumeRepository()}, ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
    setupData("shared");
    task.start();

    // There is already one rule in DB
    assertThat(ruleDao.selectAll()).hasSize(VolumeRepository.SIZE + 1);
  }

  // SONAR-3305
  @Test
  public void should_fail_with_rule_without_name() throws Exception {
    task = new RegisterRules(new RuleRepository[] {new RuleWithoutNameRepository()}, ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
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
    task = new RegisterRules(new RuleRepository[] {new RuleWithoutNameRepository()}, ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
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
    task = new RegisterRules(new RuleRepository[] {new RuleWithoutDescriptionRepository()}, ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
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
    task = new RegisterRules(new RuleRepository[] {new RuleWithoutDescriptionRepository()}, ruleI18nManager, profilesManager, ruleRegistry, myBatis, ruleDao, activeRuleDao);
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
    rule1.setTags("tag1", "tag3", "tag5");

    Rule rule2 = Rule.create("fake", "rule2", "Two");
    rule2.setDescription("Description of Two");
    rule2.setSeverity(RulePriority.INFO);
    rule2.setStatus(Rule.STATUS_DEPRECATED);

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
