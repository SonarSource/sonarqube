/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.server.rule;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.db.DbClient;
import org.sonar.db.DbSession;
import org.sonar.db.organization.OrganizationDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QualityProfileDto;
import org.sonar.db.rule.RuleDao;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.organization.DefaultOrganizationProvider;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.qualityprofile.RuleActivation;
import org.sonar.server.qualityprofile.RuleActivator;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexDefinition;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.ServerTester;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

// TODO remove ServerTester usage when ActiveRule Daov2 is removed
public class RuleUpdaterMediumTest {

  static final RuleKey RULE_KEY = RuleKey.of("squid", "S001");

  @ClassRule
  public static ServerTester tester = new ServerTester().withEsIndexes();

  @org.junit.Rule
  public UserSessionRule userSessionRule = UserSessionRule.forServerTester(tester);

  private DbClient db = tester.get(DbClient.class);
  private RuleDao ruleDao = tester.get(RuleDao.class);
  private DbSession dbSession = db.openSession(false);
  private RuleIndex ruleIndex = tester.get(RuleIndex.class);
  private OrganizationDto defaultOrganization;

  RuleUpdater underTest = tester.get(RuleUpdater.class);

  @Before
  public void before() {
    tester.clearDbAndIndexes();
    String defaultOrganizationUuid = tester.get(DefaultOrganizationProvider.class).get().getUuid();
    defaultOrganization = db.organizationDao().selectByUuid(dbSession, defaultOrganizationUuid).get();
  }

  @After
  public void after() {
    dbSession.close();
  }

