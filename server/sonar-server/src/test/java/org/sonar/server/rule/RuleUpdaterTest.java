/*
 * SonarQube
 * Copyright (C) 2009-2019 SonarSource SA
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
import javax.annotation.Nonnull;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.rule.RuleDefinitionDto;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.qualityprofile.QProfileTesting;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.server.rule.RuleUpdate.createForCustomRule;
import static org.sonar.server.rule.RuleUpdate.createForPluginRule;

public class RuleUpdaterTest {

  static final RuleKey RULE_KEY = RuleKey.of("squid", "S001");

  private System2 system2 = mock(System2.class);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public UserSessionRule userSessionRule = UserSessionRule.standalone();

  @Rule
  public DbTester db = DbTester.create(system2);

  @Rule
  public EsTester es = EsTester.create();

  private RuleIndex ruleIndex = new RuleIndex(es.client(), system2);
  private RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private DbSession dbSession = db.getSession();

  private RuleUpdater underTest = new RuleUpdater(db.getDbClient(), ruleIndexer, system2);

  @Test
  public void do_not_update_rule_with_removed_status() {
    db.rules().insert(newRule(RULE_KEY).setStatus(RuleStatus.REMOVED));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setTags(Sets.newHashSet("java9"))
      .setOrganization(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Rule with REMOVED status cannot be updated: squid:S001");

    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);
  }

  @Test
  public void no_changes() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, db.getDefaultOrganization())
      // the following fields are not supposed to be updated
      .setNoteData("my *note*")
      .setNoteUserUuid("me")
      .setTags(ImmutableSet.of("tag1"))
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier("1d")
      .setRemediationBaseEffort("5min");
    db.rules().insert(ruleDto.getDefinition());
    db.rules().insertOrUpdateMetadata(ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY);
    assertThat(update.isEmpty()).isTrue();
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserUuid()).isEqualTo("me");
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void set_markdown_note() {
    UserDto user = db.users().insertUser();
    userSessionRule.logIn(user);

    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, db.getDefaultOrganization())
      .setNoteData(null)
      .setNoteUserUuid(null)

      // the following fields are not supposed to be updated
      .setTags(ImmutableSet.of("tag1"))
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier("1d")
      .setRemediationBaseEffort("5min");
    db.rules().insert(ruleDto.getDefinition());
    db.rules().insertOrUpdateMetadata(ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setMarkdownNote("my *note*")
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserUuid()).isEqualTo(user.getUuid());
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
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, db.getDefaultOrganization())
      .setNoteData("my *note*")
      .setNoteUserUuid("me");
    db.rules().insert(ruleDto.getDefinition());
    db.rules().insertOrUpdateMetadata(ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setMarkdownNote(null)
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getNoteData()).isNull();
    assertThat(rule.getNoteUserUuid()).isNull();
    assertThat(rule.getNoteCreatedAt()).isNull();
    assertThat(rule.getNoteUpdatedAt()).isNull();
  }

  @Test
  public void set_tags() {
    // insert db
    db.rules().insert(RuleTesting.newDto(RULE_KEY, db.getDefaultOrganization())
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")).getDefinition());
    dbSession.commit();

    // java8 is a system tag -> ignore
    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setTags(Sets.newHashSet("bug", "java8"))
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getTags()).containsOnly("bug");
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    List<String> tags = ruleIndex.listTags(db.getDefaultOrganization(), null, 10);
    assertThat(tags).containsExactly("bug", "java8", "javadoc");
  }

  @Test
  public void remove_tags() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, db.getDefaultOrganization())
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc"));
    db.rules().insert(ruleDto.getDefinition());
    db.rules().insertOrUpdateMetadata(ruleDto.getMetadata());
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setTags(null)
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    List<String> tags = ruleIndex.listTags(db.getDefaultOrganization(), null, 10);
    assertThat(tags).containsExactly("java8", "javadoc");
  }

  @Test
  public void override_debt() {
    db.rules().insert(newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min"));
    dbSession.commit();

    DefaultDebtRemediationFunction fn = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "1min");
    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(fn)
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("1min");

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void override_debt_only_offset() {
    db.rules().insert(newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort(null));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "2d", null))
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("2d");
    assertThat(rule.getRemediationBaseEffort()).isNull();

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isNull();
  }

  @Test
  public void override_debt_from_linear_with_offset_to_constant() {
    db.rules().insert(newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min"));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "10min"))
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("10min");

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  public void reset_remediation_function() {
    RuleDto ruleDto = RuleTesting.newDto(RULE_KEY, db.getDefaultOrganization())
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min")
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("1min");
    db.rules().insert(ruleDto.getDefinition());
    db.rules().insertOrUpdateMetadata(ruleDto.getMetadata().setRuleId(ruleDto.getId()));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(null)
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);
    dbSession.clearCache();

    // verify debt is coming from default values
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), RULE_KEY);
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
    db.rules().insert(templateRule.getDefinition());
    db.rules().insertRuleParam(templateRule.getDefinition(), param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));
    db.rules().insertRuleParam(templateRule.getDefinition(), param -> param.setName("format").setType("STRING").setDescription("Format"));

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .getDefinition();
    db.rules().insert(customRule);
    db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue("a.*"));
    db.rules().insertRuleParam(customRule, param -> param.setName("format").setType("STRING").setDescription("Format").setDefaultValue(null));

    // Update custom rule
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "b.*"))
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    RuleDto customRuleReloaded = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, db.getDefaultOrganization(), customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getName()).isEqualTo("New name");
    assertThat(customRuleReloaded.getDescription()).isEqualTo("New description");
    assertThat(customRuleReloaded.getSeverityString()).isEqualTo("MAJOR");
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.READY);

    List<RuleParamDto> params = db.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleReloaded.getKey());
    assertThat(params).extracting(RuleParamDto::getDefaultValue).containsOnly("b.*", null);

    // Verify in index
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("New name"), new SearchOptions()).getIds()).containsOnly(customRule.getId());
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("New description"), new SearchOptions()).getIds()).containsOnly(customRule.getId());

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Old name"), new SearchOptions()).getTotal()).isZero();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Old description"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  public void update_custom_rule_with_empty_parameter() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule.getDefinition());
    db.rules().insertRuleParam(templateRule.getDefinition(), param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(null));

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule)
      .setName("Old name")
      .setDescription("Old description")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA)
      .getDefinition();
    db.rules().insert(customRule);
    db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(null));

    dbSession.commit();

    // Update custom rule without setting a value for the parameter
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY)
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    List<RuleParamDto> params = db.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRule.getKey());
    assertThat(params.get(0).getDefaultValue()).isNull();
  }

  @Test
  public void update_active_rule_parameters_when_updating_custom_rule() {
    // Create template rule with 3 parameters
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001")).setLanguage("xoo");
    RuleDefinitionDto templateRuleDefinition = templateRule.getDefinition();
    db.rules().insert(templateRuleDefinition);
    db.rules().insertRuleParam(templateRuleDefinition, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));
    db.rules().insertRuleParam(templateRuleDefinition, param -> param.setName("format").setType("STRING").setDescription("format").setDefaultValue("csv"));
    db.rules().insertRuleParam(templateRuleDefinition, param -> param.setName("message").setType("STRING").setDescription("message"));

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule)
      .setSeverity(Severity.MAJOR)
      .setLanguage("xoo")
      .getDefinition();
    db.rules().insert(customRule);
    RuleParamDto ruleParam1 = db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue("a.*"));
    db.rules().insertRuleParam(customRule, param -> param.setName("format").setType("STRING").setDescription("format").setDefaultValue("txt"));
    db.rules().insertRuleParam(customRule, param -> param.setName("message").setType("STRING").setDescription("message"));

    // Create a quality profile
    QProfileDto profileDto = QProfileTesting.newXooP1(db.getDefaultOrganization());
    db.getDbClient().qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();

    // Activate the custom rule
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileId(profileDto.getId())
      .setRuleId(customRule.getId())
      .setSeverity(Severity.BLOCKER);
    db.getDbClient().activeRuleDao().insert(dbSession, activeRuleDto);
    db.getDbClient().activeRuleDao().insertParam(dbSession, activeRuleDto, new ActiveRuleParamDto()
      .setActiveRuleId(activeRuleDto.getId())
      .setRulesParameterId(ruleParam1.getId())
      .setKey(ruleParam1.getName())
      .setValue(ruleParam1.getDefaultValue()));
    dbSession.commit();

    // Update custom rule parameter 'regex', add 'message' and remove 'format'
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setParameters(ImmutableMap.of("regex", "b.*", "message", "a message"))
      .setOrganization(db.getDefaultOrganization());
    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);

    // Verify custom rule parameters has been updated
    List<RuleParamDto> params = db.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRule.getKey());
    assertThat(params).hasSize(3);

    Map<String, RuleParamDto> paramsByKey = paramsByKey(params);
    assertThat(paramsByKey.get("regex")).isNotNull();
    assertThat(paramsByKey.get("regex").getDefaultValue()).isEqualTo("b.*");
    assertThat(paramsByKey.get("message")).isNotNull();
    assertThat(paramsByKey.get("message").getDefaultValue()).isEqualTo("a message");
    assertThat(paramsByKey.get("format")).isNotNull();
    assertThat(paramsByKey.get("format").getDefaultValue()).isNull();

    // Verify that severity has not changed
    ActiveRuleDto activeRuleReloaded = db.getDbClient().activeRuleDao().selectByKey(dbSession, ActiveRuleKey.of(profileDto, customRule.getKey())).get();
    assertThat(activeRuleReloaded.getSeverityString()).isEqualTo(Severity.BLOCKER);

    // Verify active rule parameters has been updated
    List<ActiveRuleParamDto> activeRuleParams = db.getDbClient().activeRuleDao().selectParamsByActiveRuleId(dbSession, activeRuleReloaded.getId());

    assertThat(activeRuleParams).hasSize(2);
    Map<String, ActiveRuleParamDto> activeRuleParamsByKey = ActiveRuleParamDto.groupByKey(activeRuleParams);
    assertThat(activeRuleParamsByKey.get("regex").getValue()).isEqualTo("b.*");
    assertThat(activeRuleParamsByKey.get("message").getValue()).isEqualTo("a message");
    assertThat(activeRuleParamsByKey.get("format")).isNull();
  }

  @Test
  public void fail_to_update_custom_rule_when_empty_name() {
    // Create template rule
    RuleDefinitionDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001")).getDefinition();
    db.rules().insert(templateRule);

    // Create custom rule
    RuleDefinitionDto customRule = RuleTesting.newCustomRule(templateRule);
    db.rules().insert(customRule);

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setName("")
      .setMarkdownDescription("New desc")
      .setOrganization(db.getDefaultOrganization());

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The name is missing");

    underTest.update(dbSession, update, db.getDefaultOrganization(), userSessionRule);
  }

  @Test
  public void fail_to_update_custom_rule_when_empty_description() {
    // Create template rule
    RuleDto templateRule = RuleTesting.newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule.getDefinition());

    // Create custom rule
    RuleDto customRule = RuleTesting.newCustomRule(templateRule);
    db.rules().insert(customRule.getDefinition());

    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("The description is missing");

    underTest.update(dbSession,
      createForCustomRule(customRule.getKey()).setName("New name").setMarkdownDescription("").setOrganization(db.getDefaultOrganization()),
      db.getDefaultOrganization(), userSessionRule);
  }

  @Test
  public void fail_to_update_plugin_rule_if_name_is_set() {
    RuleDefinitionDto ruleDefinition = db.rules().insert(newRule(RuleKey.of("squid", "S01")));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Not a custom rule");

    createForPluginRule(ruleDefinition.getKey()).setName("New name");
  }

  @Test
  public void fail_to_update_plugin_rule_if_description_is_set() {
    RuleDefinitionDto ruleDefinition = db.rules().insert(newRule(RuleKey.of("squid", "S01")));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Not a custom rule");

    createForPluginRule(ruleDefinition.getKey()).setMarkdownDescription("New description");
  }

  @Test
  public void fail_to_update_plugin_rule_if_severity_is_set() {
    RuleDefinitionDto ruleDefinition = db.rules().insert(newRule(RuleKey.of("squid", "S01")));
    dbSession.commit();

    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Not a custom rule");

    createForPluginRule(ruleDefinition.getKey()).setSeverity(CRITICAL);
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
