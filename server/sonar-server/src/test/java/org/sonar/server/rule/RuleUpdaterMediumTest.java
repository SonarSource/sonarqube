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
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.db.DbSession;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.debt.CharacteristicDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.db.DbClient;
import org.sonar.server.debt.DebtTesting;
import org.sonar.server.qualityprofile.ActiveRule;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.qualityprofile.index.ActiveRuleIndex;
import org.sonar.server.rule.db.RuleDao;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.search.BaseIndex;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuleUpdaterMediumTest {

  private static final RuleKey RULE_KEY = RuleKey.of("squid", "S001");

  @ClassRule
  public static ServerTester tester = new ServerTester();
  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  DbClient db = tester.get(DbClient.class);
  RuleDao ruleDao = tester.get(RuleDao.class);
  DbSession dbSession;
  BaseIndex<Rule, RuleDto, RuleKey> ruleIndex = tester.get(RuleIndex.class);
  RuleUpdater updater = tester.get(RuleUpdater.class);
  int softReliabilityId;
  int hardReliabilityId;

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    dbSession = db.openSession(false);
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void do_not_update_rule_with_removed_status() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY).setStatus(RuleStatus.REMOVED));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setTags(Sets.newHashSet("java9"));
    try {
      updater.update(update, userSessionRule);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule with REMOVED status cannot be updated: squid:S001");
    }
  }

  @Test
  public void no_changes() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      // the following fields are not supposed to be updated
      .setNoteData("my *note*")
      .setNoteUserLogin("me")
      .setTags(ImmutableSet.of("tag1"))
      .setSubCharacteristicId(33)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient("1d")
      .setRemediationOffset("5min"));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY);
    assertThat(update.isEmpty()).isTrue();
    updater.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.getNullableByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getSubCharacteristicId()).isEqualTo(33);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationCoefficient()).isEqualTo("1d");
    assertThat(rule.getRemediationOffset()).isEqualTo("5min");
  }

  @Test
  public void set_markdown_note() {
    userSessionRule.login("me");

    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setNoteData(null)
      .setNoteUserLogin(null)

      // the following fields are not supposed to be updated
      .setTags(ImmutableSet.of("tag1"))
      .setSubCharacteristicId(33)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient("1d")
      .setRemediationOffset("5min"));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY);
    update.setMarkdownNote("my *note*");
    updater.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.getNullableByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
    assertThat(rule.getNoteCreatedAt()).isNotNull();
    assertThat(rule.getNoteUpdatedAt()).isNotNull();
    // no other changes
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getSubCharacteristicId()).isEqualTo(33);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationCoefficient()).isEqualTo("1d");
    assertThat(rule.getRemediationOffset()).isEqualTo("5min");
  }

  @Test
  public void remove_markdown_note() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setNoteData("my *note*")
      .setNoteUserLogin("me"));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setMarkdownNote(null);
    updater.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.getNullableByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isNull();
    assertThat(rule.getNoteUserLogin()).isNull();
    assertThat(rule.getNoteCreatedAt()).isNull();
    assertThat(rule.getNoteUpdatedAt()).isNull();
  }

  @Test
  public void set_tags() {
    // insert db
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")));
    dbSession.commit();

    // java8 is a system tag -> ignore
    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setTags(Sets.newHashSet("bug", "java8"));
    updater.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.getNullableByKey(dbSession, RULE_KEY);
    assertThat(rule.getTags()).containsOnly("bug");
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    Set<String> tags = tester.get(RuleService.class).listTags();
    assertThat(tags).containsOnly("bug", "java8", "javadoc");
  }

  @Test
  public void remove_tags() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setTags(null);
    updater.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.getNullableByKey(dbSession, RULE_KEY);
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    Set<String> tags = tester.get(RuleService.class).listTags();
    assertThat(tags).containsOnly("java8", "javadoc");
  }

  @Test
  public void override_debt() {
    insertDebtCharacteristics(dbSession);
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setSubCharacteristicId(null)
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null));
    dbSession.commit();

    DefaultDebtRemediationFunction fn = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "1min");
    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtSubCharacteristic("SOFT_RELIABILITY")
      .setDebtRemediationFunction(fn);
    updater.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("SOFT_RELIABILITY");
    assertThat(indexedRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(indexedRule.debtRemediationFunction().coefficient()).isNull();
    assertThat(indexedRule.debtRemediationFunction().offset()).isEqualTo("1min");

    assertThat(indexedRule.debtOverloaded()).isTrue();
    assertThat(indexedRule.defaultDebtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.defaultDebtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.defaultDebtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(indexedRule.defaultDebtRemediationFunction().coefficient()).isEqualTo("1d");
    assertThat(indexedRule.defaultDebtRemediationFunction().offset()).isEqualTo("5min");
  }

  @Test
  public void override_debt_only_offset() {
    insertDebtCharacteristics(dbSession);
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset(null)
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "2d", null));
    updater.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(indexedRule.debtRemediationFunction().coefficient()).isEqualTo("2d");
    assertThat(indexedRule.debtRemediationFunction().offset()).isNull();

    assertThat(indexedRule.debtOverloaded()).isTrue();
    assertThat(indexedRule.defaultDebtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.defaultDebtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.defaultDebtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(indexedRule.defaultDebtRemediationFunction().coefficient()).isEqualTo("1d");
    assertThat(indexedRule.defaultDebtRemediationFunction().offset()).isNull();
  }

  @Test
  public void override_debt_from_linear_with_offset_to_constant() {
    insertDebtCharacteristics(dbSession);
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setRemediationFunction(null)
      .setRemediationCoefficient(null)
      .setRemediationOffset(null));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "10min"));
    updater.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE);
    assertThat(indexedRule.debtRemediationFunction().coefficient()).isNull();
    assertThat(indexedRule.debtRemediationFunction().offset()).isEqualTo("10min");

    assertThat(indexedRule.debtOverloaded()).isTrue();
    assertThat(indexedRule.defaultDebtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.defaultDebtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.defaultDebtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET);
    assertThat(indexedRule.defaultDebtRemediationFunction().coefficient()).isEqualTo("1d");
    assertThat(indexedRule.defaultDebtRemediationFunction().offset()).isEqualTo("5min");
  }

  @Test
  public void reset_debt() {
    insertDebtCharacteristics(dbSession);
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient(null)
      .setRemediationOffset("1min"));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtSubCharacteristic(RuleUpdate.DEFAULT_DEBT_CHARACTERISTIC);
    updater.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is coming from default values
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.debtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(indexedRule.debtRemediationFunction().coefficient()).isEqualTo("1d");
    assertThat(indexedRule.debtRemediationFunction().offset()).isEqualTo("5min");

    assertThat(indexedRule.debtOverloaded()).isFalse();
    assertThat(indexedRule.defaultDebtCharacteristicKey()).isEqualTo("RELIABILITY");
    assertThat(indexedRule.defaultDebtSubCharacteristicKey()).isEqualTo("HARD_RELIABILITY");
    assertThat(indexedRule.defaultDebtRemediationFunction().type()).isEqualTo(DebtRemediationFunction.Type.LINEAR);
    assertThat(indexedRule.defaultDebtRemediationFunction().coefficient()).isEqualTo("1d");
    assertThat(indexedRule.defaultDebtRemediationFunction().offset()).isEqualTo("5min");
  }

  @Test
  public void unset_debt() {
    insertDebtCharacteristics(dbSession);
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefaultSubCharacteristicId(hardReliabilityId)
      .setDefaultRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefaultRemediationCoefficient("1d")
      .setDefaultRemediationOffset("5min")
      .setSubCharacteristicId(softReliabilityId)
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationCoefficient(null)
      .setRemediationOffset("1min"));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtSubCharacteristic(null);
    updater.update(update, userSessionRule);

    // verify db
    dbSession.clearCache();
    RuleDto rule = ruleDao.getNullableByKey(dbSession, RULE_KEY);
    assertThat(rule.getSubCharacteristicId()).isEqualTo(-1);
    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationCoefficient()).isNull();
    assertThat(rule.getRemediationOffset()).isNull();

    assertThat(rule.getDefaultSubCharacteristicId()).isNotNull();
    assertThat(rule.getDefaultRemediationFunction()).isNotNull();
    assertThat(rule.getDefaultRemediationCoefficient()).isNotNull();
    assertThat(rule.getDefaultRemediationOffset()).isNotNull();

    // verify index
    Rule indexedRule = tester.get(RuleIndex.class).getByKey(RULE_KEY);
    assertThat(indexedRule.debtCharacteristicKey()).isEqualTo("NONE");
    assertThat(indexedRule.debtSubCharacteristicKey()).isEqualTo("NONE");
    assertThat(indexedRule.debtRemediationFunction()).isNull();
    assertThat(indexedRule.debtOverloaded()).isTrue();
  }

  @Test
  public void update_custom_rule() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule);
    RuleParamDto templateRuleParam1 = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    RuleParamDto templateRuleParam2 = RuleParamDto.createFor(templateRule).setName("format").setType("STRING").setDescription("Format");
    ruleDao.insertRuleParam(dbSession, templateRule, templateRuleParam1);
    ruleDao.insertRuleParam(dbSession, templateRule, templateRuleParam2);

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA);
    ruleDao.insert(dbSession, customRule);
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam1.setDefaultValue("a.*"));
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam2.setDefaultValue(null));

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "b.*"));
    updater.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    Rule customRuleReloaded = ruleIndex.getByKey(customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.name()).isEqualTo("New name");
    assertThat(customRuleReloaded.htmlDescription()).isEqualTo("New description");
    assertThat(customRuleReloaded.severity()).isEqualTo("MAJOR");
    assertThat(customRuleReloaded.status()).isEqualTo(RuleStatus.READY);
    assertThat(customRuleReloaded.params()).hasSize(2);

    assertThat(customRuleReloaded.params().get(0).defaultValue()).isEqualTo("b.*");
    assertThat(customRuleReloaded.params().get(1).defaultValue()).isNull();
  }

  @Test
  public void update_custom_rule_with_empty_parameter() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule);
    RuleParamDto templateRuleParam = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex");
    ruleDao.insertRuleParam(dbSession, templateRule, templateRuleParam);

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA);
    ruleDao.insert(dbSession, customRule);
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam);

    dbSession.commit();

    // Update custom rule without setting a value for the parameter
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY);
    updater.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    Rule customRuleReloaded = ruleIndex.getByKey(customRule.getKey());
    RuleParam param = customRuleReloaded.params().get(0);
    assertThat(param.defaultValue()).isNull();
  }

  @Test
  public void update_active_rule_parameters_when_updating_custom_rule() {
    // Create template rule with 3 parameters
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001")).setLanguage("xoo");
    ruleDao.insert(dbSession, templateRule);
    RuleParamDto templateRuleParam1 = RuleParamDto.createFor(templateRule).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    ruleDao.insertRuleParam(dbSession, templateRule, templateRuleParam1);
    RuleParamDto templateRuleParam2 = RuleParamDto.createFor(templateRule).setName("format").setType("STRING").setDescription("format").setDefaultValue("csv");
    ruleDao.insertRuleParam(dbSession, templateRule, templateRuleParam2);
    RuleParamDto templateRuleParam3 = RuleParamDto.createFor(templateRule).setName("message").setType("STRING").setDescription("message");
    ruleDao.insertRuleParam(dbSession, templateRule, templateRuleParam3);

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule).setSeverity(Severity.MAJOR).setLanguage("xoo");
    ruleDao.insert(dbSession, customRule);
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam1.setDefaultValue("a.*"));
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam2.setDefaultValue("txt"));
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam3);

    // Create a quality profile
    QualityProfileDto profileDto = QProfileTesting.newXooP1();
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();

    // Activate the custom rule
    RuleActivation activation = new RuleActivation(customRule.getKey()).setSeverity(Severity.BLOCKER);
    tester.get(RuleActivator.class).activate(dbSession, activation, QProfileTesting.XOO_P1_NAME);
    dbSession.commit();
    dbSession.clearCache();

    // Update custom rule parameter 'regex', add 'message' and remove 'format'
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setParameters(ImmutableMap.of("regex", "b.*", "message", "a message"));
    updater.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule parameters has been updated
    Rule customRuleReloaded = ruleIndex.getByKey(customRule.getKey());
    assertThat(customRuleReloaded.params()).hasSize(3);
    assertThat(customRuleReloaded.param("regex")).isNotNull();
    assertThat(customRuleReloaded.param("regex").defaultValue()).isEqualTo("b.*");
    assertThat(customRuleReloaded.param("message")).isNotNull();
    assertThat(customRuleReloaded.param("message").defaultValue()).isEqualTo("a message");
    assertThat(customRuleReloaded.param("format")).isNotNull();
    assertThat(customRuleReloaded.param("format").defaultValue()).isNull();

    RuleParam param = customRuleReloaded.params().get(0);
    assertThat(param.defaultValue()).isEqualTo("b.*");

    // Verify active rule parameters has been updated
    ActiveRule activeRule = tester.get(ActiveRuleIndex.class).getByKey(ActiveRuleKey.of(profileDto.getKey(), customRule.getKey()));
    assertThat(activeRule.params()).hasSize(2);
    assertThat(activeRule.params().get("regex")).isEqualTo("b.*");
    assertThat(activeRule.params().get("message")).isEqualTo("a message");
    assertThat(activeRule.params().get("format")).isNull();

    // Verify that severity has not changed
    assertThat(activeRule.severity()).isEqualTo(Severity.BLOCKER);
  }

  @Test
  public void fail_to_update_custom_rule_when_empty_name() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule);

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(dbSession, customRule);

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("")
      .setMarkdownDescription("New desc");
    try {
      updater.update(update, userSessionRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The name is missing");
    }
  }

  @Test
  public void fail_to_update_custom_rule_when_empty_description() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule);

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(dbSession, customRule);

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("");
    try {
      updater.update(update, userSessionRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The description is missing");
    }
  }

  @Test
  public void update_manual_rule() {
    // Create manual rule
    RuleDto manualRule = RuleTesting.newManualRule("My manual")
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.INFO);
    ruleDao.insert(dbSession, manualRule);

    dbSession.commit();

    // Update manual rule
    RuleUpdate update = RuleUpdate.createForManualRule(manualRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity(Severity.CRITICAL);
    updater.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify manual rule is updated
    Rule manualRuleReloaded = ruleIndex.getByKey(manualRule.getKey());
    assertThat(manualRuleReloaded).isNotNull();
    assertThat(manualRuleReloaded.name()).isEqualTo("New name");
    assertThat(manualRuleReloaded.htmlDescription()).isEqualTo("New description");
    assertThat(manualRuleReloaded.severity()).isEqualTo(Severity.CRITICAL);
  }

  @Test
  public void fail_to_update_manual_rule_if_status_is_set() {
    // Create manual rule
    RuleDto manualRule = RuleTesting.newManualRule("My manual");
    ruleDao.insert(dbSession, manualRule);

    dbSession.commit();

    try {
      // Update manual rule
      RuleUpdate.createForManualRule(manualRule.getKey())
        .setStatus(RuleStatus.BETA);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  @Test
  public void fail_to_update_manual_rule_if_parameters_are_set() {
    // Create manual rule
    RuleDto manualRule = RuleTesting.newManualRule("My manual");
    ruleDao.insert(dbSession, manualRule);

    dbSession.commit();

    try {
      // Update manual rule
      RuleUpdate.createForManualRule(manualRule.getKey())
        .setStatus(RuleStatus.BETA)
        .setParameters(ImmutableMap.of("regex", "b.*", "message", "a message"));
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  @Test
  public void fail_to_update_plugin_rule_if_name_is_set() {
    // Create rule rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("squid", "S01"));
    ruleDao.insert(dbSession, ruleDto);

    dbSession.commit();

    try {
      // Update rule
      RuleUpdate.createForPluginRule(ruleDto.getKey())
        .setName("New name");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom or a manual rule");
    }
  }

  @Test
  public void fail_to_update_plugin_rule_if_description_is_set() {
    // Create rule rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("squid", "S01"));
    ruleDao.insert(dbSession, ruleDto);

    dbSession.commit();

    try {
      // Update rule
      RuleUpdate.createForPluginRule(ruleDto.getKey())
        .setMarkdownDescription("New description");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom or a manual rule");
    }
  }

  @Test
  public void fail_to_update_plugin_rule_if_severity_is_set() {
    // Create rule rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("squid", "S01"));
    ruleDao.insert(dbSession, ruleDto);

    dbSession.commit();

    try {
      // Update rule
      RuleUpdate.createForPluginRule(ruleDto.getKey())
        .setSeverity(Severity.CRITICAL);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom or a manual rule");
    }
  }

  private void insertDebtCharacteristics(DbSession dbSession) {
    CharacteristicDto reliability = DebtTesting.newCharacteristicDto("RELIABILITY");
    db.debtCharacteristicDao().insert(dbSession, reliability);

    CharacteristicDto softReliability = DebtTesting.newCharacteristicDto("SOFT_RELIABILITY")
      .setParentId(reliability.getId());
    db.debtCharacteristicDao().insert(dbSession, softReliability);
    softReliabilityId = softReliability.getId();

    CharacteristicDto hardReliability = DebtTesting.newCharacteristicDto("HARD_RELIABILITY")
      .setParentId(reliability.getId());
    db.debtCharacteristicDao().insert(dbSession, hardReliability);
    hardReliabilityId = hardReliability.getId();
  }
}