  @Test
  public void do_not_update_rule_with_removed_status() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY).setStatus(RuleStatus.REMOVED).getDefinition());
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setTags(Sets.newHashSet("java9"));
    try {
      underTest.update(update, userSessionRule);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Rule with REMOVED status cannot be updated: squid:S001");
    }
  }

  @Test
  public void no_changes() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, defaultOrganization)
      // the following fields are not supposed to be updated
      .setNoteData("my *note*")
      .setNoteUserLogin("me")
      .setTags(ImmutableSet.of("tag1"))
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier("1d")
      .setRemediationBaseEffort("5min");
    ruleDao.insert(dbSession, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(dbSession, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY);
    assertThat(update.isEmpty()).isTrue();
    underTest.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void set_markdown_note() {
    userSessionRule.logIn("me");

    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, defaultOrganization)
      .setNoteData(null)
      .setNoteUserLogin(null)

      // the following fields are not supposed to be updated
      .setTags(ImmutableSet.of("tag1"))
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier("1d")
      .setRemediationBaseEffort("5min");
    ruleDao.insert(dbSession, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(dbSession, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY);
    update.setMarkdownNote("my *note*");
    underTest.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserLogin()).isEqualTo("me");
    assertThat(rule.getNoteCreatedAt()).isNotNull();
    assertThat(rule.getNoteUpdatedAt()).isNotNull();
    // no other changes
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void remove_markdown_note() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, defaultOrganization)
      .setNoteData("my *note*")
      .setNoteUserLogin("me");
    ruleDao.insert(dbSession, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(dbSession, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setMarkdownNote(null);
    underTest.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getNoteData()).isNull();
    assertThat(rule.getNoteUserLogin()).isNull();
    assertThat(rule.getNoteCreatedAt()).isNull();
    assertThat(rule.getNoteUpdatedAt()).isNull();
  }

  @Test
  public void set_tags() {
    // insert db
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY, defaultOrganization)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")).getDefinition());
    dbSession.commit();

    // java8 is a system tag -> ignore
    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setTags(Sets.newHashSet("bug", "java8"));
    underTest.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getTags()).containsOnly("bug");
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    Set<String> tags = tester.get(RuleIndex.class).terms(RuleIndexDefinition.FIELD_RULE_ALL_TAGS);
    assertThat(tags).containsOnly("bug", "java8", "javadoc");
  }

  @Test
  public void remove_tags() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, defaultOrganization)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc"));
    ruleDao.insert(dbSession, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(dbSession, ruleDto.getMetadata());
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setTags(null);
    underTest.update(update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    Set<String> tags = tester.get(RuleIndex.class).terms(RuleIndexDefinition.FIELD_RULE_ALL_TAGS);
    assertThat(tags).containsOnly("java8", "javadoc");
  }

  @Test
  public void override_debt() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min")
      .getDefinition());
    dbSession.commit();

    DefaultDebtRemediationFunction fn = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "1min");
    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(fn);
    underTest.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("1min");

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void override_debt_only_offset() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort(null)
      .getDefinition());
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "2d", null));
    underTest.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("2d");
    assertThat(rule.getRemediationBaseEffort()).isNull();

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isNull();
  }

  @Test
  public void override_debt_from_linear_with_offset_to_constant() {
    ruleDao.insert(dbSession, RuleTesting.newDto(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min")
      .getDefinition());
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "10min"));
    underTest.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("10min");

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void reset_remediation_function() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, defaultOrganization)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min")
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("1min");
    ruleDao.insert(dbSession, ruleDto.getDefinition());
    ruleDao.insertOrUpdate(dbSession, ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = RuleUpdate.createForPluginRule(RULE_KEY).setDebtRemediationFunction(null);
    underTest.update(update, userSessionRule);
    dbSession.clearCache();

    // verify debt is coming from default values
    RuleDto rule = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), RULE_KEY);
    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");

    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isNull();
  }

  @Test
  public void update_custom_rule() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule.getDefinition());
    RuleParamDto templateRuleParam1 = RuleParamDto.createFor(templateRule.getDefinition()).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    RuleParamDto templateRuleParam2 = RuleParamDto.createFor(templateRule.getDefinition()).setName("format").setType("STRING").setDescription("Format");
    ruleDao.insertRuleParam(dbSession, templateRule.getDefinition(), templateRuleParam1);
    ruleDao.insertRuleParam(dbSession, templateRule.getDefinition(), templateRuleParam2);

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .getDefinition();
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
    underTest.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    RuleDto customRuleReloaded = ruleDao.selectOrFailByKey(dbSession, defaultOrganization.getUuid(), customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getName()).isEqualTo("New name");
    assertThat(customRuleReloaded.getDescription()).isEqualTo("New description");
    assertThat(customRuleReloaded.getSeverityString()).isEqualTo("MAJOR");
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.READY);

    List<RuleParamDto> params = ruleDao.selectRuleParamsByRuleKey(dbSession, customRuleReloaded.getKey());
    assertThat(params).hasSize(2);
    assertThat(params.get(0).getDefaultValue()).isEqualTo("b.*");
    assertThat(params.get(1).getDefaultValue()).isNull();

    // Verify in index
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("New name"), new SearchOptions()).getIds()).containsOnly(customRule.getKey());
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("New description"), new SearchOptions()).getIds()).containsOnly(customRule.getKey());

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Old name"), new SearchOptions()).getTotal()).isZero();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Old description"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  public void update_custom_rule_with_empty_parameter() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule.getDefinition());
    RuleParamDto templateRuleParam = RuleParamDto.createFor(templateRule.getDefinition()).setName("regex").setType("STRING").setDescription("Reg ex");
    ruleDao.insertRuleParam(dbSession, templateRule.getDefinition(), templateRuleParam);

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .getDefinition();
    ruleDao.insert(dbSession, customRule);
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam);

    dbSession.commit();

    // Update custom rule without setting a value for the parameter
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY);
    underTest.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    List<RuleParamDto> params = ruleDao.selectRuleParamsByRuleKey(dbSession, customRule.getKey());
    assertThat(params.get(0).getDefaultValue()).isNull();
  }

  @Test
  public void update_active_rule_parameters_when_updating_custom_rule() {
    // Create template rule with 3 parameters
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001")).setLanguage("xoo");
    RuleDefinitionDto templateRuleDefinition = templateRule.getDefinition();
    ruleDao.insert(dbSession, templateRuleDefinition);
    RuleParamDto templateRuleParam1 = RuleParamDto.createFor(templateRuleDefinition).setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*");
    ruleDao.insertRuleParam(dbSession, templateRuleDefinition, templateRuleParam1);
    RuleParamDto templateRuleParam2 = RuleParamDto.createFor(templateRuleDefinition).setName("format").setType("STRING").setDescription("format").setDefaultValue("csv");
    ruleDao.insertRuleParam(dbSession, templateRuleDefinition, templateRuleParam2);
    RuleParamDto templateRuleParam3 = RuleParamDto.createFor(templateRuleDefinition).setName("message").setType("STRING").setDescription("message");
    ruleDao.insertRuleParam(dbSession, templateRuleDefinition, templateRuleParam3);

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule)
      .setSeverity(Severity.MAJOR)
      .setLanguage("xoo")
      .getDefinition();
    ruleDao.insert(dbSession, customRule);
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam1.setDefaultValue("a.*"));
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam2.setDefaultValue("txt"));
    ruleDao.insertRuleParam(dbSession, customRule, templateRuleParam3);

    // Create a quality profile
    QualityProfileDto profileDto = QProfileTesting.newXooP1(defaultOrganization);
    db.qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();

    // Activate the custom rule
    RuleActivation activation = new RuleActivation(customRule.getKey()).setSeverity(Severity.BLOCKER);
    tester.get(RuleActivator.class).activate(dbSession, activation, profileDto);
    dbSession.commit();
    dbSession.clearCache();

    // Update custom rule parameter 'regex', add 'message' and remove 'format'
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setParameters(ImmutableMap.of("regex", "b.*", "message", "a message"));
    underTest.update(update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule parameters has been updated
    List<RuleParamDto> params = ruleDao.selectRuleParamsByRuleKey(dbSession, customRule.getKey());
    assertThat(params).hasSize(3);

    Map<String, RuleParamDto> paramsByKey = paramsByKey(params);
    assertThat(paramsByKey.get("regex")).isNotNull();
    assertThat(paramsByKey.get("regex").getDefaultValue()).isEqualTo("b.*");
    assertThat(paramsByKey.get("message")).isNotNull();
    assertThat(paramsByKey.get("message").getDefaultValue()).isEqualTo("a message");
    assertThat(paramsByKey.get("format")).isNotNull();
    assertThat(paramsByKey.get("format").getDefaultValue()).isNull();

    // Verify that severity has not changed
    ActiveRuleDto activeRuleDto = db.activeRuleDao().selectOrFailByKey(dbSession, ActiveRuleKey.of(profileDto.getKey(), customRule.getKey()));
    assertThat(activeRuleDto.getSeverityString()).isEqualTo(Severity.BLOCKER);

    // Verify active rule parameters has been updated
    List<ActiveRuleParamDto> activeRuleParams = db.activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRuleDto.getId());

    assertThat(activeRuleParams).hasSize(2);
    Map<String, ActiveRuleParamDto> activeRuleParamsByKey = ActiveRuleParamDto.groupByKey(activeRuleParams);
    assertThat(activeRuleParamsByKey.get("regex").getValue()).isEqualTo("b.*");
    assertThat(activeRuleParamsByKey.get("message").getValue()).isEqualTo("a message");
    assertThat(activeRuleParamsByKey.get("format")).isNull();
  }

  @Test
  public void fail_to_update_custom_rule_when_empty_name() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule.getDefinition());

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(dbSession, customRule.getDefinition());

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("")
      .setMarkdownDescription("New desc");
    try {
      underTest.update(update, userSessionRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The name is missing");
    }
  }

  @Test
  public void fail_to_update_custom_rule_when_empty_description() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    ruleDao.insert(dbSession, templateRule.getDefinition());

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    ruleDao.insert(dbSession, customRule.getDefinition());

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = RuleUpdate.createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("");
    try {
      underTest.update(update, userSessionRule);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalArgumentException.class).hasMessage("The description is missing");
    }
  }

  @Test
  public void fail_to_update_plugin_rule_if_name_is_set() {
    // Create rule rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("squid", "S01"));
    ruleDao.insert(dbSession, ruleDto.getDefinition());

    dbSession.commit();

    try {
      // Update rule
      RuleUpdate.createForPluginRule(ruleDto.getKey())
        .setName("New name");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  @Test
  public void fail_to_update_plugin_rule_if_description_is_set() {
    // Create rule rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("squid", "S01"));
    ruleDao.insert(dbSession, ruleDto.getDefinition());

    dbSession.commit();

    try {
      // Update rule
      RuleUpdate.createForPluginRule(ruleDto.getKey())
        .setMarkdownDescription("New description");
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  @Test
  public void fail_to_update_plugin_rule_if_severity_is_set() {
    // Create rule rule
    RuleDto ruleDto = RuleTesting.newDto(RuleKey.of("squid", "S01"));
    ruleDao.insert(dbSession, ruleDto.getDefinition());

    dbSession.commit();

    try {
      // Update rule
      RuleUpdate.createForPluginRule(ruleDto.getKey())
        .setSeverity(Severity.CRITICAL);
      fail();
    } catch (Exception e) {
      assertThat(e).isInstanceOf(IllegalStateException.class).hasMessage("Not a custom rule");
    }
  }

  private static Map<String, RuleParamDto> paramsByKey(List<RuleParamDto> params) {
    return FluentIterable.from(params).uniqueIndex(RuleParamToKey.INSTANCE);
  }

  private enum RuleParamToKey implements Function<RuleParamDto, String> {
    INSTANCE;

    @Override
    public String apply(@Nonnull RuleParamDto input) {
      return input.getName();
    }
  }
}
