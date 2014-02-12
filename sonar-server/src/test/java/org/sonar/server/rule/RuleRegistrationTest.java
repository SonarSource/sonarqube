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

package org.sonar.server.rule;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleDefinitions;
import org.sonar.core.persistence.AbstractDaoTestCase;
import org.sonar.core.persistence.MyBatis;
import org.sonar.core.qualityprofile.db.ActiveRuleDao;
import org.sonar.core.rule.RuleDao;
import org.sonar.core.rule.RuleTagDao;
import org.sonar.server.qualityprofile.ProfilesManager;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class RuleRegistrationTest extends AbstractDaoTestCase {

  private static final String[] EXCLUDED_COLUMN_NAMES = {
      "created_at", "updated_at", "note_data", "note_user_login", "note_created_at", "note_updated_at"};

  RuleRegistration task;
  ProfilesManager profilesManager = mock(ProfilesManager.class);
  RuleRegistry ruleRegistry = mock(RuleRegistry.class);
  ESRuleTags esRuleTags = mock(ESRuleTags.class);
  RuleTagOperations ruleTagOperations;
  MyBatis myBatis;
  RuleDao ruleDao;
  RuleTagDao ruleTagDao;
  ActiveRuleDao activeRuleDao;

  @Before
  public void before() {
    myBatis = getMyBatis();
    ruleDao = new RuleDao(myBatis);
    ruleTagDao = new RuleTagDao(myBatis);
    activeRuleDao = new ActiveRuleDao(myBatis);
    ruleTagOperations = new RuleTagOperations(ruleTagDao, esRuleTags);
    task = new RuleRegistration(new RuleDefinitionsLoader(mock(RuleRepositories.class), new RuleDefinitions[]{new FakeRepository()}),
      profilesManager, ruleRegistry, esRuleTags, ruleTagOperations, myBatis, ruleDao, ruleTagDao, activeRuleDao);
  }

  @Test
  public void should_insert_new_rules() {
    setupData("shared");
    task.start();

    checkTables("should_insert_new_rules", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "rules_rule_tags", "rule_tags");
  }

  @Test
  public void should_update_template_rule_language() {
    setupData("should_update_template_rule_language");
    task.start();

    checkTables("should_update_template_rule_language", EXCLUDED_COLUMN_NAMES, "rules");
  }

  /**
   * SONAR-4642
   */
  @Test
  public void notify_for_removed_rules_when_repository_is_still_existing() {
    setupData("notify_for_removed_rules_when_repository_is_still_existing");
    task.start();

    verify(profilesManager).removeActivatedRules(1);
  }

  /**
   * SONAR-4642
   */
  @Test
  public void not_notify_for_removed_rules_when_repository_do_not_exists_anymore() {
    setupData("shared");
    task.start();

    verifyZeroInteractions(profilesManager);
  }

  @Test
  public void should_reactivate_disabled_rules() {
    setupData("should_reactivate_disabled_rules");
    task.start();

    checkTables("should_reactivate_disabled_rules", EXCLUDED_COLUMN_NAMES, "rules");

    assertThat(ruleDao.selectById(1).getUpdatedAt()).isNotNull();
  }

  @Test
  public void reactivate_disabled_template_rules() {
    setupData("reactivate_disabled_template_rules");
    task.start();

    checkTables("reactivate_disabled_template_rules", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_disable_deprecated_active_rules() {
    setupData("should_disable_deprecated_active_rules");
    task.start();

    checkTables("should_disable_deprecated_active_rules", EXCLUDED_COLUMN_NAMES, "rules");
  }

  @Test
  public void should_disable_deprecated_active_rule_params() {
    setupData("should_disable_deprecated_active_rule_params");
    task.start();

    checkTables("should_disable_deprecated_active_rule_params", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "active_rules", "active_rule_parameters");
  }

  @Test
  public void should_disable_deprecated_rules() {
    setupData("should_disable_deprecated_rules");
    task.start();

    checkTables("should_disable_deprecated_rules", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "rules_rule_tags", "rule_tags");
  }

  @Test
  public void should_update_rule_fields() {
    setupData("updadeRuleFields");
    task.start();

    checkTables("updadeRuleFields", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters", "rule_tags", "rules_rule_tags");
  }

  @Test
  public void should_update_rule_parameters() {
    setupData("should_update_rule_parameters");
    task.start();

    checkTables("should_update_rule_parameters", EXCLUDED_COLUMN_NAMES, "rules", "rules_parameters");
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
  public void test_high_number_of_rules() {
    task = new RuleRegistration(new RuleDefinitionsLoader(mock(RuleRepositories.class), new RuleDefinitions[]{new BigRepository()}),
      profilesManager, ruleRegistry, esRuleTags, ruleTagOperations, myBatis, ruleDao, ruleTagDao, activeRuleDao);

    setupData("shared");
    task.start();

    // There is already one rule in DB
    assertThat(ruleDao.selectAll()).hasSize(BigRepository.SIZE + 1);
    assertThat(ruleDao.selectParameters()).hasSize(BigRepository.SIZE * 20);
    assertThat(ruleDao.selectTags(getMyBatis().openSession())).hasSize(BigRepository.SIZE * 3);
  }

  @Test
  public void should_insert_extended_repositories() {
    task = new RuleRegistration(new RuleDefinitionsLoader(mock(RuleRepositories.class), new RuleDefinitions[]{
        new FindbugsRepository(), new FbContribRepository()}),
      profilesManager, ruleRegistry, esRuleTags, ruleTagOperations, myBatis, ruleDao, ruleTagDao, activeRuleDao);

    setupData("empty");
    task.start();

    checkTables("should_insert_extended_repositories", EXCLUDED_COLUMN_NAMES, "rules");
  }

  static class FakeRepository implements RuleDefinitions {
    @Override
    public void define(Context context) {
      NewRepository repo = context.newRepository("fake", "java");

      NewRule rule1 = repo.newRule("rule1")
          .setName("One")
          .setHtmlDescription("Description of One")
          .setSeverity(Severity.BLOCKER)
          .setInternalKey("config1")
          .setTags("tag1", "tag3", "tag5");
      rule1.newParam("param1").setDescription("parameter one").setDefaultValue("default value one");
      rule1.newParam("param2").setDescription("parameter two").setDefaultValue("default value two");

      repo.newRule("rule2")
          .setName("Two")
          .setHtmlDescription("Description of Two")
          .setSeverity(Severity.INFO)
          .setStatus(RuleStatus.DEPRECATED);
      repo.done();
    }
  }

  static class BigRepository implements RuleDefinitions {
    static final int SIZE = 500;

    @Override
    public void define(Context context) {
      NewRepository repo = context.newRepository("big", "java");
      for (int i = 0; i < SIZE; i++) {
        NewRule rule = repo.newRule("rule" + i)
            .setName("name of " + i)
            .setHtmlDescription("description of " + i)
            .setSeverity(Severity.BLOCKER)
            .setInternalKey("config1")
            .setTags("tag1", "tag3", "tag5");
        for (int j = 0; j < 20; j++) {
          rule.newParam("param" + j);
        }

      }
      repo.done();
    }
  }

  static class FindbugsRepository implements RuleDefinitions {
    @Override
    public void define(Context context) {
      NewRepository repo = context.newRepository("findbugs", "java");
      repo.newRule("rule1")
          .setName("Rule One")
          .setHtmlDescription("Description of Rule One");
      repo.done();
    }
  }

  static class FbContribRepository implements RuleDefinitions {
    @Override
    public void define(Context context) {
      NewExtendedRepository repo = context.extendRepository("findbugs", "java");
      repo.newRule("rule2")
          .setName("Rule Two")
          .setHtmlDescription("Description of Rule Two");
      repo.done();
    }
  }
}

