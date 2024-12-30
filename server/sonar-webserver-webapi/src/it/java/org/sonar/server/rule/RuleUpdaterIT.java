/*
 * SonarQube
 * Copyright (C) 2009-2024 SonarSource SA
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.sonar.api.config.Configuration;
import org.sonar.api.impl.utils.TestSystem2;
import org.sonar.api.issue.impact.SoftwareQuality;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rule.RuleStatus;
import org.sonar.api.rule.Severity;
import org.sonar.api.rules.RuleType;
import org.sonar.api.server.debt.DebtRemediationFunction;
import org.sonar.api.server.debt.internal.DefaultDebtRemediationFunction;
import org.sonar.api.utils.System2;
import org.sonar.core.util.UuidFactoryFast;
import org.sonar.db.DbSession;
import org.sonar.db.DbTester;
import org.sonar.db.issue.ImpactDto;
import org.sonar.db.qualityprofile.ActiveRuleDto;
import org.sonar.db.qualityprofile.ActiveRuleKey;
import org.sonar.db.qualityprofile.ActiveRuleParamDto;
import org.sonar.db.qualityprofile.QProfileDto;
import org.sonar.db.qualityprofile.QualityProfileTesting;
import org.sonar.db.rule.RuleDto;
import org.sonar.db.rule.RuleParamDto;
import org.sonar.db.rule.RuleTesting;
import org.sonar.db.user.UserDto;
import org.sonar.server.es.EsTester;
import org.sonar.server.es.SearchOptions;
import org.sonar.server.rule.index.RuleIndex;
import org.sonar.server.rule.index.RuleIndexer;
import org.sonar.server.rule.index.RuleQuery;
import org.sonar.server.tester.UserSessionRule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.sonar.api.rule.Severity.CRITICAL;
import static org.sonar.db.rule.RuleTesting.newCustomRule;
import static org.sonar.db.rule.RuleTesting.newRule;
import static org.sonar.db.rule.RuleTesting.newTemplateRule;
import static org.sonar.server.rule.RuleUpdate.createForCustomRule;
import static org.sonar.server.rule.RuleUpdate.createForPluginRule;

class RuleUpdaterIT {

  private static final RuleKey RULE_KEY = RuleKey.of("java", "S001");

  private final System2 system2 = new TestSystem2().setNow(Instant.now().toEpochMilli());

  @RegisterExtension
  private final UserSessionRule userSessionRule = UserSessionRule.standalone();

  @RegisterExtension
  private final DbTester db = DbTester.create(system2);

  @RegisterExtension
  private final EsTester es = EsTester.create();

  private final Configuration config = mock(Configuration.class);

  private final RuleIndex ruleIndex = new RuleIndex(es.client(), system2, config);
  private final RuleIndexer ruleIndexer = new RuleIndexer(es.client(), db.getDbClient());
  private final DbSession dbSession = db.getSession();

  private final UuidFactoryFast uuidFactory = UuidFactoryFast.getInstance();
  private final RuleUpdater underTest = new RuleUpdater(db.getDbClient(), ruleIndexer, uuidFactory, system2);

  @Test
  void do_update_rule_with_removed_status() {
    db.rules().insert(newRule(RULE_KEY).setStatus(RuleStatus.REMOVED));
    dbSession.commit();

    RuleUpdate update = createForCustomRule(RULE_KEY)
      .setTags(Sets.newHashSet("java9"))
      .setStatus(RuleStatus.READY);

    assertThatNoException().isThrownBy(() -> underTest.update(dbSession, update, userSessionRule));
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);

    assertThat(rule.getTags()).containsOnly("java9");
    assertThat(rule.getStatus()).isEqualTo(RuleStatus.READY);
  }

  @Test
  void no_changes() {
    RuleDto ruleDto = RuleTesting.newRule(RULE_KEY)
      // the following fields are not supposed to be updated
      .setNoteData("my *note*")
      .setNoteUserUuid("me")
      .setTags(ImmutableSet.of("tag1"))
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier("1d")
      .setRemediationBaseEffort("5min");
    db.rules().insert(ruleDto);
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY);
    assertThat(update.isEmpty()).isTrue();
    underTest.update(dbSession, update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isEqualTo("my *note*");
    assertThat(rule.getNoteUserUuid()).isEqualTo("me");
    assertThat(rule.getTags()).containsOnly("tag1");
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  void set_markdown_note() {
    UserDto user = db.users().insertUser();
    userSessionRule.logIn(user);

    RuleDto ruleDto = RuleTesting.newRule(RULE_KEY)
      .setNoteData(null)
      .setNoteUserUuid(null)

      // the following fields are not supposed to be updated
      .setTags(ImmutableSet.of("tag1"))
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier("1d")
      .setRemediationBaseEffort("5min");
    db.rules().insert(ruleDto);
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setMarkdownNote("my *note*");
    underTest.update(dbSession, update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
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
  void remove_markdown_note() {
    RuleDto ruleDto = RuleTesting.newRule(RULE_KEY)
      .setNoteData("my *note*")
      .setNoteUserUuid("me");
    db.rules().insert(ruleDto);
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setMarkdownNote(null);
    underTest.update(dbSession, update, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getNoteData()).isNull();
    assertThat(rule.getNoteUserUuid()).isNull();
    assertThat(rule.getNoteCreatedAt()).isNull();
    assertThat(rule.getNoteUpdatedAt()).isNull();
  }

  @Test
  void set_tags() {
    // insert db
    db.rules().insert(RuleTesting.newRule(RULE_KEY)
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc")));
    dbSession.commit();

    // java8 is a system tag -> ignore
    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setTags(Sets.newHashSet("bug", "java8"));
    underTest.update(dbSession, update, userSessionRule);

    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getTags()).containsOnly("bug");
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    List<String> tags = ruleIndex.listTags(null, null, 10);
    assertThat(tags).containsExactly("bug", "java8", "javadoc");
  }

  @Test
  void remove_tags() {
    RuleDto ruleDto = RuleTesting.newRule(RULE_KEY)
      .setUuid("57a3af91-32f8-48b0-9e11-0eac14ffa915")
      .setTags(Sets.newHashSet("security"))
      .setSystemTags(Sets.newHashSet("java8", "javadoc"));
    db.rules().insert(ruleDto);
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setTags(null);
    underTest.update(dbSession, update, null, userSessionRule);

    dbSession.clearCache();
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getTags()).isEmpty();
    assertThat(rule.getSystemTags()).containsOnly("java8", "javadoc");

    // verify that tags are indexed in index
    List<String> tags = ruleIndex.listTags(null, null, 10);
    assertThat(tags).containsExactly("java8", "javadoc");
  }

  @Test
  void override_debt() {
    db.rules().insert(newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min"));
    dbSession.commit();

    DefaultDebtRemediationFunction fn = new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "1min");
    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(fn);
    underTest.update(dbSession, update, null, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("1min");

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  void override_debt_only_offset() {
    db.rules().insert(newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort(null));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.LINEAR, "2d", null));
    underTest.update(dbSession, update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getRemediationGapMultiplier()).isEqualTo("2d");
    assertThat(rule.getRemediationBaseEffort()).isNull();

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isNull();
  }

  @Test
  void override_debt_from_linear_with_offset_to_constant() {
    db.rules().insert(newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR_OFFSET.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min"));
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(new DefaultDebtRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE, null, "10min"));
    underTest.update(dbSession, update, userSessionRule);
    dbSession.clearCache();

    // verify debt is overridden
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.CONSTANT_ISSUE.name());
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isEqualTo("10min");

    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR_OFFSET.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");
  }

  @Test
  void reset_remediation_function() {
    RuleDto ruleDto = RuleTesting.newRule(RULE_KEY)
      .setDefRemediationFunction(DebtRemediationFunction.Type.LINEAR.name())
      .setDefRemediationGapMultiplier("1d")
      .setDefRemediationBaseEffort("5min")
      .setRemediationFunction(DebtRemediationFunction.Type.CONSTANT_ISSUE.name())
      .setRemediationGapMultiplier(null)
      .setRemediationBaseEffort("1min");
    db.rules().insert(ruleDto);
    dbSession.commit();

    RuleUpdate update = createForPluginRule(RULE_KEY)
      .setDebtRemediationFunction(null);
    underTest.update(dbSession, update, userSessionRule);
    dbSession.clearCache();

    // verify debt is coming from default values
    RuleDto rule = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, RULE_KEY);
    assertThat(rule.getDefRemediationFunction()).isEqualTo(DebtRemediationFunction.Type.LINEAR.name());
    assertThat(rule.getDefRemediationGapMultiplier()).isEqualTo("1d");
    assertThat(rule.getDefRemediationBaseEffort()).isEqualTo("5min");

    assertThat(rule.getRemediationFunction()).isNull();
    assertThat(rule.getRemediationGapMultiplier()).isNull();
    assertThat(rule.getRemediationBaseEffort()).isNull();
  }

  @Test
  void update_custom_rule() {
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);
    db.rules().insertRuleParam(templateRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));
    db.rules().insertRuleParam(templateRule, param -> param.setName("format").setType("STRING").setDescription("Format"));

    RuleDto customRule = newCustomRule(templateRule, "Old description")
      .setName("Old name")
      .setType(RuleType.CODE_SMELL)
      .replaceAllDefaultImpacts(List.of(new ImpactDto()
        .setSoftwareQuality(SoftwareQuality.MAINTAINABILITY)
        .setSeverity(org.sonar.api.issue.impact.Severity.LOW)))
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA);
    db.rules().insert(customRule);
    db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue("a.*"));
    db.rules().insertRuleParam(customRule, param -> param.setName("format").setType("STRING").setDescription("Format").setDefaultValue(null));

    // Update custom rule
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY)
      .setParameters(ImmutableMap.of("regex", "b.*"));
    underTest.update(dbSession, update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    RuleDto customRuleReloaded = db.getDbClient().ruleDao().selectOrFailByKey(dbSession, customRule.getKey());
    assertThat(customRuleReloaded).isNotNull();
    assertThat(customRuleReloaded.getName()).isEqualTo("New name");
    assertThat(customRuleReloaded.getDefaultRuleDescriptionSection().getContent()).isEqualTo("New description");
    assertThat(customRuleReloaded.getDefaultImpacts()).extracting(ImpactDto::getSoftwareQuality, ImpactDto::getSeverity)
      .containsExactlyInAnyOrder(tuple(SoftwareQuality.MAINTAINABILITY, org.sonar.api.issue.impact.Severity.MEDIUM));
    assertThat(customRuleReloaded.getSeverityString()).isEqualTo("MAJOR");
    assertThat(customRuleReloaded.getStatus()).isEqualTo(RuleStatus.READY);

    List<RuleParamDto> params = db.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRuleReloaded.getKey());
    assertThat(params).extracting(RuleParamDto::getDefaultValue).containsOnly("b.*", null);

    // Verify in index
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("New name"), new SearchOptions()).getUuids()).containsOnly(customRule.getUuid());
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("New description"), new SearchOptions()).getUuids()).containsOnly(customRule.getUuid());

    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Old name"), new SearchOptions()).getTotal()).isZero();
    assertThat(ruleIndex.search(new RuleQuery().setQueryText("Old description"), new SearchOptions()).getTotal()).isZero();
  }

  @Test
  void update_custom_rule_with_empty_parameter() {
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);
    db.rules().insertRuleParam(templateRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(null));

    RuleDto customRule = newCustomRule(templateRule, "Old description")
      .setName("Old name")
      .setSeverity(Severity.MINOR)
      .setStatus(RuleStatus.BETA);
    db.rules().insert(customRule);
    db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(null));

    dbSession.commit();

    // Update custom rule without setting a value for the parameter
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setName("New name")
      .setMarkdownDescription("New description")
      .setSeverity("MAJOR")
      .setStatus(RuleStatus.READY);
    underTest.update(dbSession, update, userSessionRule);

    dbSession.clearCache();

    // Verify custom rule is updated
    List<RuleParamDto> params = db.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRule.getKey());
    assertThat(params.get(0).getDefaultValue()).isNull();
  }

  @Test
  void update_active_rule_parameters_when_updating_custom_rule() {
    // Create template rule with 3 parameters
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001")).setLanguage("xoo");
    RuleDto templateRuleDefinition = templateRule;
    db.rules().insert(templateRuleDefinition);
    db.rules().insertRuleParam(templateRuleDefinition, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue(".*"));
    db.rules().insertRuleParam(templateRuleDefinition, param -> param.setName("format").setType("STRING").setDescription("format").setDefaultValue("csv"));
    db.rules().insertRuleParam(templateRuleDefinition, param -> param.setName("message").setType("STRING").setDescription("message"));

    // Create custom rule
    RuleDto customRule = newCustomRule(templateRule)
      .setSeverity(Severity.MAJOR)
      .setLanguage("xoo");
    db.rules().insert(customRule);
    RuleParamDto ruleParam1 = db.rules().insertRuleParam(customRule, param -> param.setName("regex").setType("STRING").setDescription("Reg ex").setDefaultValue("a.*"));
    db.rules().insertRuleParam(customRule, param -> param.setName("format").setType("STRING").setDescription("format").setDefaultValue("txt"));
    db.rules().insertRuleParam(customRule, param -> param.setName("message").setType("STRING").setDescription("message"));

    // Create a quality profile
    QProfileDto profileDto = QualityProfileTesting.newQualityProfileDto();
    db.getDbClient().qualityProfileDao().insert(dbSession, profileDto);
    dbSession.commit();

    // Activate the custom rule
    ActiveRuleDto activeRuleDto = new ActiveRuleDto()
      .setProfileUuid(profileDto.getRulesProfileUuid())
      .setRuleUuid(customRule.getUuid())
      .setSeverity(Severity.BLOCKER);
    db.getDbClient().activeRuleDao().insert(dbSession, activeRuleDto);
    db.getDbClient().activeRuleDao().insertParam(dbSession, activeRuleDto, new ActiveRuleParamDto()
      .setActiveRuleUuid(activeRuleDto.getUuid())
      .setRulesParameterUuid(ruleParam1.getUuid())
      .setKey(ruleParam1.getName())
      .setValue(ruleParam1.getDefaultValue()));
    dbSession.commit();

    // Update custom rule parameter 'regex', add 'message' and remove 'format'
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setParameters(ImmutableMap.of("regex", "b.*", "message", "a message"));
    underTest.update(dbSession, update, userSessionRule);

    // Verify custom rule parameters has been updated
    List<RuleParamDto> params = db.getDbClient().ruleDao().selectRuleParamsByRuleKey(dbSession, customRule.getKey());
    assertThat(params).hasSize(3);

    Map<String, RuleParamDto> paramsByKey = paramsByName(params);
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
    List<ActiveRuleParamDto> activeRuleParams = db.getDbClient().activeRuleDao().selectParamsByActiveRuleUuid(dbSession, activeRuleReloaded.getUuid());

    assertThat(activeRuleParams).hasSize(2);
    Map<String, ActiveRuleParamDto> activeRuleParamsByKey = ActiveRuleParamDto.groupByKey(activeRuleParams);
    assertThat(activeRuleParamsByKey.get("regex").getValue()).isEqualTo("b.*");
    assertThat(activeRuleParamsByKey.get("message").getValue()).isEqualTo("a message");
    assertThat(activeRuleParamsByKey.get("format")).isNull();
  }

  @Test
  void fail_to_update_custom_rule_when_empty_name() {
    // Create template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);

    // Create custom rule
    RuleDto customRule = newCustomRule(templateRule);
    db.rules().insert(customRule);

    dbSession.commit();

    // Update custom rule
    RuleUpdate update = createForCustomRule(customRule.getKey())
      .setName("")
      .setMarkdownDescription("New desc");

    assertThatThrownBy(() -> {
      underTest.update(dbSession, update, userSessionRule);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The name is missing");
  }

  @Test
  void fail_to_update_custom_rule_when_empty_description() {
    // Create template rule
    RuleDto templateRule = newTemplateRule(RuleKey.of("java", "S001"));
    db.rules().insert(templateRule);

    // Create custom rule
    RuleDto customRule = newCustomRule(templateRule);
    db.rules().insert(customRule);

    dbSession.commit();

    assertThatThrownBy(() -> {
      underTest.update(dbSession,
        createForCustomRule(customRule.getKey()).setName("New name").setMarkdownDescription(""),
        userSessionRule);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("The description is missing");
  }

  @Test
  void fail_to_update_plugin_rule_if_name_is_set() {
    RuleDto ruleDto = db.rules().insert(newRule(RuleKey.of("java", "S01")));
    dbSession.commit();

    assertThatThrownBy(() -> {
      createForPluginRule(ruleDto.getKey()).setName("New name");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Not a custom rule");
  }

  @Test
  void fail_to_update_plugin_rule_if_description_is_set() {
    RuleDto ruleDto = db.rules().insert(newRule(RuleKey.of("java", "S01")));
    dbSession.commit();

    assertThatThrownBy(() -> {
      createForPluginRule(ruleDto.getKey()).setMarkdownDescription("New description");
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Not a custom rule");
  }

  @Test
  void fail_to_update_plugin_rule_if_severity_is_set() {
    RuleDto ruleDto = db.rules().insert(newRule(RuleKey.of("java", "S01")));
    dbSession.commit();

    assertThatThrownBy(() -> {
      createForPluginRule(ruleDto.getKey()).setSeverity(CRITICAL);
    })
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessage("Not a custom rule");
  }

  private static Map<String, RuleParamDto> paramsByName(List<RuleParamDto> params) {
    return params.stream().collect(Collectors.toMap(RuleParamDto::getName, p -> p));
  }

}
