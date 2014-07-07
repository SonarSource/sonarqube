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

package org.sonar.server.rule;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang.time.DateUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.utils.MessageException;
import org.sonar.core.permission.GlobalPermissions;
import org.sonar.core.persistence.DbSession;
import org.sonar.core.rule.RuleDto;
import org.sonar.core.rule.RuleParamDto;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryOptions;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.user.MockUserSession;
import org.sonar.server.user.UserSession;

import javax.annotation.Nullable;

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RegisterRulesMediumTest {

  static final XooRulesDefinition RULE_DEFS = new XooRulesDefinition();

  @ClassRule
  public static final ServerTester TESTER = new ServerTester().addComponents(RULE_DEFS);

  RuleIndex index;
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    TESTER.clearDbAndIndexes();
    db = TESTER.get(DbClient.class);
    dbSession = TESTER.get(DbClient.class).openSession(false);
    dbSession.clearCache();
    index = TESTER.get(RuleIndex.class);
  }

  @After
  public void after() {
    if (dbSession != null) {
      dbSession.close();
    }
  }

  private void register(@Nullable Rules rules) {
    if (dbSession != null) {
      dbSession.close();
    }
    RULE_DEFS.set(rules);
    TESTER.get(Platform.class).executeStartupTasks();
    db = TESTER.get(DbClient.class);
    dbSession = TESTER.get(DbClient.class).openSession(false);
    dbSession.clearCache();
    index = TESTER.get(RuleIndex.class);
  }

  @Test
  public void register_rules_at_startup() throws Exception {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc")
          .setSeverity(Severity.MINOR)
          .setEffortToFixDescription("x1 effort to fix")
          .setTags("tag1");
        x1Rule.createParam("acceptWhitespace")
          .setType(RuleParamType.BOOLEAN)
          .setDefaultValue("false")
          .setDescription("Accept whitespaces on the line");
        x1Rule.createParam("min")
          .setType(RuleParamType.INTEGER);
        x1Rule
          .setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY)
          .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
      }
    });

    // verify db
    List<RuleDto> rules = db.ruleDao().findAll(dbSession);
    assertThat(rules).hasSize(1);
    assertThat(rules.get(0).getKey()).isEqualTo(RuleKey.of("xoo", "x1"));
    List<RuleParamDto> ruleParams = db.ruleDao().findAllRuleParams(dbSession);
    assertThat(ruleParams).hasSize(2);

    // verify es
    Result<Rule> searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(1);
    assertThat(searchResult.getHits()).hasSize(1);
    Rule rule = index.getByKey(RuleKey.of("xoo", "x1"));
    assertThat(rule.severity()).isEqualTo(Severity.MINOR);
    assertThat(rule.name()).isEqualTo("x1 name");
    assertThat(rule.htmlDescription()).isEqualTo("x1 desc");
    assertThat(rule.systemTags()).contains("tag1");
    assertThat(rule.params()).hasSize(2);
    assertThat(rule.param("acceptWhitespace").type()).isEqualTo(RuleParamType.BOOLEAN);
    assertThat(rule.param("acceptWhitespace").defaultValue()).isEqualTo("false");
    assertThat(rule.param("acceptWhitespace").description()).isEqualTo("Accept whitespaces on the line");
    assertThat(rule.param("min").type()).isEqualTo(RuleParamType.INTEGER);
    assertThat(rule.param("min").defaultValue()).isNull();
    assertThat(rule.param("min").description()).isNull();
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY);
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(rule.debtRemediationFunction().coefficient()).isEqualTo("1h");
    assertThat(rule.debtRemediationFunction().offset()).isEqualTo("30min");
    assertThat(rule.effortToFixDescription()).isEqualTo("x1 effort to fix");
  }

  /**
   * Use-case:
   * 1. start server
   * 2. stop server
   * 3. drop elasticsearch index: rm -rf data/es
   * 4. start server -> db is up-to-date (no changes) but rules must be re-indexed
   */
  @Test
  public void index_rules_even_if_no_changes() throws Exception {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1")
          .setName("x1 name")
          .setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // clear ES but keep db
    TESTER.clearIndexes();
    register(rules);

    // verify that rules are indexed
    Result<Rule> searchResult = index.search(new RuleQuery(), new QueryOptions());
    searchResult = index.search(new RuleQuery().setKey("xoo:x1"), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(1);
    assertThat(searchResult.getHits()).hasSize(1);
    assertThat(searchResult.getHits().get(0).key()).isEqualTo(RuleKey.of("xoo", "x1"));
  }

  @Test
  public void update_existing_rules() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1")
          .setName("Name1")
          .setHtmlDescription("Desc1")
          .setSeverity(Severity.MINOR)
          .setEffortToFixDescription("Effort1")
          .setTags("tag1", "tag2");
        x1Rule.createParam("max")
          .setType(RuleParamType.INTEGER)
          .setDefaultValue("10")
          .setDescription("Maximum1");
        x1Rule.createParam("min")
          .setType(RuleParamType.INTEGER);
        x1Rule
          .setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY)
          .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
      }
    });

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule(RuleTesting.XOO_X1.rule())
          .setName("Name2")
          .setHtmlDescription("Desc2")
          .setSeverity(Severity.INFO)
          .setEffortToFixDescription("Effort2")
          .setTags("tag2", "tag3");
        // Param "max" is updated, "min" is removed, "format" is added
        x1Rule.createParam("max")
          .setType(RuleParamType.INTEGER)
          .setDefaultValue("15")
          .setDescription("Maximum2");
        x1Rule.createParam("format").setType(RuleParamType.TEXT);
        x1Rule
          .setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY)
          .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linear("2h"));
      }
    });

    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.severity()).isEqualTo(Severity.INFO);
    assertThat(rule.name()).isEqualTo("Name2");
    assertThat(rule.htmlDescription()).isEqualTo("Desc2");
    assertThat(rule.systemTags()).contains("tag2", "tag3");
    assertThat(rule.params()).hasSize(2);
    assertThat(rule.param("max").type()).isEqualTo(RuleParamType.INTEGER);
    assertThat(rule.param("max").defaultValue()).isEqualTo("15");
    assertThat(rule.param("max").description()).isEqualTo("Maximum2");
    assertThat(rule.param("format").type()).isEqualTo(RuleParamType.TEXT);
    assertThat(rule.param("format").defaultValue()).isNull();
    assertThat(rule.param("format").description()).isNull();
    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY);
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(rule.debtRemediationFunction().coefficient()).isEqualTo("2h");
    assertThat(rule.debtRemediationFunction().offset()).isNull();
    assertThat(rule.effortToFixDescription()).isEqualTo("Effort2");
  }

  @Test
  public void do_not_update_rules_if_no_changes() throws Exception {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // Store updated at date
    Date updatedAt = index.getByKey(RuleTesting.XOO_X1).updatedAt();

    // Re-execute startup tasks
    register(rules);

    // Verify rule has not been updated
    Rule customRuleReloaded = index.getByKey(RuleTesting.XOO_X1);
    assertThat(DateUtils.isSameInstant(customRuleReloaded.updatedAt(), updatedAt));
  }

  @Test
  public void uninstall_and_reinstall_rules() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // Uninstall plugin
    register(null);
    RuleDto rule = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
    Rule indexedRule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(indexedRule.status()).isEqualTo(RuleStatus.REMOVED);

    // Re-install plugin
    register(rules);
    rule = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.READY);
    indexedRule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(indexedRule.status()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void deactivate_removed_rules_only_if_repository_still_exists() throws Exception {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    });

    // Create a profile and activate rule
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    TESTER.get(QProfileService.class).activate(QProfileTesting.XOO_P1_KEY, activation);

    // Restart, repo xoo still exists -> deactivate x1
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x2").setName("x2 name").setHtmlDescription("x2 desc");
      }
    });
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x1")).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2")).getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, QProfileTesting.XOO_P1_KEY)).hasSize(0);
  }

  @Test
  public void do_not_deactivate_removed_rules_if_repository_accidentaly_uninstalled() throws Exception {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // create a profile and activate rule
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    TESTER.get(QProfileService.class).activate(QProfileTesting.XOO_P1_KEY, activation);

    // Restart without xoo
    register(null);
    assertThat(db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, QProfileTesting.XOO_P1_KEY)).hasSize(1);

    // Re-install
    register(rules);
    assertThat(db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1).getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, QProfileTesting.XOO_P1_KEY)).hasSize(1);
  }

  @Test
  public void remove_user_tags_that_are_newly_declared_as_system() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc").setTags("tag1");
      }
    });
    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).containsOnly("tag1");
    assertThat(rule.tags()).isEmpty();

    // User adds tag
    TESTER.get(RuleUpdater.class).update(RuleUpdate.createForPluginRule(RuleTesting.XOO_X1).setTags(newHashSet("tag2")), UserSession.get());
    dbSession.clearCache();
    rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).containsOnly("tag1");
    assertThat(rule.tags()).containsOnly("tag2");

    // Definition updated -> user tag "tag2" becomes a system tag
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc").setTags("tag1", "tag2");
      }
    });
    rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).containsOnly("tag1", "tag2");
    assertThat(rule.tags()).isEmpty();
  }

  @Test
  public void fail_if_debt_characteristic_is_root() throws Exception {
    try {
      register(new Rules() {
        @Override
        public void init(RulesDefinition.NewRepository repository) {
          RulesDefinition.NewRule rule = repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
          rule
            .setDebtSubCharacteristic("REUSABILITY")
            .setDebtRemediationFunction(rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
        }
      });
      fail();
    } catch (MessageException e) {
      assertThat(e).hasMessage("Rule 'xoo:x1' cannot be linked on the root characteristic 'REUSABILITY'");
    }
  }

  @Test
  public void update_custom_rule_on_template_change() throws Exception {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    });
    Rule template = index.getByKey(RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", template.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    // Update template and restart
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule rule = repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.BLOCKER)
          .setStatus(RuleStatus.BETA)
          .setTemplate(true)
          .setInternalKey("new_internal");
        rule
          .setDebtSubCharacteristic(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY)
          .setDebtRemediationFunction(rule.debtRemediationFunctions().linearWithOffset("1h", "30min"))
          .setEffortToFixDescription("Effort");
      }
    });

    // Verify custom rule has been restore from the template
    Rule customRule = index.getByKey(customRuleKey);
    assertThat(customRule.language()).isEqualTo("xoo");
    assertThat(customRule.internalKey()).isEqualTo("new_internal");
    assertThat(customRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(customRule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(customRule.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY);
    assertThat(customRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(customRule.effortToFixDescription()).isEqualTo("Effort");
  }

  @Test
  public void do_not_update_custom_rule_if_no_template_change() throws Exception {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    };
    register(rules);
    Rule template = index.getByKey(RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", template.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    Date updatedAt = index.getByKey(customRuleKey).updatedAt();

    register(rules);

    // Verify custom rule has been restore from the template
    Rule customRuleReloaded = index.getByKey(customRuleKey);
    assertThat(customRuleReloaded.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  public void do_not_update_custom_rule_params_from_template() throws Exception {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    });
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
            // "format" removed, "format2" added
          .createParam("format2")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    });

    // Verify custom rule param has not been changed!
    Rule customRuleReloaded = index.getByKey(customRuleKey);
    assertThat(customRuleReloaded.params().get(0).key()).isEqualTo("format");
  }

  @Test
  public void disable_custom_rules_if_template_disabled() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("T1")
          .setName("template1 name")
          .setHtmlDescription("template1 desc")
          .setSeverity(Severity.MAJOR)
          .setTemplate(true)
          .createParam("format")
          .setDefaultValue("csv")
          .setType(RuleParamType.STRING)
          .setDescription("format parameter");
      }
    };
    register(rules);
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart without template
    register(null);

    // Verify custom rule is removed
    assertThat(index.getByKey(templateRule.key()).status()).isEqualTo(RuleStatus.REMOVED);
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.REMOVED);

    // Re-install template
    register(rules);
    assertThat(index.getByKey(templateRule.key()).status()).isEqualTo(RuleStatus.READY);
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void do_not_disable_manual_rules() {
    // Create manual rule
    RuleKey manualRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual")
      .setHtmlDescription("Some description"));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(index.getByKey(manualRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart
    register(null);

    // Verify manual rule is still ready
    assertThat(index.getByKey(manualRuleKey).status()).isEqualTo(RuleStatus.READY);
  }

  interface Rules {
    void init(RulesDefinition.NewRepository repository);
  }

  public static class XooRulesDefinition implements RulesDefinition {
    private Rules rules = null;

    void set(@Nullable Rules rules) {
      this.rules = rules;
    }

    @Override
    public void define(Context context) {
      if (rules != null) {
        NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
        rules.init(repository);
        repository.done();
      }
    }

  }
}
