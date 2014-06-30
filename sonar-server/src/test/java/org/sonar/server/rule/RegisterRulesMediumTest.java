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

import java.util.Date;
import java.util.List;

import static com.google.common.collect.Sets.newHashSet;
import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;

public class RegisterRulesMediumTest {

  static XooRulesDefinition rulesDefinition = new XooRulesDefinition();

  @ClassRule
  public static ServerTester tester = new ServerTester().addComponents(rulesDefinition);

  RuleIndex index;

  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    rulesDefinition.includeX1 = true;
    rulesDefinition.includeX1bis = false;
    rulesDefinition.includeX2 = true;
    rulesDefinition.includeTemplate1 = true;
    rulesDefinition.includeRuleLinkedToRootCharacteristic = false;
    tester.get(Platform.class).executeStartupTasks();
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
    dbSession.clearCache();

    index = tester.get(RuleIndex.class);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void register_rules_at_startup() throws Exception {
    verifyRulesInDb();

    Result<Rule> searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(3);
    assertThat(searchResult.getHits()).hasSize(3);

    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.severity()).isEqualTo(Severity.MINOR);
    assertThat(rule.name()).isEqualTo("x1 name");
    assertThat(rule.htmlDescription()).isEqualTo("x1 desc");
    assertThat(rule.systemTags()).contains("tag1");

