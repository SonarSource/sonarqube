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
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
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
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.platform.Platform;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileService;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.search.QueryContext;
import org.sonar.server.search.Result;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static com.google.common.collect.Sets.newHashSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RegisterRulesMediumTest {

  static final XooRulesDefinition RULE_DEFS = new XooRulesDefinition();

  @ClassRule
  public static final ServerTester TESTER = new ServerTester().addXoo().addComponents(RULE_DEFS);
  public static final RuleKey X1_KEY = RuleKey.of("xoo", "x1");
  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(TESTER);

  RuleIndex ruleIndex;
  ActiveRuleIndex activeRuleIndex;
  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    TESTER.clearDbAndIndexes();
    db = TESTER.get(DbClient.class);
    dbSession = TESTER.get(DbClient.class).openSession(false);
    dbSession.clearCache();
    ruleIndex = TESTER.get(RuleIndex.class);
    activeRuleIndex = TESTER.get(ActiveRuleIndex.class);
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
    ruleIndex = TESTER.get(RuleIndex.class);
  }

  @Test
  public void register_rules_at_startup() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1")
          .setName("x1 name")
          .setMarkdownDescription("x1 desc")
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

    // verify db : rule x1 + 6 common rules
    List<RuleDto> rules = db.deprecatedRuleDao().selectAll(dbSession);
    assertThat(rules).hasSize(7);
    assertThat(rules).extracting("key").contains(X1_KEY);
    List<RuleParamDto> ruleParams = db.deprecatedRuleDao().selectRuleParamsByRuleKey(dbSession, X1_KEY);
    assertThat(ruleParams).hasSize(2);

    // verify es : rule x1 + 6 common rules
    Result<Rule> searchResult = ruleIndex.search(new RuleQuery(), new QueryContext(userSessionRule));
    assertThat(searchResult.getTotal()).isEqualTo(7);
    assertThat(searchResult.getHits()).hasSize(7);
    Rule rule = ruleIndex.getByKey(X1_KEY);
    assertThat(rule.severity()).isEqualTo(Severity.MINOR);
    assertThat(rule.name()).isEqualTo("x1 name");
    assertThat(rule.htmlDescription()).isEqualTo("x1 desc");
    assertThat(rule.systemTags()).contains("tag1");
    assertThat(rule.language()).contains("xoo");
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
  public void index_rules_even_if_no_changes() {
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
    Result<Rule> searchResult = ruleIndex.search(new RuleQuery(), new QueryContext(userSessionRule));
    searchResult = ruleIndex.search(new RuleQuery().setKey("xoo:x1"), new QueryContext(userSessionRule));
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

    Rule rule = ruleIndex.getByKey(RuleTesting.XOO_X1);
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
  public void update_only_rule_name() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1")
          .setName("Name1")
          .setHtmlDescription("Desc1");
      }
    });

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule(RuleTesting.XOO_X1.rule())
          .setName("Name2")
          .setHtmlDescription("Desc1");
      }
    });

    Rule rule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.name()).isEqualTo("Name2");
    assertThat(rule.htmlDescription()).isEqualTo("Desc1");
  }

  @Test
  public void update_only_rule_description() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1")
          .setName("Name1")
          .setHtmlDescription("Desc1");
      }
    });

    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule(RuleTesting.XOO_X1.rule())
          .setName("Name1")
          .setHtmlDescription("Desc2");
      }
    });

    Rule rule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.name()).isEqualTo("Name1");
    assertThat(rule.htmlDescription()).isEqualTo("Desc2");
  }

  @Test
  public void do_not_update_rules_if_no_changes() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // Store updated at date
    Date updatedAt = ruleIndex.getByKey(RuleTesting.XOO_X1).updatedAt();

    // Re-execute startup tasks
    register(rules);

    // Verify rule has not been updated
    Rule customRuleReloaded = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(DateUtils.isSameInstant(customRuleReloaded.updatedAt(), updatedAt)).isTrue();
  }

  @Test
  public void disable_then_enable_rules() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // Uninstall plugin
    register(null);
    RuleDto rule = db.deprecatedRuleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
    Rule indexedRule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(indexedRule.status()).isEqualTo(RuleStatus.REMOVED);

    // Re-install plugin
    register(rules);
    rule = db.deprecatedRuleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.READY);
    indexedRule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(indexedRule.status()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void deactivate_removed_rules_only_if_repository_still_exists() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    });

    // Create a profile and activate rule
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
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
    assertThat(ruleIndex.getByKey(RuleKey.of("xoo", "x1")).status()).isEqualTo(RuleStatus.REMOVED);
    assertThat(ruleIndex.getByKey(RuleKey.of("xoo", "x2")).status()).isEqualTo(RuleStatus.READY);
    assertThat(activeRuleIndex.findByProfile(QProfileTesting.XOO_P1_KEY)).hasSize(0);
  }

  @Test
  public void do_not_deactivate_removed_rules_if_repository_accidentally_uninstalled() {
    Rules rules = new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
      }
    };
    register(rules);

    // create a profile and activate rule
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    TESTER.get(QProfileService.class).activate(QProfileTesting.XOO_P1_KEY, activation);

    // Restart without xoo
    register(null);
    assertThat(ruleIndex.getByKey(RuleTesting.XOO_X1).status()).isEqualTo(RuleStatus.REMOVED);
    assertThat(activeRuleIndex.findByProfile(QProfileTesting.XOO_P1_KEY)).isEmpty();

    // Re-install
    register(rules);
    assertThat(ruleIndex.getByKey(RuleTesting.XOO_X1).status()).isEqualTo(RuleStatus.READY);
    assertThat(activeRuleIndex.findByProfile(QProfileTesting.XOO_P1_KEY)).hasSize(1);
  }

  @Test
  public void update_active_rules_on_param_changes() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
        // has default value
        x1Rule.createParam("min").setType(RuleParamType.INTEGER).setDefaultValue("5");
        // no default value
        x1Rule.createParam("format").setType(RuleParamType.STRING);
      }
    });

    // Create profile and activate rule
    userSessionRule.login().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN);
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    activation.setParameter("format", "txt");
    TESTER.get(QProfileService.class).activate(QProfileTesting.XOO_P1_KEY, activation);

    // Default value of "min" is changed, "format" is removed, "format2" is added, "max" is added with a default value
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        RulesDefinition.NewRule x1Rule = repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc");
        x1Rule.createParam("min").setType(RuleParamType.INTEGER).setDefaultValue("6");
        x1Rule.createParam("format2").setType(RuleParamType.STRING);
        x1Rule.createParam("max").setType(RuleParamType.INTEGER).setDefaultValue("10");
      }
    });

    ActiveRule activeRule = activeRuleIndex.getByKey(ActiveRuleKey.of(QProfileTesting.XOO_P1_KEY, RuleTesting.XOO_X1));
    Map<String, String> params = activeRule.params();
    assertThat(params).hasSize(2);

    // do not change default value on existing active rules -> keep min=5
    assertThat(params.get("min")).isEqualTo("5");

    // new param with default value
    assertThat(params.get("max")).isEqualTo("10");
  }

  @Test
  public void remove_user_tags_that_are_newly_declared_as_system() {
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc").setTags("tag1");
      }
    });
    Rule rule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).containsOnly("tag1");
    assertThat(rule.tags()).isEmpty();

    // User adds tag
    TESTER.get(RuleUpdater.class).update(RuleUpdate.createForPluginRule(RuleTesting.XOO_X1).setTags(newHashSet("tag2")), userSessionRule);
    dbSession.clearCache();
    rule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).containsOnly("tag1");
    assertThat(rule.tags()).containsOnly("tag2");

    // Definition updated -> user tag "tag2" becomes a system tag
    register(new Rules() {
      @Override
      public void init(RulesDefinition.NewRepository repository) {
        repository.createRule("x1").setName("x1 name").setHtmlDescription("x1 desc").setTags("tag1", "tag2");
      }
    });
    rule = ruleIndex.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).containsOnly("tag1", "tag2");
    assertThat(rule.tags()).isEmpty();
  }

  @Test
  public void fail_if_debt_characteristic_is_root() {
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
  public void update_custom_rule_on_template_change() {
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
    Rule template = ruleIndex.getByKey(RuleKey.of("xoo", "T1"));

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
    Rule customRule = ruleIndex.getByKey(customRuleKey);
    assertThat(customRule.language()).isEqualTo("xoo");
    assertThat(customRule.internalKey()).isEqualTo("new_internal");
    assertThat(customRule.severity()).isEqualTo(Severity.BLOCKER);
    assertThat(customRule.status()).isEqualTo(RuleStatus.BETA);
    assertThat(customRule.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY);
    assertThat(customRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(customRule.effortToFixDescription()).isEqualTo("Effort");
  }

  @Test
  public void do_not_update_custom_rule_if_no_template_change() {
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
    Rule template = ruleIndex.getByKey(RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", template.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    Date updatedAt = ruleIndex.getByKey(customRuleKey).updatedAt();

    register(rules);

    // Verify custom rule has been restore from the template
    Rule customRuleReloaded = ruleIndex.getByKey(customRuleKey);
    assertThat(customRuleReloaded.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  public void do_not_update_custom_rule_params_from_template() {
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
    Rule templateRule = ruleIndex.getByKey(RuleKey.of("xoo", "T1"));

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
    Rule customRuleReloaded = ruleIndex.getByKey(customRuleKey);
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
    Rule templateRule = ruleIndex.getByKey(RuleKey.of("xoo", "T1"));

    // Create custom rule
    RuleKey customRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));
    assertThat(ruleIndex.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart without template
    register(null);

    // Verify custom rule is removed
    assertThat(ruleIndex.getByKey(templateRule.key()).status()).isEqualTo(RuleStatus.REMOVED);
    assertThat(ruleIndex.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.REMOVED);

    // Re-install template
    register(rules);
    assertThat(ruleIndex.getByKey(templateRule.key()).status()).isEqualTo(RuleStatus.READY);
    assertThat(ruleIndex.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void do_not_disable_manual_rules() {
    // Create manual rule
    RuleKey manualRuleKey = TESTER.get(RuleCreator.class).create(NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual")
      .setHtmlDescription("Some description"));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(ruleIndex.getByKey(manualRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart
    register(null);

    // Verify manual rule is still ready
    assertThat(ruleIndex.getByKey(manualRuleKey).status()).isEqualTo(RuleStatus.READY);
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
        NewRepository repository = context.createRepository("xoo", ServerTester.Xoo.KEY).setName("Xoo Repo");
        rules.init(repository);
        repository.done();
      }
    }
  }

}
