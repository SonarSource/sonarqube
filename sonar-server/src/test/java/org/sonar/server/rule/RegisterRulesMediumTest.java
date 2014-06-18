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
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.rule.RuleParamType;
import org.sonar.api.server.rule.RulesDefinition;
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

import java.util.Date;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class RegisterRulesMediumTest {

  static XooRulesDefinition rulesDefinition = new XooRulesDefinition();

  @ClassRule
  public static ServerTester tester = new ServerTester().addComponents(rulesDefinition);

  DbClient db;
  DbSession dbSession;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    rulesDefinition.includeX1 = true;
    rulesDefinition.includeX2 = true;
    rulesDefinition.includeTemplate1 = true;
    tester.get(Platform.class).executeStartupTasks();
    db = tester.get(DbClient.class);
    dbSession = tester.get(DbClient.class).openSession(false);
    dbSession.clearCache();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void register_rules_at_startup() throws Exception {
    verifyRulesInDb();

    RuleIndex index = tester.get(RuleIndex.class);

    Result<Rule> searchResult = index.search(new RuleQuery(), new QueryOptions());
    assertThat(searchResult.getTotal()).isEqualTo(3);
    assertThat(searchResult.getHits()).hasSize(3);
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
    RuleIndex index = tester.get(RuleIndex.class);

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
  public void mark_rule_as_removed() throws Exception {
    verifyRulesInDb();

    rulesDefinition.includeX2 = false;
    tester.get(Platform.class).executeStartupTasks();

    verifyRulesInDb();
    RuleDto rule = db.ruleDao().getByKey(dbSession, RuleKey.of("xoo", "x2"));
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.REMOVED);
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
  public void update_custom_rule_from_template() throws Exception {
    RuleIndex index = tester.get(RuleIndex.class);
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
    RuleIndex index = tester.get(RuleIndex.class);
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
    RuleIndex index = tester.get(RuleIndex.class);
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

  private void verifyRulesInDb() {
    List<RuleDto> rules = db.ruleDao().findAll(dbSession);
    assertThat(rules).hasSize(3);
    List<RuleParamDto> ruleParams = db.ruleDao().findAllRuleParams(dbSession);
    assertThat(ruleParams).hasSize(2);
  }

  public static class XooRulesDefinition implements RulesDefinition {

    boolean includeX1 = true, includeX2 = true, includeTemplate1 = true;

    @Override
    public void define(Context context) {
      if (includeX1 || includeX2 || includeTemplate1) {
        NewRepository repository = context.createRepository("xoo", "xoo").setName("Xoo Repo");
        if (includeX1) {
          repository.createRule("x1")
            .setName("x1 name")
            .setHtmlDescription("x1 desc")
            .setSeverity(Severity.MINOR)
            .createParam("acceptWhitespace")
            .setDefaultValue("false")
            .setType(RuleParamType.BOOLEAN)
            .setDescription("Accept whitespaces on the line");
        }

        if (includeX2) {
          repository.createRule("x2")
            .setName("x2 name")
            .setHtmlDescription("x2 desc")
            .setSeverity(Severity.MAJOR);
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