    assertThat(rule.params()).hasSize(1);
    assertThat(rule.param("acceptWhitespace").type()).isEqualTo(RuleParamType.BOOLEAN);
    assertThat(rule.param("acceptWhitespace").defaultValue()).isEqualTo("false");
    assertThat(rule.param("acceptWhitespace").description()).isEqualTo("Accept whitespaces on the line");

    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY);
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(rule.debtRemediationFunction().coefficient()).isEqualTo("1h");
    assertThat(rule.debtRemediationFunction().offset()).isEqualTo("30min");
    assertThat(rule.effortToFixDescription()).isEqualTo("x1 effort to fix");
  }

  /**
   * support the use-case:
   * 1. start server
   * 2. stop server
   * 3. drop elasticsearch index: rm -rf data/es
   * 4. start server -> db is up-to-date (no changes) but rules must be re-indexed
   */
  @Test
  public void index_even_if_no_changes() throws Exception {
    verifyRulesInDb();

    // clear ES but keep db
    tester.clearIndexes();
    verifyRulesInDb();
    Result<Rule> searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(0);
    assertThat(searchResult.getHits()).hasSize(0);

    // db is not updated (same rules) but es must be reindexed
    tester.get(Platform.class).executeStartupTasks();

    index = tester.get(RuleIndex.class);

    verifyRulesInDb();
    searchResult = index.search(new RuleQuery().setKey("xoo:x1"), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(1);
    assertThat(searchResult.getHits()).hasSize(1);
    assertThat(searchResult.getHits().get(0).params()).hasSize(1);
  }

  @Test
  public void update_rule() {
    verifyRulesInDb();

    // The plugin X1 will be updated
    rulesDefinition.includeX1 = false;
    rulesDefinition.includeX1bis = true;
    tester.get(Platform.class).executeStartupTasks();

    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.severity()).isEqualTo(Severity.INFO);
    assertThat(rule.name()).isEqualTo("x1 name updated");
    assertThat(rule.htmlDescription()).isEqualTo("x1 desc updated");
    assertThat(rule.systemTags()).contains("tag1", "tag2");

    assertThat(rule.params()).hasSize(2);

    assertThat(rule.param("acceptWhitespace").type()).isEqualTo(RuleParamType.BOOLEAN);
    assertThat(rule.param("acceptWhitespace").defaultValue()).isEqualTo("true");
    assertThat(rule.param("acceptWhitespace").description()).isEqualTo("Accept whitespaces on the line updated");

    // New parameter
    assertThat(rule.param("format").type()).isEqualTo(RuleParamType.TEXT);
    assertThat(rule.param("format").defaultValue()).isEqualTo("txt");
    assertThat(rule.param("format").description()).isEqualTo("Format");

    assertThat(rule.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INSTRUCTION_RELIABILITY);
    assertThat(rule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(rule.debtRemediationFunction().coefficient()).isEqualTo("2h");
    assertThat(rule.debtRemediationFunction().offset()).isNull();
    assertThat(rule.effortToFixDescription()).isEqualTo("x1 effort to fix updated");
  }

  @Test
  public void not_update_rule_if_no_change() throws Exception {
    // Store updated at date
    Date updatedAt = index.getByKey(RuleTesting.XOO_X1).updatedAt();

    // Re-execute startup tasks
    tester.get(Platform.class).executeStartupTasks();
    dbSession.clearCache();

    // Verify rule has not been updated
    Rule customRuleReloaded = index.getByKey(RuleTesting.XOO_X1);
    assertThat(DateUtils.isSameInstant(customRuleReloaded.updatedAt(), updatedAt));
  }

  @Test
  public void mark_rule_as_removed() throws Exception {
    verifyRulesInDb();

    rulesDefinition.includeX2 = false;
    tester.get(Platform.class).executeStartupTasks();

    verifyRulesInDb();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2"));
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
  }

  @Test
  public void reactivate_disabled_rules() {
    verifyRulesInDb();

    // Disable plugin X1
    rulesDefinition.includeX1 = false;
    tester.get(Platform.class).executeStartupTasks();

    RuleDto rule = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
    dbSession.clearCache();

    // Reactivate plugin X1
    rulesDefinition.includeX1 = true;
    tester.get(Platform.class).executeStartupTasks();

    RuleDto ruleReloaded = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(ruleReloaded.getStatus()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void deactivate_removed_rules_only_if_repository_still_exists() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    // create a profile and activate rule
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    tester.get(QProfileService.class).activate(QProfileTesting.XOO_P1_KEY, activation);
    dbSession.clearCache();

    // restart, x2 still exists -> deactivate x1
    rulesDefinition.includeX1 = false;
    rulesDefinition.includeX2 = true;
    tester.get(Platform.class).executeStartupTasks();
    dbSession.clearCache();
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x1")).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2")).getStatus()).isEqualTo(RuleStatus.READY);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, QProfileTesting.XOO_P1_KEY)).hasSize(0);
  }

  @Test
  public void do_not_deactivate_removed_rules_if_repository_accidentaly_uninstalled() throws Exception {
    MockUserSession.set().setGlobalPermissions(GlobalPermissions.QUALITY_PROFILE_ADMIN).setLogin("me");

    // create a profile and activate rule
    db.qualityProfileDao().insert(dbSession, QProfileTesting.newXooP1());
    dbSession.commit();
    dbSession.clearCache();
    RuleActivation activation = new RuleActivation(RuleTesting.XOO_X1);
    tester.get(QProfileService.class).activate(QProfileTesting.XOO_P1_KEY, activation);
    dbSession.clearCache();

    // restart without x1, x2, template1 -> keep active rule of x1
    rulesDefinition.includeX1 = false;
    rulesDefinition.includeX2 = false;
    rulesDefinition.includeTemplate1 = false;
    tester.get(Platform.class).executeStartupTasks();
    dbSession.clearCache();
    assertThat(db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X2).getStatus()).isEqualTo(RuleStatus.REMOVED);
    assertThat(db.activeRuleDao().findByProfileKey(dbSession, QProfileTesting.XOO_P1_KEY)).hasSize(1);
  }

  @Test
  public void remove_end_user_tags_that_are_declared_as_system() {
    verifyRulesInDb();

    Rule rule = index.getByKey(RuleTesting.XOO_X1);
    assertThat(rule.systemTags()).contains("tag1");
    assertThat(rule.tags()).isEmpty();

    // Add a user tag
    tester.get(RuleUpdater.class).update(RuleUpdate.createForPluginRule(rule.key())
      .setTags(newHashSet("user-tag")),
      UserSession.get());
    dbSession.clearCache();

    // Verify tags
    Rule ruleUpdated = index.getByKey(RuleTesting.XOO_X1);
    assertThat(ruleUpdated.systemTags()).contains("tag1");
    assertThat(ruleUpdated.tags()).contains("user-tag");

    // The plugin X1 will be updated
    rulesDefinition.includeX1 = false;
    rulesDefinition.includeX1bis = true;
    tester.get(Platform.class).executeStartupTasks();
    dbSession.clearCache();

    // User tag should become a system tag
    RuleDto ruleDtoReloaded = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    assertThat(ruleDtoReloaded.getSystemTags()).contains("tag1", "tag2", "user-tag");
    assertThat(ruleDtoReloaded.getTags()).isEmpty();

    // User tag should become a system tag
    Rule ruleReloaded = index.getByKey(RuleTesting.XOO_X1);
    assertThat(ruleReloaded.systemTags()).contains("tag1", "tag2", "user-tag");
    assertThat(ruleReloaded.tags()).isEmpty();
  }

  @Test
  public void update_debt_rule() throws Exception {
    verifyRulesInDb();

    // Update x1 rule
    RuleDto ruleDto = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X1);
    db.ruleDao().update(dbSession, ruleDto
      .setDefaultSubCharacteristicId(123456)
      .setDefaultRemediationFunction("LINEAR_OFFSET")
      .setDefaultRemediationCoefficient("2h")
      .setDefaultRemediationOffset("35min")
      );
    dbSession.commit();
    dbSession.clearCache();

    // Re-execute startup tasks
    tester.get(Platform.class).executeStartupTasks();

    // Verify default debt has been reset to plugin definition
    Rule ruleReloaded = index.getByKey(RuleTesting.XOO_X1);
    assertThat(ruleReloaded.debtSubCharacteristicKey()).isEqualTo(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY);
    assertThat(ruleReloaded.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(ruleReloaded.debtRemediationFunction().coefficient()).isEqualTo("1h");
    assertThat(ruleReloaded.debtRemediationFunction().offset()).isEqualTo("30min");
  }

  @Test
  public void remove_debt_rule() throws Exception {
    verifyRulesInDb();

    // Set some default debt on x2 rule, which has no debt provided by th plugin
    RuleDto ruleDto = db.ruleDao().getByKey(dbSession, RuleTesting.XOO_X2);
    db.ruleDao().update(dbSession, ruleDto
      .setDefaultSubCharacteristicId(db.debtCharacteristicDao().selectByKey(RulesDefinition.SubCharacteristics.INTEGRATION_TESTABILITY, dbSession).getId())
      .setDefaultRemediationFunction("LINEAR_OFFSET")
      .setDefaultRemediationCoefficient("2h")
      .setDefaultRemediationOffset("35min")
      );
    dbSession.commit();
    dbSession.clearCache();

    // Re-execute startup tasks
    tester.get(Platform.class).executeStartupTasks();

    // Verify default debt has been removed
    Rule ruleReloaded = index.getByKey(RuleTesting.XOO_X2);
    assertThat(ruleReloaded.debtSubCharacteristicKey()).isNull();
    assertThat(ruleReloaded.debtRemediationFunction()).isNull();
  }

  @Test
  public void fail_when_rule_is_linked_on_root_characteristic() throws Exception {
    verifyRulesInDb();

    rulesDefinition.includeRuleLinkedToRootCharacteristic = true;
    try {
      // Re-execute startup tasks
      tester.get(Platform.class).executeStartupTasks();
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(MessageException.class).hasMessage("Rule 'xoo:RuleLinkedToRootCharacteristic' cannot be linked on the root characteristic 'REUSABILITY'");
    }
  }

  @Test
  public void update_custom_rule_from_template() throws Exception {
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "template1"));

    // Create custom rule
    RuleKey customRuleKey = tester.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));

    // Update custom rule
    RuleDto customRuleDto = db.ruleDao().getByKey(dbSession, customRuleKey);
    db.ruleDao().update(dbSession, customRuleDto
      .setLanguage("other language")
      .setConfigKey("other config key")
      .setDefaultSubCharacteristicId(45)
      .setDefaultRemediationFunction("LINEAR_OFFSET")
      .setDefaultRemediationCoefficient("1h")
      .setDefaultRemediationOffset("5min")
      .setEffortToFixDescription("effort to fix desc")
      );
    dbSession.commit();
    dbSession.clearCache();

    // Re-execute startup tasks
    tester.get(Platform.class).executeStartupTasks();

    // Verify custom rule has been restore from the template
    Rule customRule = index.getByKey(customRuleKey);
    assertThat(customRule.language()).isEqualTo("xoo");
    assertThat(customRule.internalKey()).isNull();
    assertThat(customRule.debtSubCharacteristicKey()).isNull();
    assertThat(customRule.debtRemediationFunction()).isNull();
  }

  @Test
  public void not_update_custom_rule_from_template_if_no_change() throws Exception {
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "template1"));

    // Create custom rule
    RuleKey customRuleKey = tester.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));
    dbSession.commit();
    dbSession.clearCache();

    // Store updated at date
    Date updatedAt = index.getByKey(customRuleKey).updatedAt();

    // Re-execute startup tasks
    tester.get(Platform.class).executeStartupTasks();

    // Verify custom rule has not been updated
    Rule customRuleReloaded = index.getByKey(customRuleKey);
    assertThat(customRuleReloaded.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  public void not_update_custom_rule_params_from_template() throws Exception {
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "template1"));

    // Create custom rule
    RuleKey customRuleKey = tester.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("format", "txt")));
    dbSession.commit();
    dbSession.clearCache();

    // Update custom rule param name
    RuleDto customRuleDto = db.ruleDao().getByKey(dbSession, customRuleKey);
    RuleParamDto customRuleParamDto = db.ruleDao().findRuleParamsByRuleKey(dbSession, customRuleKey).get(0);
    db.ruleDao().removeRuleParam(dbSession, customRuleDto, customRuleParamDto);
    db.ruleDao().addRuleParam(dbSession, customRuleDto, customRuleParamDto.setName("format2"));
    dbSession.commit();
    dbSession.clearCache();

    // Verify param has been updated
    Rule customRule = index.getByKey(customRuleKey);
    assertThat(customRule.params()).hasSize(1);
    assertThat(customRule.params().get(0).key()).isEqualTo("format2");

    // Re-execute startup tasks
    tester.get(Platform.class).executeStartupTasks();

    // Verify custom rule param has not been changed!
    Rule customRuleReloaded = index.getByKey(customRuleKey);
    assertThat(customRuleReloaded.params().get(0).key()).isEqualTo("format2");
  }

  @Test
  public void disable_custom_rules_if_template_is_disabled() {
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "template1"));

    // Create custom rule
    RuleKey customRuleKey = tester.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart without template rule
    rulesDefinition.includeTemplate1 = false;
    tester.get(Platform.class).executeStartupTasks();

    // Verify custom rule is removed
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.REMOVED);
  }

  @Test
  public void reactivate_disabled_custom_rules() {
    Rule templateRule = index.getByKey(RuleKey.of("xoo", "template1"));

    // Create custom rule
    RuleKey customRuleKey = tester.get(RuleCreator.class).create(NewRule.createForCustomRule("CUSTOM_RULE", templateRule.key())
      .setName("My custom")
      .setHtmlDescription("Some description")
      .setSeverity(Severity.MAJOR)
      .setStatus(RuleStatus.READY));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart without template rule
    rulesDefinition.includeTemplate1 = false;
    tester.get(Platform.class).executeStartupTasks();
    dbSession.clearCache();


    // Verify custom rule is removed
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.REMOVED);

    // Restart with template rule
    rulesDefinition.includeTemplate1 = true;
    tester.get(Platform.class).executeStartupTasks();
    dbSession.clearCache();

    // Verify custom rule is reactivate
    assertThat(index.getByKey(customRuleKey).status()).isEqualTo(RuleStatus.READY);
  }

  @Test
  public void not_disable_manual_rules() {
    // Create manual rule
    RuleKey manualRuleKey = tester.get(RuleCreator.class).create(NewRule.createForManualRule("MANUAL_RULE")
      .setName("My manual")
      .setHtmlDescription("Some description"));
    dbSession.commit();
    dbSession.clearCache();
    assertThat(index.getByKey(manualRuleKey).status()).isEqualTo(RuleStatus.READY);

    // Restart
    tester.get(Platform.class).executeStartupTasks();

    // Verify manual rule is still ready
    assertThat(index.getByKey(manualRuleKey).status()).isEqualTo(RuleStatus.READY);
  }

  private void verifyRulesInDb() {
    List<RuleDto> rules = db.ruleDao().findAll(dbSession);
    assertThat(rules).hasSize(3);
    List<RuleParamDto> ruleParams = db.ruleDao().findAllRuleParams(dbSession);
    assertThat(ruleParams).hasSize(2);
  }

  public static class XooRulesDefinition implements RulesDefinition {

    boolean includeX1 = true, includeX1bis = false, includeX2 = true, includeTemplate1 = true, includeRuleLinkedToRootCharacteristic = false;

    @Override
    public void define(Context context) {
      if (includeX1 || includeX1bis || includeX2 || includeTemplate1 || includeRuleLinkedToRootCharacteristic) {
        NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
        if (includeX1) {
          NewRule x1Rule = repository.createRule(RuleTesting.XOO_X1.rule())
            .setName("x1 name")
            .setHtmlDescription("x1 desc")
            .setSeverity(Severity.MINOR)
            .setEffortToFixDescription("x1 effort to fix")
            .setTags("tag1");
          x1Rule.createParam("acceptWhitespace")
            .setType(RuleParamType.BOOLEAN)
            .setDefaultValue("false")
            .setDescription("Accept whitespaces on the line");
          x1Rule
            .setDebtSubCharacteristic(SubCharacteristics.INTEGRATION_TESTABILITY)
            .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
        }

        // X1 having fields updated to simulate an update from the plugin
        if (includeX1bis) {
          NewRule x1Rule = repository.createRule(RuleTesting.XOO_X1.rule())
            .setName("x1 name updated")
            .setHtmlDescription("x1 desc updated")
            .setSeverity(Severity.INFO)
            .setEffortToFixDescription("x1 effort to fix updated")
            .setTags("tag1", "tag2", "user-tag");
          x1Rule.createParam("acceptWhitespace")
            .setType(RuleParamType.BOOLEAN)
            .setDefaultValue("true")
            .setDescription("Accept whitespaces on the line updated");
          // New param
          x1Rule.createParam("format")
            .setType(RuleParamType.TEXT)
            .setDefaultValue("txt")
            .setDescription("Format");
          x1Rule
            .setDebtSubCharacteristic(SubCharacteristics.INSTRUCTION_RELIABILITY)
            .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linear("2h"));
        }

        if (includeX2) {
          repository.createRule(RuleTesting.XOO_X2.rule())
            .setName("x2 name")
            .setHtmlDescription("x2 desc")
            .setSeverity(Severity.MAJOR);
        }

        if (includeRuleLinkedToRootCharacteristic) {
          NewRule x1Rule = repository.createRule("RuleLinkedToRootCharacteristic")
            .setName("RuleLinkedToRootCharacteristic name")
            .setHtmlDescription("RuleLinkedToRootCharacteristic desc")
            .setSeverity(Severity.MINOR);
          x1Rule
            // Link to a root characteristic -> fail
            .setDebtSubCharacteristic("REUSABILITY")
            .setDebtRemediationFunction(x1Rule.debtRemediationFunctions().linearWithOffset("1h", "30min"));
        }

        if (includeTemplate1) {
          repository.createRule("template1")
            .setName("template1 name")
            .setHtmlDescription("template1 desc")
            .setSeverity(Severity.MAJOR)
            .setTemplate(true)
            .createParam("format")
            .setDefaultValue("csv")
            .setType(RuleParamType.STRING)
            .setDescription("format parameter");
        }

        repository.done();
      }
    }
  }
}
